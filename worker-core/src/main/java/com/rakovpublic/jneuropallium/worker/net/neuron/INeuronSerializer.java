/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.neuron;

import com.rakovpublic.jneuropallium.worker.net.storages.ISerializer;

/**
 * Interface the neuron serializing deserializing
 */
public interface INeuronSerializer<N extends INeuron> extends ISerializer<N, String> {
}
