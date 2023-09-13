package com.rakovpublic.jneuropallium.worker.net.storages.http;


import com.rakovpublic.jneuropallium.master.model.UploadSignalsRequest;
import com.rakovpublic.jneuropallium.worker.application.HttpCommunicationClient;
import com.rakovpublic.jneuropallium.worker.application.HttpRequestResolver;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.storages.ISignalStorage;
import com.rakovpublic.jneuropallium.worker.net.storages.ISplitInput;
import com.rakovpublic.jneuropallium.worker.neuron.INeuron;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.Authenticator;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;

public abstract class AbstractHttpInputMeta implements ISplitInput {
    private static final Logger logger = LogManager.getLogger(AbstractHttpInputMeta.class);
    private String nodeId;
    private String readInputsEndpoint;
    private String readNeuronsEndpoint;
    private String sendResultEndpoint;

    private HashMap<String, Long> neuronInputNameMapping;
    private Integer currentInnerLoopCount;
    private Long run;

    private Long start;
    private Long end;

    private Integer layerId;

    public AbstractHttpInputMeta(Long run, String nodeId, String readInputsEndpoint, String readNeuronsEndpoint, String sendResultEndpoint, HashMap<String, Long> neuronInputNameMapping, Integer currentInnerLoopCount, Long start, Long end, Integer layerId) {
        this.run = run;
        this.nodeId = nodeId;
        this.readInputsEndpoint = readInputsEndpoint;
        this.readNeuronsEndpoint = readNeuronsEndpoint;
        this.sendResultEndpoint = sendResultEndpoint;
        this.neuronInputNameMapping = neuronInputNameMapping;
        this.currentInnerLoopCount = currentInnerLoopCount;
        this.start = start;
        this.end = end;
        this.layerId = layerId;
    }

    @Override
    public ISignalStorage readInputs() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(readInputsEndpoint))
                .timeout(Duration.ofMinutes(2))
                .header("Content-Type", "application/json")
                .GET()
                .build();
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(20))
                .authenticator(Authenticator.getDefault())
                .build();
        HttpResponse<String> response = null;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            logger.error("cannot send request", e);
        }

        return parseSignalsForNeurons(response);
    }


    @Override
    public void saveResults(HashMap<Integer, HashMap<Long, List<ISignal>>> signals) {
        HttpCommunicationClient communicationClient = new HttpCommunicationClient();
        UploadSignalsRequest uploadSignalsRequest = new UploadSignalsRequest();
        uploadSignalsRequest.setSignals(signals);
        uploadSignalsRequest.setName(nodeId);
        try {
            communicationClient.sendRequest(HttpRequestResolver.createPost(sendResultEndpoint, uploadSignalsRequest));
        } catch (IOException | InterruptedException e) {
            logger.error("cannot send request", e);
        }
    }

    @Override
    public void setNodeIdentifier(String name) {
        nodeId = name;
    }

    @Override
    public String getNodeIdentifier() {
        return nodeId;
    }

    @Override
    public List<? extends INeuron> getNeurons() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(readNeuronsEndpoint))
                .timeout(Duration.ofMinutes(2))
                .header("Content-Type", "application/json")
                .GET()
                .build();
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(20))
                .authenticator(Authenticator.getDefault())
                .build();
        HttpResponse<String> response = null;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            logger.error("cannot send request", e);
        }

        return parseNeurons(response);
    }

    @Override
    public void setServiceInputsMap(HashMap<String, Long> neuronInputNameMapping) {
        this.neuronInputNameMapping = neuronInputNameMapping;
    }

    @Override
    public HashMap<String, Long> getServiceInputsMap() {
        return neuronInputNameMapping;
    }

    @Override
    public Integer getEpoch() {
        return currentInnerLoopCount;
    }

    @Override
    public void setRun(Long run) {
        this.run = run;
    }

    @Override
    public Long getRun() {
        return run;
    }

    protected abstract ISignalStorage parseSignalsForNeurons(HttpResponse<String> response);

    protected abstract List<? extends INeuron> parseNeurons(HttpResponse<String> response);

    protected abstract String resultToJSON(HashMap<Long, List<ISignal>> signals);

    @Override
    public Long getStart() {
        return start;
    }

    @Override
    public void setStart(Long start) {
        this.start = start;
    }

    @Override
    public Long getEnd() {
        return end;
    }

    @Override
    public void setEnd(Long end) {
        this.end = end;
    }

    @Override
    public Integer getLayerId() {
        return layerId;
    }

    public void setLayerId(Integer layerId) {
        this.layerId = layerId;
    }
}
