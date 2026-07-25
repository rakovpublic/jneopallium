/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.neuron;

import com.rakovpublic.jneuropallium.worker.net.storages.IStorageMeta;

/**
 * This interface represents neuron wrapper for storage
 */
public interface INeuronMeta<K extends INeuron> extends IStorageMeta {

    K toNeuron();

    Class<K> getNeuronClass();
}
