/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.master.services.impl;

import com.rakovpublic.jneuropallium.master.services.ILayerService;
import com.rakovpublic.jneuropallium.worker.net.layers.LayerMove;
import com.rakovpublic.jneuropallium.worker.net.storages.ILayerMeta;
import com.rakovpublic.jneuropallium.worker.net.storages.ILayersMeta;
import com.rakovpublic.jneuropallium.worker.net.storages.ReconnectStrategy;
import com.rakovpublic.jneuropallium.worker.neuron.INeuron;

import java.util.HashMap;
import java.util.List;

public class LayerService implements ILayerService {

    private ILayersMeta layersMeta;

    public LayerService(ILayersMeta layersMeta) {
        this.layersMeta = layersMeta;
    }

    @Override
    public void deleteNeuron(Integer layerId, Long neuronId) {

    }

    @Override
    public  void addNeuron(INeuron neuron, Integer layerId) {
        List<INeuron> neurons =layersMeta.getLayerByID(layerId).getNeurons();
        neurons.add(neuron);
        layersMeta.getLayerByID(layerId).saveNeurons(neurons);

    }


    @Override
    public void deleteLayer(Integer layerId, ReconnectStrategy reconnectStrategy) {

        HashMap<Integer, HashMap<Long, HashMap<Integer, List<Long>>>> updateMap = reconnectStrategy.getNewConnections(layersMeta, layerId);

        for (Integer layersToFix : updateMap.keySet()) {
            ILayerMeta layerMeta = layersMeta.getLayerByID(layersToFix);
            layerMeta.addLayerMove(new LayerMove(updateMap.get(layersToFix), layerId));
        }
        ILayerMeta layerToRemove = layersMeta.getLayerByID(layerId);
        layersMeta.removeLayer(layerToRemove);

    }
}
