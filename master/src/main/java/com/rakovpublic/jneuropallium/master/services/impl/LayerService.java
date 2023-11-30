/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.master.services.impl;

import com.rakovpublic.jneuropallium.master.services.ConfigurationService;
import com.rakovpublic.jneuropallium.master.services.ILayerService;
import com.rakovpublic.jneuropallium.worker.model.LayerParamUpdate;
import com.rakovpublic.jneuropallium.worker.net.layers.ILayerMeta;
import com.rakovpublic.jneuropallium.worker.net.layers.LayerMetaParam;
import com.rakovpublic.jneuropallium.worker.net.layers.LayerMove;
import com.rakovpublic.jneuropallium.worker.net.neuron.INeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ReconnectStrategy;
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
        configurationService.getInputService().getLayersMeta().getLayerById(layerId).removeNeuron(neuronId);
    }

    @Override
    public void addNeuron(INeuron neuron, Integer layerId) {
        configurationService.getInputService().getLayersMeta().getLayerById(layerId).addNeuron(neuron);
    }


    @Override
    public void deleteLayer(Integer layerId, ReconnectStrategy reconnectStrategy) {

        HashMap<Integer, HashMap<Long, HashMap<Integer, List<Long>>>> updateMap = reconnectStrategy.getNewConnections(configurationService.getInputService().getLayersMeta(), layerId);

        for (Integer layersToFix : updateMap.keySet()) {
            ILayerMeta layerMeta = configurationService.getInputService().getLayersMeta().getLayerById(layersToFix);
            layerMeta.addLayerMove(new LayerMove(updateMap.get(layersToFix), layerId));
        }
        ILayerMeta layerToRemove = configurationService.getInputService().getLayersMeta().getLayerById(layerId);
        configurationService.getInputService().getLayersMeta().removeLayer(layerToRemove);

    }

    @Override
    public LayerMetaParam getMetaParam(String name, Integer layerId) {

        return configurationService.getInputService().getLayersMeta().getLayerById(layerId).getLayerMetaParams().get(name);
    }

    @Override
    public void updateMetaParam(LayerParamUpdate layerParamUpdate) {
        HashMap<String, LayerMetaParam> params = configurationService.getInputService().getLayersMeta().getLayerById(layerParamUpdate.getLayerId()).getLayerMetaParams();
        params.put(layerParamUpdate.getParamName(), layerParamUpdate.getLayerMetaParam());

    }
}
