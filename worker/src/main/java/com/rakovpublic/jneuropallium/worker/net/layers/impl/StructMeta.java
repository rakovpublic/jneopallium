/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.layers.impl;

import com.rakovpublic.jneuropallium.worker.net.layers.ILayerMeta;
import com.rakovpublic.jneuropallium.worker.net.layers.ILayersMeta;
import com.rakovpublic.jneuropallium.worker.net.layers.IResultLayerMeta;
import com.rakovpublic.jneuropallium.worker.net.layers.IStructMeta;
import com.rakovpublic.jneuropallium.worker.net.neuron.INeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ReconnectStrategy;
import com.rakovpublic.jneuropallium.worker.net.signals.storage.IInputResolver;
import com.rakovpublic.jneuropallium.worker.net.study.ILearningRequest;

import java.util.HashMap;
import java.util.List;


public class StructMeta implements IStructMeta {


    private final IInputResolver inputResolver;
    private final ILayersMeta layersMeta;


    public StructMeta(IInputResolver hiddenInputMeta, ILayersMeta layersMeta) {

        this.inputResolver = hiddenInputMeta;
        this.layersMeta = layersMeta;
    }

    void init() {
    }

    @Override
    public List<ILayerMeta> getLayers() {
        return layersMeta.getLayers();
    }


    @Override
    public IInputResolver getInputResolver() {

        return inputResolver;
    }


    @Override
    public void learn(List<ILearningRequest> requests) {
        for (ILearningRequest request : requests) {
            ILayerMeta lm = layersMeta.getLayerById(request.getLayerId());
            INeuron ner = lm.getNeuronByID(request.getNeuronId());
            ner.getAxon().resetConnection(request.getNewConnections());
        }
        for (ILayerMeta meta : layersMeta.getLayers()) {
            meta.dumpLayer();
        }
        layersMeta.getResultLayer().dumpLayer();
    }

    @Override
    public IResultLayerMeta getResultLayer() {
        return layersMeta.getResultLayer();
    }

    @Override
    public void removeLayer(Integer layerId, ReconnectStrategy reconnectStrategy) {

        HashMap<Integer, HashMap<Long, HashMap<Integer, List<Long>>>> updateMap = reconnectStrategy.getNewConnections(layersMeta, layerId);

        for (Integer layersToFix : updateMap.keySet()) {
            ILayerMeta layerMeta = layersMeta.getLayerById(layersToFix);
            layerMeta.addLayerMove(new LayerMove(updateMap.get(layersToFix), layerId));
        }
        ILayerMeta layerToRemove = layersMeta.getLayerById(layerId);
        layersMeta.removeLayer(layerToRemove);
    }


}
