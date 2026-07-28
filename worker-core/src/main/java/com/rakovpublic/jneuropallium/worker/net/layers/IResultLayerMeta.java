/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.layers;

import com.rakovpublic.jneuropallium.worker.net.neuron.IResultNeuron;

import java.util.List;

/**
 * This interface represents the result layer of neuron net
 */
public interface IResultLayerMeta extends ILayerMeta {
    /**
     * This method returns the result neurons list
     *
     * @return neurons list
     */
    List<IResultNeuron> getResultNeurons();

}
