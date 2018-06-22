package web.storages;

import web.neuron.INeuron;

public interface INeuronMeta<K extends INeuron> extends IStorageMeta {
    K toNeuron();
}
