package com.rakovpublic.jneuropallium.worker.net.layers.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.rakovpublic.jneuropallium.master.model.CreateNeuronRequest;
import com.rakovpublic.jneuropallium.master.model.DeleteNeuronRequest;
import com.rakovpublic.jneuropallium.master.model.UploadSignalsRequest;
import com.rakovpublic.jneuropallium.worker.application.HttpCommunicationClient;
import com.rakovpublic.jneuropallium.worker.application.HttpRequestResolver;
import com.rakovpublic.jneuropallium.worker.net.layers.ILayer;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.storages.ILayerMeta;
import com.rakovpublic.jneuropallium.worker.net.storages.INeuronSerializer;
import com.rakovpublic.jneuropallium.worker.neuron.INeuron;
import com.rakovpublic.jneuropallium.worker.neuron.IRule;
import com.rakovpublic.jneuropallium.worker.neuron.impl.layersizing.CreateNeuronSignal;
import com.rakovpublic.jneuropallium.worker.neuron.impl.layersizing.DeleteNeuronSignal;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class HttpLayer implements ILayer {
    private String masterAddress;
    private Integer layerId;
    private String UUID;

    public HttpLayer(String masterAddress, Integer layerId, String UUID) {
        this.masterAddress = masterAddress;
        this.layerId = layerId;
        this.UUID = UUID;
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

            //TODO: add logger return
            return;
        }
        CreateNeuronRequest createNeuronRequest = new CreateNeuronRequest();
        createNeuronRequest.setLayerId(layerId);
        createNeuronRequest.setNeuronJson(json);
        createNeuronRequest.setNeuronClass(neuron.getCurrentNeuronClass().getCanonicalName());
        try {
            communicationClient.sendRequest(HttpRequestResolver.createPost(createUrl, createNeuronRequest));
        } catch (IOException e) {
            //TODO: add logger
            return;
        } catch (InterruptedException e) {
            //TODO: add logger
            return;
        }
        UploadSignalsRequest uploadSignalsRequest = new UploadSignalsRequest();
        uploadSignalsRequest.setSignals(signal.getValue().getCreateRelationsSignals());
        uploadSignalsRequest.setName(UUID);
        try {
            communicationClient.sendRequest(HttpRequestResolver.createPost(sendResultLink, uploadSignalsRequest));
        } catch (IOException e) {
            //TODO: add logger
            return;
        } catch (InterruptedException e) {
            //TODO: add logger
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
        } catch (IOException e) {
            //TODO: add logger return
            return;
        } catch (InterruptedException e) {
            //TODO: add logger return
            return;
        }
        UploadSignalsRequest uploadSignalsRequest = new UploadSignalsRequest();
        uploadSignalsRequest.setSignals(deleteNeuronIntegration.getValue().getCreateRelationsSignals());
        uploadSignalsRequest.setName(UUID);
        try {
            communicationClient.sendRequest(HttpRequestResolver.createPost(sendResultLink, uploadSignalsRequest));
        } catch (IOException e) {
            //TODO: add logger
            return;
        } catch (InterruptedException e) {
            //TODO: add logger
            return;
        }
    }

    @Override
    public long getLayerSize() {
        return 0;
    }

    @Override
    public Boolean validateGlobal() {
        return null;
    }

    @Override
    public Boolean validateLocal() {
        return null;
    }

    @Override
    public void addGlobalRule(IRule rule) {

    }

    @Override
    public void register(INeuron neuron) {

    }

    @Override
    public void registerAll(List neuron) {

    }

    @Override
    public void addInput(ISignal signal, Long neuronId) {

    }

    @Override
    public void process() {

    }

    @Override
    public int getId() {
        return 0;
    }

    @Override
    public Boolean isProcessed() {
        return null;
    }

    @Override
    public void dumpResult() {

    }

    @Override
    public void dumpNeurons(ILayerMeta layerMeta) {

    }

    @Override
    public HashMap<Integer, HashMap<Long, List<ISignal>>> getResults() {
        return null;
    }

    @Override
    public String toJSON() {
        return null;
    }

    @Override
    public void addNeuronSerializer(INeuronSerializer serializer) {

    }

    @Override
    public void sendCallBack(String name, List list) {

    }

    @Override
    public INeuronSerializer getNeuronSerializer(Class neuronClass) {
        return null;
    }
}
