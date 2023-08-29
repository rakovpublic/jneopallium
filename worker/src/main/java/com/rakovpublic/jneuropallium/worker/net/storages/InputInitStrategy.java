package com.rakovpublic.jneuropallium.worker.net.storages;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.HashMap;
import java.util.List;

/**
 * This interface incapsulate the logic of passing input signals to input layer of the neuron net
 */
@JsonDeserialize(using = InitInputDeserializer.class)
public interface InputInitStrategy {
    /**
     * @param layersMeta meta data which contains layers information
     * @param signals    list of input signals
     * @return the structure which represents input in format layer id neuron id signals list
     */
    HashMap<Integer, HashMap<Long, List<ISignal>>> getInputs(ILayersMeta layersMeta, List<ISignal> signals);
}
