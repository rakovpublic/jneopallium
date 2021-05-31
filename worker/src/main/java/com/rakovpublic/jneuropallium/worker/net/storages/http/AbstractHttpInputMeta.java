package com.rakovpublic.jneuropallium.worker.net.storages.http;


import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.storages.ISignalStorage;
import com.rakovpublic.jneuropallium.worker.net.storages.ISplitInput;
import com.rakovpublic.jneuropallium.worker.neuron.INeuron;

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
    private String nodeId;
    private String readInputsEndpoint;
    private String readNeuronsEndpoint;
    private String sendResultEndpoint;

    private HashMap<String, Long> neuronInputNameMapping;
    private Integer currentInnerLoopCount;

    public AbstractHttpInputMeta(String nodeId, String readInputsEndpoint, String readNeuronsEndpoint, String sendResultEndpoint, HashMap<String, Long> neuronInputNameMapping, Integer currentInnerLoopCount) {
        this.nodeId = nodeId;
        this.readInputsEndpoint = readInputsEndpoint;
        this.readNeuronsEndpoint = readNeuronsEndpoint;
        this.sendResultEndpoint = sendResultEndpoint;
        this.neuronInputNameMapping = neuronInputNameMapping;
        this.currentInnerLoopCount = currentInnerLoopCount;
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
        } catch (IOException e) {
            e.printStackTrace();
            //TODO: add logger
        } catch (InterruptedException e) {
            e.printStackTrace();
            //TODO: add logger
        }

        return parseSignalsForNeurons(response);
    }


    @Override
    public void saveResults(HashMap<Long, List<ISignal>> signals) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(sendResultEndpoint))
                .timeout(Duration.ofMinutes(2))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(resultToJSON(signals)))
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
        } catch (IOException e) {
            e.printStackTrace();
            //TODO: add logger
        } catch (InterruptedException e) {
            e.printStackTrace();
            //TODO: add logger
        }
    }

    @Override
    public void setNodeIdentifier(String name) {
        nodeId=name;
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
        } catch (IOException e) {
            e.printStackTrace();
            //TODO: add logger
        } catch (InterruptedException e) {
            e.printStackTrace();
            //TODO: add logger
        }

        return parseNeurons(response);
    }

    @Override
    public void setCycleInputsMap(HashMap<String, Long> neuronInputNameMapping) {
        this.neuronInputNameMapping=neuronInputNameMapping;
    }

    @Override
    public HashMap<String, Long> getCycleInputsMap() {
        return neuronInputNameMapping;
    }

    @Override
    public Integer getCurrentLoopCount() {
        return currentInnerLoopCount;
    }


    protected abstract ISignalStorage parseSignalsForNeurons(HttpResponse<String> response);

    protected abstract List<? extends INeuron> parseNeurons(HttpResponse<String> response);

    protected abstract String resultToJSON(HashMap<Long, List<ISignal>> signals);
}
