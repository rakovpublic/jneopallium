/*
 * Copyright (c) 2026. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.layers.impl.redis;

import com.rakovpublic.jneuropallium.worker.net.layers.ILayer;
import com.rakovpublic.jneuropallium.worker.net.layers.ILayerMeta;
import com.rakovpublic.jneuropallium.worker.net.layers.impl.LayerMetaParam;
import com.rakovpublic.jneuropallium.worker.net.neuron.INeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.IRule;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.layersizing.CreateNeuronSignal;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.layersizing.DeleteNeuronSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The layer handle a neuron sees while a worker processes it.
 * <p>
 * In cluster mode the processing loop lives in the application, not in the layer, so this
 * class only has to answer questions about the layer and forward structural changes to the
 * shared Redis layer configuration.
 */
public class RedisLayer<N extends INeuron> implements ILayer<N> {
    private final RedisLayerMeta layerMeta;

    public RedisLayer(RedisLayerMeta layerMeta) {
        this.layerMeta = layerMeta;
    }

    @Override
    public <K extends CreateNeuronSignal> void createNeuron(K signal) {
        layerMeta.addNeuron(signal.getValue().getNeuron());
    }

    @Override
    public void deleteNeuron(DeleteNeuronSignal deleteNeuronIntegration) {
        layerMeta.removeNeuron(deleteNeuronIntegration.getValue().getNeuronId());
    }

    @Override
    public LayerMetaParam getLayerMetaParam(String key) {
        return layerMeta.getLayerMetaParams().get(key);
    }

    @Override
    public void updateLayerMetaParam(String key, LayerMetaParam metaParam) {
        HashMap<String, LayerMetaParam> params = layerMeta.getLayerMetaParams();
        params.put(key, metaParam);
        layerMeta.setLayerMetaParams(params);
    }

    @Override
    public void setLayerMetaParams(HashMap<String, LayerMetaParam> params) {
        layerMeta.setLayerMetaParams(params);
    }

    @Override
    public long getLayerSize() {
        Long size = layerMeta.getSize();
        return size == null ? 0L : size;
    }

    @Override
    public Boolean validateGlobal() {
        return true;
    }

    @Override
    public Boolean validateLocal() {
        return true;
    }

    @Override
    public void addGlobalRule(IRule rule) {
        // Validation rules are part of the layer meta params in a Redis backed net.
    }

    @Override
    public void register(N neuron) {
        layerMeta.addNeuron(neuron);
    }

    @Override
    public void registerAll(List<? extends N> neurons) {
        layerMeta.saveNeurons((List<INeuron>) neurons);
    }

    @Override
    public void process() {
        throw new UnsupportedOperationException("In cluster mode neurons are processed by the worker application");
    }

    @Override
    public int getId() {
        return layerMeta.getID();
    }

    @Override
    public Boolean isProcessed() {
        return true;
    }

    @Override
    public void dumpResult() {
        // Results are written to the shared signal storage by the split input.
    }

    @Override
    public void dumpNeurons(ILayerMeta meta) {
        meta.dumpLayer();
    }

    @Override
    public HashMap<Integer, HashMap<Long, CopyOnWriteArrayList<ISignal>>> getResults() {
        return new HashMap<>();
    }

    @Override
    public String toJSON() {
        return "{\"layerID\":" + getId() + "}";
    }

    @Override
    public void sendCallBack(String name, List<ISignal> signals) {
        // Upstream callbacks are routed by the master; a worker layer has no upstream link.
    }

    @Override
    public void processWeights() {
        throw new UnsupportedOperationException("In cluster mode weights are processed by the worker application");
    }
}
