package net.storages;

import net.neuron.INeuron;

public interface INeuronMeta<K extends INeuron> extends IStorageMeta {
    K toNeuron();

    Class<K> getNeuronClass();
}
