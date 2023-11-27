package com.rakovpublic.jneuropallium.worker.application;

import com.rakovpublic.jneuropallium.worker.exceptions.HttpClusterCommunicationException;
import com.rakovpublic.jneuropallium.worker.model.NodeCompleteRequest;
import com.rakovpublic.jneuropallium.worker.net.neuron.IAxon;
import com.rakovpublic.jneuropallium.worker.net.neuron.INeuron;
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
            for (INeuron neuron : splitInput.getNeurons()) {
                neuron.setCurrentLoop(inputResolver.getCurrentLoop());
                neuron.setRun(inputResolver.getRun());
                neuron.addSignals(input.get(neuron.getId()));
                neuron.setCyclingNeuronInputMapping(inputResolver.getCycleNeuronAddressMapping());
                neuron.processSignals();
                neuron.activate();
                IAxon axon = neuron.getAxon();
                HashMap<Integer, HashMap<Long, List<ISignal>>> result = axon.getSignalResultStructure(axon.processSignals(neuron.getResult()));
                splitInput.saveResults(result);
                splitInput.saveNeuron(neuron);
            }
        }
    }

    private ISplitInput parseSplitInput(String json) {

        //TODO: implement

        return null;
    }


}
