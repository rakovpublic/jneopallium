/*
 * Copyright (c) 2024. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.signals.storage.http;

import com.rakovpublic.jneuropallium.worker.net.layers.ILayersMeta;
import com.rakovpublic.jneuropallium.worker.net.neuron.INeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.storage.IInputResolver;
import com.rakovpublic.jneuropallium.worker.net.signals.storage.ISplitInput;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.List;
//TODO: add implementation
public class GRPCSplitInput implements ISplitInput {
    private static final Logger logger = LogManager.getLogger(GRPCSplitInput.class);
    private String nodeId;
    private IInputResolver inputResolver;
    private Long start;
    private Long end;
    private ILayersMeta layersMeta;
    private String discriminatorName;
    private Integer layerId;
    private Integer threads;

    public GRPCSplitInput(String nodeId, IInputResolver inputResolver, Long start, Long end, ILayersMeta layersMeta, String discriminatorName, Integer layerId, Integer threads) {
        this.nodeId = nodeId;
        this.inputResolver = inputResolver;
        this.start = start;
        this.end = end;
        this.layersMeta = layersMeta;
        this.discriminatorName = discriminatorName;
        this.layerId = layerId;
        this.threads = threads;
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
        return null;
    }

    @Override
    public void saveResults(HashMap<Integer, HashMap<Long, List<ISignal>>> signals) {

    }

    @Override
    public void saveNeuron(INeuron neuron) {

    }

    @Override
    public void setNodeIdentifier(String name) {

    }

    @Override
    public ISplitInput getNewInstance() {
        return null;
    }

    @Override
    public List<? extends INeuron> getNeurons() {
        return null;
    }

    @Override
    public String getNodeIdentifier() {
        return null;
    }

    @Override
    public Long getStart() {
        return null;
    }

    @Override
    public Long getEnd() {
        return null;
    }

    @Override
    public void setStart(Long start) {

    }

    @Override
    public void setEnd(Long end) {

    }

    @Override
    public Integer getLayerId() {
        return null;
    }

    @Override
    public void setLayer(Integer layerId) {

    }

    @Override
    public void applyMeta(ILayersMeta layersMeta) {

    }

    @Override
    public Integer getThreads() {
        return null;
    }
}
