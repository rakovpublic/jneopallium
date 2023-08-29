package com.rakovpublic.jneuropallium.worker.net.storages;

import com.rakovpublic.jneuropallium.worker.neuron.INeuron;

/**
 * Interface the neuron serializing deserializing
 */
public interface INeuronSerializer<N extends INeuron> extends ISerializer<N, String> {
}
