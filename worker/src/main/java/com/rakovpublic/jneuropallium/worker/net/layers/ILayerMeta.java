/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.layers;

import com.rakovpublic.jneuropallium.worker.net.storages.IStorageMeta;
import com.rakovpublic.jneuropallium.worker.net.neuron.INeuron;

import java.util.HashMap;
import java.util.List;

/**
 * This interface represents layer storage
 */
public interface ILayerMeta extends IStorageMeta {

    HashMap<String, LayerMetaParam> getLayerMetaParams();

    void setLayerMetaParams(HashMap<String, LayerMetaParam> metaParams);

    /**
     * @return layer id
     */
    int getID();

    void addLayerMove(LayerMove layerMove);


    /**
     * @return list of layer neurons
     */
    List<INeuron> getNeurons();

    /**
     * @param id neuron id
     * @return neuron
     */
    INeuron getNeuronByID(Long id);

    void removeNeuron(Long neuron);

    void addNeuron(INeuron neuron);

    /**
     * @param neurons list of neurons to save
     */
    void saveNeurons(List<INeuron> neurons);

    /**
     * this method persist layer
     */
    void dumpLayer();

    /**
     * @return the amount of neurons in the layer
     */
    Long getSize();
}
