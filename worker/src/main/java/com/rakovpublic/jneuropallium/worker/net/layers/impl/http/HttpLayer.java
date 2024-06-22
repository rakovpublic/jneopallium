/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.layers.impl.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rakovpublic.jneuropallium.worker.application.HttpCommunicationClient;
import com.rakovpublic.jneuropallium.worker.application.HttpRequestResolver;
import com.rakovpublic.jneuropallium.worker.model.CreateNeuronRequest;
import com.rakovpublic.jneuropallium.worker.model.DeleteNeuronRequest;
import com.rakovpublic.jneuropallium.worker.model.LayerParamUpdate;
import com.rakovpublic.jneuropallium.worker.model.UploadSignalsRequest;
import com.rakovpublic.jneuropallium.worker.net.layers.ILayer;
import com.rakovpublic.jneuropallium.worker.net.layers.ILayerMeta;
import com.rakovpublic.jneuropallium.worker.net.layers.impl.LayerMetaParam;
import com.rakovpublic.jneuropallium.worker.net.neuron.IAxon;
import com.rakovpublic.jneuropallium.worker.net.neuron.INeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.IRule;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISynapse;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.NeuronRunnerService;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.layersizing.CreateNeuronSignal;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.layersizing.DeleteNeuronSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.storage.ISplitInput;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class HttpLayer<N extends INeuron> implements ILayer<N> {
    private static final Logger logger = LogManager.getLogger(HttpLayer.class);
    private final String masterAddress;
    private final Integer layerId;
    private final String UUID;
    private final Integer layerSize;
    private final TreeMap<Long, INeuron> neurons;
    private boolean isProcessed;
    private ConcurrentLinkedQueue<INeuron> notProcessed;
    private final Integer threads;

    private final ISplitInput splitInput;

    public HttpLayer(String masterAddress, Integer layerId, String UUID, Integer layerSize, Integer threads, ISplitInput splitInput) {
        this.masterAddress = masterAddress;
        this.layerId = layerId;
        this.UUID = UUID;
        this.layerSize = layerSize;
        this.threads = threads;
        notProcessed = new ConcurrentLinkedQueue<INeuron>();
        this.splitInput = splitInput;
        neurons = new TreeMap<>();
        isProcessed = false;
    }

    @Override
    public void createNeuron(CreateNeuronSignal signal) {
        String createUrl = masterAddress + "/layer/createNeuron";
        String sendResultLink = masterAddress + "/input/callback";
        HttpCommunicationClient communicationClient = new HttpCommunicationClient();
        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        INeuron neuron = signal.getValue().getNeuron();
        String json;
        try {
            json = ow.writeValueAsString(signal.getValue().getNeuron());
        } catch (JsonProcessingException e) {
            logger.error("Cannot generate json", e);
            return;
        }
        CreateNeuronRequest createNeuronRequest = new CreateNeuronRequest();
        createNeuronRequest.setLayerId(layerId);
        createNeuronRequest.setNeuronJson(json);
        createNeuronRequest.setNeuronClass(neuron.getCurrentNeuronClass().getCanonicalName());
        try {
            communicationClient.sendRequest(HttpRequestResolver.createPost(createUrl, createNeuronRequest));
        } catch (IOException | InterruptedException e) {
            logger.error("Cannot send create neuron request", e);
            return;
        }
        UploadSignalsRequest uploadSignalsRequest = new UploadSignalsRequest();
        uploadSignalsRequest.setSignals(signal.getValue().getCreateRelationsSignals());
        uploadSignalsRequest.setName(UUID);
        try {
            communicationClient.sendRequest(HttpRequestResolver.createPost(sendResultLink, uploadSignalsRequest));
        } catch (IOException | InterruptedException e) {
            logger.error("Cannot send add neuron connections request", e);
            return;
        }
    }


    @Override
    public void deleteNeuron(DeleteNeuronSignal deleteNeuronIntegration) {
        String deleteUrl = masterAddress + "/layer/deleteNeuron";
        String sendResultLink = masterAddress + "/input/callback";
        HttpCommunicationClient communicationClient = new HttpCommunicationClient();
        DeleteNeuronRequest deleteNeuronRequest = new DeleteNeuronRequest();
        deleteNeuronRequest.setNeuronId(deleteNeuronIntegration.getValue().getNeuronId());
        deleteNeuronRequest.setLayerId(layerId);
        try {
            communicationClient.sendRequest(HttpRequestResolver.createPost(deleteUrl, deleteNeuronRequest));
        } catch (IOException | InterruptedException e) {
            logger.error("Cannot send delete request", e);
            return;
        }
        UploadSignalsRequest uploadSignalsRequest = new UploadSignalsRequest();
        uploadSignalsRequest.setSignals(deleteNeuronIntegration.getValue().getCreateRelationsSignals());
        uploadSignalsRequest.setName(UUID);
        try {
            communicationClient.sendRequest(HttpRequestResolver.createPost(sendResultLink, uploadSignalsRequest));
        } catch (IOException | InterruptedException e) {
            logger.error("Cannot send update neuron connections request", e);

        }
        return;
    }


    @Override
    public long getLayerSize() {
        return layerSize;
    }

    @Override
    public Boolean validateGlobal() {
        return true;
    }

    @Override
    public Boolean validateLocal() {
        return true;
    }

    @Override
    public void addGlobalRule(IRule rule) {

    }

    @Override
    public void registerAll(List<? extends N> neuron) {
        for (INeuron ner : neuron) {
            ner.setLayer(this);
            neurons.put(ner.getId(), ner);
        }
    }

    @Override
    public void register(INeuron neuron) {
        neurons.put(neuron.getId(), neuron);
    }


    @Override
    public void process() {
        HashMap<Long, CopyOnWriteArrayList<ISignal>> input = splitInput.getInputResolver().getSignalPersistStorage().getLayerSignals(this.layerId);
        INeuron neur;
        NeuronRunnerService ns = NeuronRunnerService.getService();
        notProcessed = ns.getNeuronQueue();
        for (Long neuronId : neurons.keySet()) {
            if (input.containsKey(neuronId)) {
                neur = neurons.get(neuronId);
                neur.setCyclingNeuronInputMapping(splitInput.getInputResolver().getCycleNeuronAddressMapping());
                neur.setSignalHistory(splitInput.getInputResolver().getSignalsHistoryStorage());
                neur.addSignals(input.get(neuronId));
                neur.setRun(splitInput.getInputResolver().getRun());
                neur.setCurrentLoop(splitInput.getInputResolver().getCurrentLoop());
                ns.addNeuron(neur);
            }
        }
        ns.process(threads);
    }

    @Override
    public int getId() {
        return 0;
    }

    @Override
    public Boolean isProcessed() {
        if (!isProcessed && notProcessed.size() == 0) {
            isProcessed = true;
            for (INeuron ner : neurons.values()) {
                if (!ner.hasResult()) {
                    notProcessed.add(ner);
                    isProcessed = false;
                }
            }
        } else {

            for (INeuron ner : notProcessed) {
                if (ner.hasResult()) {
                    notProcessed.remove(ner);
                }
            }
            if (notProcessed.size() == 0) {
                isProcessed = true;
            }
        }
        return isProcessed;
    }

    @Override
    public void dumpResult() {
        HashMap<Integer, HashMap<Long, CopyOnWriteArrayList<ISignal>>> result = getResults();
        splitInput.getInputResolver().getSignalPersistStorage().putSignals(result);
    }

    @Override
    public void dumpNeurons(ILayerMeta layerMeta) {
        layerMeta.saveNeurons(neurons.values().stream().collect(Collectors.toList()));
    }

    @Override
    public HashMap<Integer, HashMap<Long, CopyOnWriteArrayList<ISignal>>> getResults() {
        HashMap<Integer, HashMap<Long, CopyOnWriteArrayList<ISignal>>> result = new HashMap<>();
        for (Long neurId : neurons.keySet()) {
            INeuron neur = neurons.get(neurId);
            IAxon axon = neur.getAxon();
            if (axon.isConnectionsWrapped()) {
                axon.unwrapConnections();
            }
            HashMap<ISignal, List<ISynapse>> tMap = axon.processSignals(neur.getResult());
            for (ISignal signal : tMap.keySet()) {
                signal.setSourceLayerId(this.layerId);
                signal.setSourceNeuronId(neurId);
                for (ISynapse connection : tMap.get(signal)) {
                    int layerId = connection.getTargetLayerId();
                    Long targetNeurId = connection.getTargetNeuronId();
                    if (result.containsKey(layerId)) {
                        if (result.get(layerId).containsKey(targetNeurId)) {
                            result.get(layerId).get(targetNeurId).add(signal);
                        } else {
                            CopyOnWriteArrayList<ISignal> signals = new CopyOnWriteArrayList<>();
                            signals.add(signal);
                            result.get(layerId).put(targetNeurId, signals);
                        }
                    } else {
                        CopyOnWriteArrayList<ISignal> signals = new CopyOnWriteArrayList<>();
                        signals.add(signal);
                        HashMap<Long, CopyOnWriteArrayList<ISignal>> ttMap = new HashMap<>();
                        ttMap.put(targetNeurId, signals);
                        result.put(layerId, ttMap);
                    }
                }
            }
        }

        return result;
    }

    @Override
    public String toJSON() {
        return null;
    }


    @Override
    public void sendCallBack(String name, List<ISignal> signals) {
        splitInput.getInputResolver().sendCallBack(name, signals);
    }

    @Override
    public LayerMetaParam getLayerMetaParam(String key) {

        String getLayerParam = masterAddress + "/layer/getLayerParam?param=" + key + "&layerId=" + layerId;
        HttpCommunicationClient communicationClient = new HttpCommunicationClient();
        String resultJson = null;
        try {
            resultJson = communicationClient.sendRequest(HttpRequestResolver.createGet(getLayerParam));
        } catch (IOException | InterruptedException e) {
            logger.error("Cannot send delete request", e);
            return null;
        }
        if (resultJson != null) {
            JsonElement jelement = new JsonParser().parse(resultJson);
            JsonObject jobject = jelement.getAsJsonObject();
            String cl = jobject.getAsJsonPrimitive("paramClass").getAsString();
            ObjectMapper mapper = new ObjectMapper();
            try {
                LayerMetaParam metaParam = new LayerMetaParam(mapper.readValue(jobject.getAsJsonPrimitive("param").getAsString(), Class.forName(cl)));
                return metaParam;
            } catch (IOException | ClassNotFoundException e) {
                logger.error("cannot parse layer meta param from json " + resultJson, e);
            }


        }
        return null;
    }

    @Override
    public void updateLayerMetaParam(String key, LayerMetaParam metaParam) {
        String sendMetaParam = masterAddress + "/layer/updateLayerParam";
        HttpCommunicationClient communicationClient = new HttpCommunicationClient();
        LayerParamUpdate layerParamUpdate = new LayerParamUpdate(key, layerId, metaParam);
        try {
            communicationClient.sendRequest(HttpRequestResolver.createPost(sendMetaParam, layerParamUpdate));
        } catch (IOException | InterruptedException e) {
            logger.error("Cannot send delete request", e);
            return;
        }
    }

    @Override
    public void setLayerMetaParams(HashMap<String, LayerMetaParam> params) {
        for (String key : params.keySet()) {
            updateLayerMetaParam(key, params.get(key));
        }

    }
}
