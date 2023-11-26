package com.rakovpublic.jneuropallium.worker.util;

import com.rakovpublic.jneuropallium.worker.net.neuron.INeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;

import java.util.HashMap;

public abstract class NeuronStatisticalProperties<K extends INeuron> {
    public Float getProbability(Integer layerId) {
        return probability.get(layerId);
    }

    private HashMap<Integer, Float> probability;
    private HashMap<Class<? extends ISignalProcessor>, Float> processorProbabilityMap;

    public NeuronStatisticalProperties(HashMap<Integer, Float> probability, HashMap<Class<? extends ISignalProcessor>, Float> processorProbabilityMap) {
        this.probability = probability;
        this.processorProbabilityMap = processorProbabilityMap;
    }

    abstract K getNeuronInstance(Long neuronId, Integer layerId);
}
