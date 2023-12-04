/*
 * Copyright (c) 2023. Rakovskyi Dmytro
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
//TODO: add implementation
public class RedisLayerMeta implements ILayer {
    @Override
    public void createNeuron(CreateNeuronSignal signal) {

    }

    @Override
    public LayerMetaParam getLayerMetaParam(String key) {
        return null;
    }

    @Override
    public void updateLayerMetaParam(String key, LayerMetaParam metaParam) {

    }

    @Override
    public void deleteNeuron(DeleteNeuronSignal deleteNeuronIntegration) {

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
    public void setLayerMetaParams(HashMap params) {

    }
}
