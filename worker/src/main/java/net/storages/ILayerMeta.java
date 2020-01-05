package net.storages;

import net.neuron.INeuron;

import java.io.IOException;
import java.io.Serializable;
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
