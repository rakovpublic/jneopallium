package com.rakovpublic.jneuropallium.worker.net.storages;

import com.rakovpublic.jneuropallium.worker.net.layers.LayerMetaParam;
import com.rakovpublic.jneuropallium.worker.net.layers.LayerMove;
import com.rakovpublic.jneuropallium.worker.neuron.INeuron;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * This interface represents layer storage
 */
public interface ILayerMeta extends IStorageMeta {

    //TODO: add meta info with default weights for axon and dendrite




    HashMap<String, LayerMetaParam> getLayerMetaParams();

    void setLayerMetaParams( HashMap<String, LayerMetaParam>  metaParams);

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
