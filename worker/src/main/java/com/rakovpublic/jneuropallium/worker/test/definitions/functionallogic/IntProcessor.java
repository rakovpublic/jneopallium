/*
 * Copyright (c) 2024. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.test.definitions.functionallogic;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.LinkedList;
import java.util.List;

public class IntProcessor implements ISignalProcessor<IntSignal, NeuronIntField> {
    public String description = "processor for IntSignal and  NeuronIntField ";
    public Class<? extends ISignalProcessor> signalProcessorClass = IntProcessor.class;
    public Class<NeuronIntField> neuronClass =  NeuronIntField.class;
    public Class<IntSignal> signalClass =  IntSignal.class;
    @Override
    public <I extends ISignal> List<I> process(IntSignal input, NeuronIntField neuron) {
        neuron.setIntField(neuron.getIntField() + input.getValue());
        return new LinkedList<>();
    }

    @Override
    public String getDescription() {
        return description;
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

    @Override
    public Class<IntSignal> getSignalClass() {
        return IntSignal.class;
    }
}
