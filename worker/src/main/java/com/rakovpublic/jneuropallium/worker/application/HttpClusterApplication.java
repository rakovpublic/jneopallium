package com.rakovpublic.jneuropallium.worker.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rakovpublic.jneuropallium.worker.exceptions.HttpClusterCommunicationException;
import com.rakovpublic.jneuropallium.worker.model.NodeCompleteRequest;
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
import java.util.HashMap;
import java.util.List;

public class HttpClusterApplication implements IApplication {
    private static final Logger logger = LogManager.getLogger(HttpClusterApplication.class);
    private final static String UUID = java.util.UUID.randomUUID().toString();

    @Override
    public void startApplication(IContext context, JarClassLoaderService classLoaderService) {
        String registerLink = context.getProperty("master.address") + "/nodeManager/register";
        String getSplitInputLink = context.getProperty("master.address") + "/nodeManager/nextRun";
        NodeCompleteRequest nodeCompleteRequest = new NodeCompleteRequest();
        nodeCompleteRequest.setNodeName(UUID);
        HttpCommunicationClient communicationClient = new HttpCommunicationClient();

        try {
            communicationClient.sendRequest(HttpRequestResolver.createPost(registerLink, nodeCompleteRequest));
        } catch (IOException | InterruptedException e) {
            logger.error("Cannot register node", e);
            throw new HttpClusterCommunicationException(e.getMessage());
        }
        String jsonSplitInput;
        while (true) {
            try {
                jsonSplitInput = communicationClient.sendRequest(HttpRequestResolver.createPost(getSplitInputLink, nodeCompleteRequest));
            } catch (IOException | InterruptedException e) {
                logger.error("Cannot register node", e);
                throw new HttpClusterCommunicationException(e.getMessage());
            }
            ISplitInput splitInput = parseSplitInput(jsonSplitInput);
            IInputResolver inputResolver = splitInput.getInputResolver();
            HashMap<Long, List<ISignal>> input = inputResolver.getSignalPersistStorage().getLayerSignals(splitInput.getLayerId());
            NeuronRunnerService neuronRunnerService = NeuronRunnerService.getService();
            List<INeuron> neurons = (List<INeuron>) splitInput.getNeurons();


            for (INeuron neuron :neurons) {
                neuron.setCurrentLoop(inputResolver.getCurrentLoop());
                neuron.setRun(inputResolver.getRun());
                neuron.addSignals(input.get(neuron.getId()));
                neuron.setCyclingNeuronInputMapping(inputResolver.getCycleNeuronAddressMapping());
                neuronRunnerService.addNeuron(neuron);
            }
            neuronRunnerService.process(splitInput.getThreads());
            while(neurons.size()>0){
                for(INeuron neuron:neurons){
                    if(neuron.hasResult()){
                        IAxon axon = neuron.getAxon();
                        HashMap<Integer, HashMap<Long, List<ISignal>>> result = axon.getSignalResultStructure(axon.processSignals(neuron.getResult()));
                        splitInput.saveResults(result);
                        splitInput.saveNeuron(neuron);
                        neurons.remove(neuron);
                    }
                }
                //fault tolerance
                if(neuronRunnerService.getNeuronQueue().isEmpty()){
                    for(INeuron neuron:neurons){
                        if(neuron.hasResult()){
                            IAxon axon = neuron.getAxon();
                            HashMap<Integer, HashMap<Long, List<ISignal>>> result = axon.getSignalResultStructure(axon.processSignals(neuron.getResult()));
                            splitInput.saveResults(result);
                            splitInput.saveNeuron(neuron);
                            neurons.remove(neuron);
                        }
                    }
                    neuronRunnerService.getNeuronQueue().addAll(neurons);
                }
            }
        }
    }

    private ISplitInput parseSplitInput(String json) {
        ObjectMapper mapper = new ObjectMapper();
        JsonElement jelement = new JsonParser().parse(json);
        JsonObject jobject = jelement.getAsJsonObject();
        ISplitInput result = null;
        try {
            result = (ISplitInput) mapper.readValue(jobject.getAsJsonObject("splitInput").getAsString(), Class.forName(jobject.getAsJsonPrimitive("className").getAsString()));
        } catch (JsonProcessingException | ClassNotFoundException e) {
            logger.error("Cannot parse loading strategy  " + json, e);
        }
        return result;

    }


}
