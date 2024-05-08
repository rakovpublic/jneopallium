/*
 * Copyright (c) 2024. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.test.definitions.structurallogic;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.test.definitions.functionallogic.NeuronA;
import com.rakovpublic.jneuropallium.worker.util.NeuronStatisticalProperties;

import java.util.HashMap;

public class NeuronAStatisticalProperties extends NeuronStatisticalProperties<NeuronA> {

    public NeuronAStatisticalProperties(HashMap<Integer, Float> probability, HashMap<Class<? extends ISignalProcessor>, Float> processorProbabilityMap) {
        super(probability, processorProbabilityMap);
    }

    public NeuronA getNeuronInstance(Long neuronId, Integer layerId){
        return null;
    }
}
