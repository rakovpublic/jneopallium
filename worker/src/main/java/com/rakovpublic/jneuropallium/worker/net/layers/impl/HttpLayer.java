package com.rakovpublic.jneuropallium.worker.net.layers.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.rakovpublic.jneuropallium.worker.application.HttpCommunicationClient;
import com.rakovpublic.jneuropallium.worker.application.HttpRequestResolver;
import com.rakovpublic.jneuropallium.worker.model.CreateNeuronRequest;
import com.rakovpublic.jneuropallium.worker.model.DeleteNeuronRequest;
import com.rakovpublic.jneuropallium.worker.model.UploadSignalsRequest;
import com.rakovpublic.jneuropallium.worker.net.layers.ILayer;
import com.rakovpublic.jneuropallium.worker.net.layers.LayerMetaParam;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.storages.ILayerMeta;
import com.rakovpublic.jneuropallium.worker.neuron.INeuron;
import com.rakovpublic.jneuropallium.worker.neuron.IRule;
import com.rakovpublic.jneuropallium.worker.neuron.impl.layersizing.CreateNeuronSignal;
import com.rakovpublic.jneuropallium.worker.neuron.impl.layersizing.DeleteNeuronSignal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class HttpLayer implements ILayer {
    private static final Logger logger = LogManager.getLogger(HttpLayer.class);
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
            return;
        }
    }

    //TODO: add implementation
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
    public void sendCallBack(String name, List list) {

    }


    @Override
    public LayerMetaParam getLayerMetaParam(String key) {
        return null;
    }

    @Override
    public void updateLayerMetaParam(String key, LayerMetaParam metaParam) {

    }

    @Override
    public void setLayerMetaParams(HashMap params) {

    }
}
