package com.rakovpublic.jneuropallium.worker.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.rakovpublic.jneuropallium.worker.model.CreateNeuronRequest;
import com.rakovpublic.jneuropallium.worker.model.NodeCompleteRequest;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.storages.ISignalStorage;
import com.rakovpublic.jneuropallium.worker.net.storages.ISplitInput;
import com.rakovpublic.jneuropallium.worker.neuron.IAxon;
import com.rakovpublic.jneuropallium.worker.neuron.INeuron;
import com.rakovpublic.jneuropallium.worker.synchronizer.IContext;
import com.rakovpublic.jneuropallium.worker.util.JarClassLoaderService;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class HttpClusterApplication implements IApplication {
    private final static String UUID = java.util.UUID.randomUUID().toString();

    @Override
    public void startApplication(IContext context, JarClassLoaderService classLoaderService) {
        String registerLink = context.getProperty("master.address") + "/nodeManager/register";
        String getSplitInputLink = context.getProperty("master.address") + "/nodeManager/nextRun";
        String sendResultLink = context.getProperty("master.address") + "/input/callback";
        String updateNeuronLink = context.getProperty("master.address") + "/layer/updateNeuron";
        NodeCompleteRequest nodeCompleteRequest = new NodeCompleteRequest();
        nodeCompleteRequest.setNodeName(UUID);
        HttpCommunicationClient communicationClient = new HttpCommunicationClient();

        try {
            communicationClient.sendRequest(HttpRequestResolver.createPost(registerLink, nodeCompleteRequest));
        } catch (IOException e) {
            //TODO: add logger
            return;
        } catch (InterruptedException e) {
            //TODO: add logger
            return;
        }
        String jsonSplitInput;
        while (true) {
            try {
                jsonSplitInput = communicationClient.sendRequest(HttpRequestResolver.createPost(getSplitInputLink, nodeCompleteRequest));
            } catch (IOException e) {
                //TODO: add logger
                return;
            } catch (InterruptedException e) {
                //TODO: add logger
                return;
            }
            ISplitInput splitInput = parseSplitInput(jsonSplitInput);
            ISignalStorage signalStorage = splitInput.readInputs();
            for (INeuron neuron : splitInput.getNeurons()) {
                neuron.setCurrentLoop(splitInput.getEpoch());
                neuron.setRun(splitInput.getRun());
                neuron.addSignals(signalStorage.getSignalsForNeuron(neuron.getId()));
                neuron.setCyclingNeuronInputMapping(splitInput.getServiceInputsMap());
                neuron.processSignals();
                neuron.activate();
                IAxon axon = neuron.getAxon();
                HashMap<Integer, HashMap<Long, List<ISignal>>> result = axon.getSignalResultStructure(axon.processSignals(neuron.getResult()));
                splitInput.saveResults(result);
                ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
                String json;
                try {
                    json = ow.writeValueAsString(neuron);
                } catch (JsonProcessingException e) {

                    //TODO: add logger return
                    return;
                }
                CreateNeuronRequest createNeuronRequest = new CreateNeuronRequest();
                createNeuronRequest.setLayerId(neuron.getLayer().getId());
                createNeuronRequest.setNeuronJson(json);
                createNeuronRequest.setNeuronClass(neuron.getCurrentNeuronClass().getCanonicalName());
                try {
                    communicationClient.sendRequest(HttpRequestResolver.createPost(updateNeuronLink, createNeuronRequest));
                } catch (IOException e) {
                    //TODO: add logger return
                    return;
                } catch (InterruptedException e) {
                    //TODO: add logger return
                    return;
                }
            }
        }
    }

    private ISplitInput parseSplitInput(String json) {

        //TODO: implement

        return null;
    }


}
