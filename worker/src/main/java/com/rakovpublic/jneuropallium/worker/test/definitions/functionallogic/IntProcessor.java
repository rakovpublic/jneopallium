/*
 * Copyright (c) 2024. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.test.definitions.functionallogic;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.LinkedList;
import java.util.List;

public class IntProcessor implements ISignalProcessor<IntSignal, NeuronIntField> {
    private static String DESCRIPTION = "processor for IntSignal and  NeuronIntField ";

    @Override
    public <I extends ISignal> List<I> process(IntSignal input, NeuronIntField neuron) {
        neuron.setIntField(neuron.getIntField() + input.getValue());
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
        return IntProcessor.class;
    }

    @Override
    public Class<NeuronIntField> getNeuronClass() {
        return NeuronIntField.class;
    }
}
