/*
 * Copyright (c) 2024. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.test.definitions.structurallogic;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.test.definitions.functionallogic.NeuronB;
import com.rakovpublic.jneuropallium.worker.util.NeuronStatisticalProperties;

import java.util.HashMap;

public class NeuronBStatisticalProperties extends NeuronStatisticalProperties<NeuronB> {

    public NeuronBStatisticalProperties(HashMap<Integer, Float> probability, HashMap<Class<? extends ISignalProcessor>, Float> processorProbabilityMap) {
        super(probability, processorProbabilityMap);
    }

    @Override
    public NeuronB getNeuronInstance(Long neuronId, Integer layerId) {
        return null;
    }
}
