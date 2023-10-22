/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.master.services.impl;

import com.rakovpublic.jneuropallium.master.services.ConfigurationService;
import com.rakovpublic.jneuropallium.master.services.ILayerService;
import com.rakovpublic.jneuropallium.worker.net.layers.LayerMove;
import com.rakovpublic.jneuropallium.worker.net.storages.ILayerMeta;
import com.rakovpublic.jneuropallium.worker.net.storages.ILayersMeta;
import com.rakovpublic.jneuropallium.worker.net.storages.ReconnectStrategy;
import com.rakovpublic.jneuropallium.worker.neuron.INeuron;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;

@Service
public class LayerService implements ILayerService {

    private ConfigurationService configurationService;

    @Autowired
    public LayerService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    @Override
    public void deleteNeuron(Integer layerId, Long neuronId) {

    }

    @Override
    public  void addNeuron(INeuron neuron, Integer layerId) {
        List<INeuron> neurons =configurationService.getInputService().getLayersMeta().getLayerByID(layerId).getNeurons();
        neurons.add(neuron);
        configurationService.getInputService().getLayersMeta().getLayerByID(layerId).saveNeurons(neurons);

    }


    @Override
    public void deleteLayer(Integer layerId, ReconnectStrategy reconnectStrategy) {

        HashMap<Integer, HashMap<Long, HashMap<Integer, List<Long>>>> updateMap = reconnectStrategy.getNewConnections(configurationService.getInputService().getLayersMeta(), layerId);

        for (Integer layersToFix : updateMap.keySet()) {
            ILayerMeta layerMeta = configurationService.getInputService().getLayersMeta().getLayerByID(layersToFix);
            layerMeta.addLayerMove(new LayerMove(updateMap.get(layersToFix), layerId));
        }
        ILayerMeta layerToRemove = configurationService.getInputService().getLayersMeta().getLayerByID(layerId);
        configurationService.getInputService().getLayersMeta().removeLayer(layerToRemove);

    }
}
