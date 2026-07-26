/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.signals;

import com.rakovpublic.jneuropallium.worker.net.layers.ILayersMeta;

import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * This interface incapsulate the logic of passing input signals to input layer of the neuron net
 */

public interface InputInitStrategy {
    /**
     * @param layersMeta meta data which contains layers information
     * @param signals    list of input signals
     * @return the structure which represents input in format layer id neuron id signals list
     */
    HashMap<Integer, HashMap<Long, CopyOnWriteArrayList<ISignal>>> getInputs(ILayersMeta layersMeta, CopyOnWriteArrayList<ISignal> signals);
}
