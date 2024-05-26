/*
 * Copyright (c) 2024. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.test.definitions.structurallogic;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.test.definitions.functionallogic.NeuronC;
import com.rakovpublic.jneuropallium.worker.util.NeuronStatisticalProperties;

import java.util.HashMap;

public class NeuronCStatisticalProperties extends NeuronStatisticalProperties<NeuronC> {

    public NeuronCStatisticalProperties(HashMap<Integer, Float> probability, HashMap<Class<? extends ISignalProcessor>, Float> processorProbabilityMap) {
        super(probability, processorProbabilityMap);
    }

    @Override
    public NeuronC getNeuronInstance(Long neuronId, Integer layerId) {
        return new NeuronC();
    }
}
