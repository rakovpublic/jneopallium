/*
 * Copyright (c) 2024. Rakovskyi Dmytro
 */


package com.rakovpublic.jneuropallium.worker.util;

import com.rakovpublic.jneuropallium.worker.net.neuron.INeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;

import java.util.HashMap;

public abstract class NeuronStatisticalProperties<K extends INeuron> {
    public Float getProbability(Integer layerId) {
        return probability.get(layerId);
    }

    protected HashMap<Integer, Float> probability;
    protected HashMap<  ISignalProcessor, Float> processorProbabilityMap;

    public HashMap< ISignalProcessor, Float> getProcessorProbabilityMap() {
        return processorProbabilityMap;
    }

    public NeuronStatisticalProperties(HashMap<Integer, Float> probability, HashMap<  ISignalProcessor, Float> processorProbabilityMap) {
        this.probability = probability;
        this.processorProbabilityMap = processorProbabilityMap;
    }

    public abstract K getNeuronInstance(Long neuronId, Integer layerId);
}
