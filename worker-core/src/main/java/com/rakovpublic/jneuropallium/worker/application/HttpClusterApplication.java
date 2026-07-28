package com.rakovpublic.jneuropallium.worker.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rakovpublic.jneuropallium.worker.exceptions.HttpClusterCommunicationException;
import com.rakovpublic.jneuropallium.worker.model.NodeCompleteRequest;
import com.rakovpublic.jneuropallium.worker.model.UploadSignalsRequest;
import com.rakovpublic.jneuropallium.worker.net.neuron.IAxon;
import com.rakovpublic.jneuropallium.worker.net.neuron.INeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.NeuronRunnerService;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.storage.IInputResolver;
import com.rakovpublic.jneuropallium.worker.net.signals.storage.ISplitInput;
import com.rakovpublic.jneuropallium.worker.util.IContext;
import com.rakovpublic.jneuropallium.worker.util.JarClassLoaderService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class HttpClusterApplication implements IApplication {
    private static final Logger logger = LogManager.getLogger(HttpClusterApplication.class);
    private static final long IDLE_POLL_MILLIS = 500L;
    private final String nodeName;

    public HttpClusterApplication() {
        this(System.getProperty("jneuropallium.node.name", java.util.UUID.randomUUID().toString()));
    }

    public HttpClusterApplication(String nodeName) {
        this.nodeName = nodeName;
    }

    @Override
    public void startApplication(IContext context, JarClassLoaderService classLoaderService) {
        String masterAddress = context.getProperty("master.address");
        String registerLink = masterAddress + "/nodeManager/register";
        String getSplitInputLink = masterAddress + "/nodeManager/nextRun";
        String callbackLink = masterAddress + "/input/callback";
        String completeLink = masterAddress + "/nodeManager/completeRun";
        NodeCompleteRequest nodeCompleteRequest = new NodeCompleteRequest();
        nodeCompleteRequest.setNodeName(nodeName);
        HttpCommunicationClient communicationClient = new HttpCommunicationClient();

        try {
            communicationClient.sendRequest(HttpRequestResolver.createPost(registerLink, nodeCompleteRequest));
        } catch (IOException | InterruptedException e) {
            logger.error("Cannot register node", e);
            throw new HttpClusterCommunicationException(e.getMessage());
        }
        logger.info("Node " + nodeName + " registered on " + masterAddress);

        while (!Thread.currentThread().isInterrupted()) {
            ISplitInput splitInput;
            try {
                String jsonSplitInput = communicationClient.sendRequest(HttpRequestResolver.createPost(getSplitInputLink, nodeCompleteRequest));
                splitInput = parseSplitInput(jsonSplitInput);
                if (splitInput == null) {
                    // The master has nothing to hand out yet: another node is still finishing the
                    // current layer, or the next epoch has not been populated. Back off and retry.
                    Thread.sleep(IDLE_POLL_MILLIS);
                    continue;
                }
            } catch (IOException | InterruptedException e) {
                logger.error("Cannot obtain next partition from master", e);
                throw new HttpClusterCommunicationException(e.getMessage());
            }

            try {
                processPartition(splitInput);
            } catch (RuntimeException e) {
                logger.error("Failed to process partition of layer " + splitInput.getLayerId(), e);
            }

            try {
                // Results are already in the shared storage, so the callback carries no payload -
                // it only tells the master that this node became idle again.
                UploadSignalsRequest uploadSignalsRequest = new UploadSignalsRequest();
                uploadSignalsRequest.setName(nodeName);
                uploadSignalsRequest.setSignals(new HashMap<>());
                uploadSignalsRequest.setDiscriminator(splitInput.getDiscriminatorName() != null);
                communicationClient.sendRequest(HttpRequestResolver.createPost(callbackLink, uploadSignalsRequest));
                communicationClient.sendRequest(HttpRequestResolver.createPost(completeLink, nodeCompleteRequest));
            } catch (IOException | InterruptedException e) {
                logger.error("Cannot report partition completion", e);
                throw new HttpClusterCommunicationException(e.getMessage());
            }
        }
    }

    private void processPartition(ISplitInput splitInput) {
        IInputResolver inputResolver = splitInput.getInputResolver();
        HashMap<Long, CopyOnWriteArrayList<ISignal>> input = inputResolver.getSignalPersistStorage().getLayerSignals(splitInput.getLayerId());
        NeuronRunnerService neuronRunnerService = NeuronRunnerService.getService();
        List<INeuron> neurons = new ArrayList<>((List<INeuron>) splitInput.getNeurons());
        logger.info("Processing layer " + splitInput.getLayerId() + " partition ["
                + splitInput.getStart() + "," + splitInput.getEnd() + ") - " + neurons.size() + " neurons");

        for (INeuron neuron : neurons) {
            neuron.setCurrentLoop(inputResolver.getCurrentLoop());
            neuron.setRun(inputResolver.getRun());
            List<ISignal> neuronSignals = input.get(neuron.getId());
            neuron.addSignals(neuronSignals == null ? new ArrayList<>() : neuronSignals);
            neuron.setCyclingNeuronInputMapping(inputResolver.getCycleNeuronAddressMapping());
            neuronRunnerService.addNeuron(neuron);
        }
        neuronRunnerService.process(splitInput.getThreads());

        List<INeuron> pending = new ArrayList<>(neurons);
        List<INeuron> processed = new ArrayList<>(neurons.size());
        HashMap<Integer, HashMap<Long, CopyOnWriteArrayList<ISignal>>> results = new HashMap<>();
        // process() returns only once the queue is drained, so one pass that frees no neuron at
        // all means the rest cannot produce a result - a neuron with no input for this run, or one
        // whose processing failed. Retrying those forever would hold the partition and stall the
        // whole cluster, so give up on them and let the master move on.
        while (!pending.isEmpty()) {
            int before = pending.size();
            collectProcessed(pending, processed, results);
            if (pending.isEmpty()) {
                break;
            }
            if (before == pending.size()) {
                logger.warn(pending.size() + " neurons of layer " + splitInput.getLayerId()
                        + " produced no result and are left unchanged");
                break;
            }
            //fault tolerance: resubmit whatever has not produced a result yet
            neuronRunnerService.getNeuronQueue().addAll(pending);
            neuronRunnerService.process(splitInput.getThreads());
        }

        // One write for the whole partition rather than one per neuron: with a remote store the
        // per-neuron round trips dominate the run and can push a partition past the node timeout.
        splitInput.saveResults(results);
        splitInput.saveNeurons(processed);
    }

    private void collectProcessed(List<INeuron> pending, List<INeuron> processed,
                                  HashMap<Integer, HashMap<Long, CopyOnWriteArrayList<ISignal>>> results) {
        for (INeuron neuron : new ArrayList<>(pending)) {
            if (!neuron.hasResult()) {
                continue;
            }
            IAxon axon = neuron.getAxon();
            merge(results, axon.getSignalResultStructure(axon.processSignals(neuron.getResult())));
            processed.add(neuron);
            pending.remove(neuron);
        }
    }

    private void merge(HashMap<Integer, HashMap<Long, CopyOnWriteArrayList<ISignal>>> target,
                       HashMap<Integer, HashMap<Long, CopyOnWriteArrayList<ISignal>>> source) {
        if (source == null) {
            return;
        }
        for (Map.Entry<Integer, HashMap<Long, CopyOnWriteArrayList<ISignal>>> layer : source.entrySet()) {
            HashMap<Long, CopyOnWriteArrayList<ISignal>> targetLayer =
                    target.computeIfAbsent(layer.getKey(), key -> new HashMap<>());
            for (Map.Entry<Long, CopyOnWriteArrayList<ISignal>> neuron : layer.getValue().entrySet()) {
                targetLayer.computeIfAbsent(neuron.getKey(), key -> new CopyOnWriteArrayList<>())
                        .addAll(neuron.getValue());
            }
        }
    }

    private ISplitInput parseSplitInput(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        ObjectMapper mapper = new ObjectMapper();
        JsonElement jelement = JsonParser.parseString(json);
        if (!jelement.isJsonObject()) {
            return null;
        }
        JsonObject jobject = jelement.getAsJsonObject();
        JsonElement payload = jobject.get("splitInput");
        JsonElement className = jobject.get("className");
        if (payload == null || payload.isJsonNull() || className == null || className.isJsonNull()) {
            return null;
        }
        try {
            return (ISplitInput) mapper.readValue(payload.toString(), Class.forName(className.getAsString()));
        } catch (JsonProcessingException | ClassNotFoundException e) {
            logger.error("Cannot parse split input " + json, e);
            return null;
        }
    }
}
