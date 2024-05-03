/*
 * Copyright (c) 2024. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.test.definitions.functionallogic;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.LinkedList;
import java.util.List;

public class DoubleProcessor implements ISignalProcessor<DoubleSignal, NeuronWithDoubleField> {
    private static String DESCRIPTION = "processor for DoubleSignal and  NeuronWithDoubleField ";

    @Override
    public <I extends ISignal> List<I> process(DoubleSignal input, NeuronWithDoubleField neuron) {
        neuron.setDoubleField(neuron.getDoubleField() + input.getValue());
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
        return DoubleProcessor.class;
    }

    @Override
    public Class<NeuronWithDoubleField> getNeuronClass() {
        return NeuronWithDoubleField.class;
    }
}
