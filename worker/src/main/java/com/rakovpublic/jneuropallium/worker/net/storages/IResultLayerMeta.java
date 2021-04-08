package com.rakovpublic.jneuropallium.worker.net.storages;

import com.rakovpublic.jneuropallium.worker.neuron.IResultNeuron;

import java.util.List;

public interface IResultLayerMeta extends ILayerMeta {
    List<? extends IResultNeuron> getNeurons();

}
