package com.rakovpublic.jneuropallium.master.services;

import com.rakovpublic.jneuropallium.worker.model.LayerParamUpdate;
import com.rakovpublic.jneuropallium.worker.net.layers.impl.LayerMetaParam;
import com.rakovpublic.jneuropallium.worker.net.neuron.INeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ReconnectStrategy;

public interface ILayerService {
    void deleteNeuron(Integer layerId, Long neuronId);

    void addNeuron(INeuron neuron, Integer layerId);

    void deleteLayer(Integer layerId, ReconnectStrategy reconnectStrategy);

    LayerMetaParam getMetaParam(String name, Integer layerId);

    void updateMetaParam(LayerParamUpdate layerParamUpdate);

}
