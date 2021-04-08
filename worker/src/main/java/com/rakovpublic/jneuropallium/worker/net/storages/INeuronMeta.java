package com.rakovpublic.jneuropallium.worker.net.storages;

import com.rakovpublic.jneuropallium.worker.neuron.INeuron;

public interface INeuronMeta<K extends INeuron> extends IStorageMeta {
    K toNeuron();

    Class<K> getNeuronClass();
}
