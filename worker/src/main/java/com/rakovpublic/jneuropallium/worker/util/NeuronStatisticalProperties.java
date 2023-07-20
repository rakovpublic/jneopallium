package com.rakovpublic.jneuropallium.worker.util;

import com.rakovpublic.jneuropallium.worker.neuron.INeuron;
import com.rakovpublic.jneuropallium.worker.neuron.ISignalProcessor;

import java.util.HashMap;

public abstract class NeuronStatisticalProperties<K extends INeuron> {
    public Float getProbability() {
        return probability;
    }

    private Float probability;
    private HashMap<Class<? extends ISignalProcessor>, Float> processorProbabilityMap;
    public NeuronStatisticalProperties(Float probability, HashMap<Class<? extends ISignalProcessor>, Float> processorProbabilityMap){
        this.probability = probability;
        this.processorProbabilityMap = processorProbabilityMap;
    }
    abstract K getNeuronInstance(Long neuronId, Integer layerId);
}