/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing;

import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.storage.IInitInput;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class CycleInputUpdateProcessor implements ISignalProcessor<CycleInputUpdateSignal, CycleNeuron> {
    private final static String DESCRIPTION = "Processor which changes processing frequency for specific input source ";

    @Override
    public <I extends ISignal> List<I> process(CycleInputUpdateSignal input, CycleNeuron neuron) {
        HashMap<IInitInput, ProcessingFrequency> result = neuron.getInputProcessingFrequencyHashMap();
        for (IInitInput initInput : result.keySet()) {
            if (initInput.getName().equals(input.value.getName())) {
                result.put(initInput, input.value.getFrequency());
            }
        }
        neuron.setInputProcessingFrequencyHashMap(result);
        return new LinkedList<>();
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    public Boolean hasMerger() {
        return false;
    }

    @Override
    public Class<? extends ISignalProcessor> getSignalProcessorClass() {
        return CycleInputUpdateProcessor.class;
    }

    @Override
    public Class<CycleNeuron> getNeuronClass() {
        return CycleNeuron.class;
    }
}
