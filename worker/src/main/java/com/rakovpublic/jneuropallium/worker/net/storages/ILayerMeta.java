package com.rakovpublic.jneuropallium.worker.net.storages;

import com.rakovpublic.jneuropallium.worker.neuron.INeuron;

import java.util.Collection;
import java.util.List;

public interface ILayerMeta extends IStorageMeta {
    int getID();

    List<? extends INeuron> getNeurons();

    INeuron getNeuronByID(Long id);

    void saveNeurons(Collection<? extends INeuron> neuronMetas);

    void dumpLayer();

    Long getSize();
}
