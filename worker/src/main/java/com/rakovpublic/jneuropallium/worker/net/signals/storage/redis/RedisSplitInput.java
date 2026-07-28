/*
 * Copyright (c) 2026. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.signals.storage.redis;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.rakovpublic.jneuropallium.worker.net.layers.ILayersMeta;
import com.rakovpublic.jneuropallium.worker.net.layers.impl.redis.RedisInputResolver;
import com.rakovpublic.jneuropallium.worker.net.layers.impl.redis.RedisLayer;
import com.rakovpublic.jneuropallium.worker.net.layers.impl.redis.RedisLayerMeta;
import com.rakovpublic.jneuropallium.worker.net.neuron.INeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.storage.IInputResolver;
import com.rakovpublic.jneuropallium.worker.net.signals.storage.ISplitInput;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A work assignment for one worker: a neuron range of one layer, plus the coordinates of the
 * Redis instance that holds the net.
 * <p>
 * This is the object the master serialises into the {@code /nodeManager/nextRun} response, so
 * everything it carries travels over the wire on every assignment. It deliberately carries no
 * neurons, no layer configuration and no signals - only ids and ranges. Both sides resolve
 * those against Redis, which keeps the payload at a few hundred bytes no matter how large the
 * model is.
 */
public class RedisSplitInput implements ISplitInput {
    private final String host;
    private final Integer port;
    private final String neuronNetName;
    private final Integer threads;
    private String nodeId;
    private String discriminatorName;
    private Integer layerId;
    private Long start;
    private Long end;
    private Long run;
    private Integer loop;
    private HashMap<String, Long> cycleNeuronMapping;

    @JsonCreator
    public RedisSplitInput(@JsonProperty("host") String host,
                           @JsonProperty("port") Integer port,
                           @JsonProperty("neuronNetName") String neuronNetName,
                           @JsonProperty("threads") Integer threads,
                           @JsonProperty("nodeId") String nodeId,
                           @JsonProperty("discriminatorName") String discriminatorName,
                           @JsonProperty("layerId") Integer layerId,
                           @JsonProperty("start") Long start,
                           @JsonProperty("end") Long end,
                           @JsonProperty("run") Long run,
                           @JsonProperty("loop") Integer loop,
                           @JsonProperty("cycleNeuronMapping") HashMap<String, Long> cycleNeuronMapping) {
        this.host = host;
        this.port = port;
        this.neuronNetName = neuronNetName;
        this.threads = threads == null ? 1 : threads;
        this.nodeId = nodeId;
        this.discriminatorName = discriminatorName;
        this.layerId = layerId;
        this.start = start;
        this.end = end;
        this.run = run;
        this.loop = loop;
        this.cycleNeuronMapping = cycleNeuronMapping;
    }

    public String getHost() {
        return host;
    }

    public Integer getPort() {
        return port;
    }

    public String getNeuronNetName() {
        return neuronNetName;
    }

    public Long getRun() {
        return run;
    }

    public Integer getLoop() {
        return loop;
    }

    public HashMap<String, Long> getCycleNeuronMapping() {
        return cycleNeuronMapping;
    }

    @Override
    public String getDiscriminatorName() {
        return discriminatorName;
    }

    @Override
    public void setDiscriminatorName(String name) {
        this.discriminatorName = name;
    }

    @Override
    @JsonIgnore
    public IInputResolver getInputResolver() {
        return new RedisInputResolver(host, port, neuronNetName, run, loop, cycleNeuronMapping);
    }

    @Override
    public void saveResults(HashMap<Integer, HashMap<Long, CopyOnWriteArrayList<ISignal>>> signals) {
        new RedisSignalStorage(host, port, neuronNetName).putSignals(signals);
    }

    @Override
    public void saveNeuron(INeuron neuron) {
        layerMeta(neuron.getLayer() == null ? layerId : neuron.getLayer().getId()).addNeuron(neuron);
    }

    @Override
    public void saveNeurons(List<? extends INeuron> neurons) {
        if (neurons == null || neurons.isEmpty()) {
            return;
        }
        layerMeta(layerId).saveNeurons(new ArrayList<>(neurons));
    }

    @Override
    public void setNodeIdentifier(String name) {
        this.nodeId = name;
    }

    @Override
    @JsonIgnore
    public ISplitInput getNewInstance() {
        return new RedisSplitInput(host, port, neuronNetName, threads, nodeId, discriminatorName, layerId,
                start, end, run, loop, cycleNeuronMapping);
    }

    @Override
    @JsonIgnore
    public List<? extends INeuron> getNeurons() {
        RedisLayerMeta meta = layerMeta(layerId);
        List<INeuron> neurons = meta.getNeurons(start, end);
        RedisLayer<INeuron> layer = new RedisLayer<>(meta);
        for (INeuron neuron : neurons) {
            neuron.setLayer(layer);
        }
        return neurons;
    }

    @Override
    public String getNodeIdentifier() {
        return nodeId;
    }

    @Override
    public Long getStart() {
        return start;
    }

    @Override
    public Long getEnd() {
        return end;
    }

    @Override
    public void setStart(Long start) {
        this.start = start;
    }

    @Override
    public void setEnd(Long end) {
        this.end = end;
    }

    @Override
    public Integer getLayerId() {
        return layerId;
    }

    @Override
    public void setLayer(Integer layerId) {
        this.layerId = layerId;
    }

    @Override
    public void applyMeta(ILayersMeta layersMeta) {
        // Layers are addressed by id in Redis, so there is no meta object to carry along.
    }

    @Override
    public void applyRunState(Long run, Integer loop, HashMap<String, Long> cycleNeuronMapping) {
        this.run = run;
        this.loop = loop;
        this.cycleNeuronMapping = cycleNeuronMapping;
    }

    @Override
    public Integer getThreads() {
        return threads;
    }

    private RedisLayerMeta layerMeta(Integer id) {
        return new RedisLayerMeta(host, port, neuronNetName, id);
    }
}
