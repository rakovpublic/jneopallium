package com.rakovpublic.jneuropallium.worker.application;

import com.rakovpublic.jneuropallium.master.model.NodeCompleteRequest;
import com.rakovpublic.jneuropallium.master.model.UploadSignalsRequest;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.storages.ISignalStorage;
import com.rakovpublic.jneuropallium.worker.net.storages.ISplitInput;
import com.rakovpublic.jneuropallium.worker.neuron.IAxon;
import com.rakovpublic.jneuropallium.worker.neuron.INeuron;
import com.rakovpublic.jneuropallium.worker.neuron.impl.Neuron;
import com.rakovpublic.jneuropallium.worker.synchronizer.IContext;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class HttpClusterApplication implements IApplication {
    private  final static String UUID = java.util.UUID.randomUUID().toString();
    @Override
    public void startApplication(IContext context) {
        String registerLink = context.getProperty("master.address") + "/nodeManager/register";
        String getSplitInputLink = context.getProperty("master.address") + "/nodeManager/nextRun";
        String sendResultLink = context.getProperty("master.address") + "/input/callback";
        NodeCompleteRequest nodeCompleteRequest = new NodeCompleteRequest();
        nodeCompleteRequest.setNodeName(UUID);
        HttpCommunicationClient communicationClient = new HttpCommunicationClient();

        try {
            communicationClient.sendRequest(HttpRequestResolver.createPost(registerLink,nodeCompleteRequest));
        } catch (IOException e) {
            //TODO: add logger
            return;
        } catch (InterruptedException e) {
            //TODO: add logger
            return;
        }
        String jsonSplitInput;
        while(true){
            try {
                jsonSplitInput = communicationClient.sendRequest(HttpRequestResolver.createPost(getSplitInputLink,nodeCompleteRequest));
            }catch (IOException e) {
                //TODO: add logger
                return;
            } catch (InterruptedException e) {
                //TODO: add logger
                return;
            }
            ISplitInput splitInput = parseSplitInput(jsonSplitInput);
            ISignalStorage signalStorage = splitInput.readInputs();
            for(INeuron neuron: splitInput.getNeurons()){
                neuron.setCurrentLoopAmount(splitInput.getCurrentLoopCount());
                neuron.setRun(splitInput.getRun());
                neuron.addSignals(signalStorage.getSignalsForNeuron(neuron.getId()));
                neuron.setCyclingNeuronInputMapping(splitInput.getServiceInputsMap());
                neuron.processSignals();
                neuron.activate();
                IAxon axon =  neuron.getAxon();
                HashMap<Integer,HashMap<Long,List<ISignal>>> result = axon.getSignalResultStructure(axon.processSignals(neuron.getResult()));
                UploadSignalsRequest uploadSignalsRequest = new UploadSignalsRequest();
                uploadSignalsRequest.setSignals(result);
                uploadSignalsRequest.setName(UUID);
                try {
                    communicationClient.sendRequest(HttpRequestResolver.createPost(sendResultLink,uploadSignalsRequest));
                } catch (IOException e) {
                    //TODO: add logger
                    return;
                } catch (InterruptedException e) {
                    //TODO: add logger
                    return;
                }
            }
        }
    }

    private ISplitInput parseSplitInput(String json){
        return null;
    }






}
