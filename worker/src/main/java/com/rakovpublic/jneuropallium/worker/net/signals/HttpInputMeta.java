/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.signals;


import com.rakovpublic.jneuropallium.worker.net.signals.storage.IInputResolver;
import com.rakovpublic.jneuropallium.worker.net.layers.ILayer;
import com.rakovpublic.jneuropallium.worker.net.signals.storage.ISplitInput;
import com.rakovpublic.jneuropallium.worker.net.layers.ILayersMeta;
import com.rakovpublic.jneuropallium.worker.net.neuron.INeuron;
import com.rakovpublic.jneuropallium.worker.util.NeuronParser;
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
//TODO: add implementation
public class HttpInputMeta implements ISplitInput {
    private static final Logger logger = LogManager.getLogger(HttpInputMeta.class);
    private String nodeId;
    private String readNeuronsEndpoint;
    private String sendResultEndpoint;
    private IInputResolver inputResolver;
    private ILayer layer;


    private Long start;
    private Long end;


    public HttpInputMeta(String nodeId, String readNeuronsEndpoint, String sendResultEndpoint, IInputResolver inputResolver, ILayer layer, Long start, Long end) {
        this.nodeId = nodeId;
        this.readNeuronsEndpoint = readNeuronsEndpoint;
        this.sendResultEndpoint = sendResultEndpoint;
        this.inputResolver = inputResolver;
        this.layer = layer;
        this.start = start;
        this.end = end;
    }

    @Override
    public String getDiscriminatorName() {
        return null;
    }

    @Override
    public void setDiscriminatorName(String name) {

    }

    @Override
    public IInputResolver getInputResolver() {
        return inputResolver;
    }

    @Override
    public void saveNeuron(INeuron neuron) {

    }

    @Override
    public void setLayer(Integer layerId) {
        this.layer = layer;
    }

    @Override
    public void applyMeta(ILayersMeta layersMeta) {

    }
    /*

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
    }*/


    @Override
    public void saveResults(HashMap<Integer, HashMap<Long, List<ISignal>>> signals) {
        inputResolver.getSignalPersistStorage().putSignals(signals);
    }

    @Override
    public void setNodeIdentifier(String name) {
        nodeId = name;
    }

    @Override
    public ISplitInput getNewInstance() {
        return new HttpInputMeta(this.nodeId, this.readNeuronsEndpoint, this.sendResultEndpoint, this.inputResolver, this.layer, this.start, this.end);
    }

    @Override
    public String getNodeIdentifier() {
        return nodeId;
    }

    @Override
    public List<? extends INeuron> getNeurons() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(readNeuronsEndpoint + "?layerId=" + layer.getId() + "&startIndex=" + start + "&endIndex=" + end))
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

    protected List<? extends INeuron> parseNeurons(HttpResponse<String> response) {
        return NeuronParser.parseNeurons(response.body());
    }


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
        return this.layer.getId();
    }
}
