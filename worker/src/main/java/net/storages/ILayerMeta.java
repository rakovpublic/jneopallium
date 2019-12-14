package net.storages;

import net.neuron.INeuron;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

public interface ILayerMeta extends IStorageMeta {
    List<INeuronMeta<? extends INeuron>> getNeurons();

    void saveNeurons(Collection<? extends INeuron> neuronMetas);

    void dumpLayer();
}
