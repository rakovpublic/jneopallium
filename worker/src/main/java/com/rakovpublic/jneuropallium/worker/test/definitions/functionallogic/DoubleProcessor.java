/*
 * Copyright (c) 2024. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.test.definitions.functionallogic;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.LinkedList;
import java.util.List;

public class DoubleProcessor implements ISignalProcessor<DoubleSignal, NeuronWithDoubleField> {
    public String description = "processor for DoubleSignal and  NeuronWithDoubleField ";
    public Class<? extends ISignalProcessor> signalProcessorClass = DoubleProcessor.class;
    public Class<NeuronWithDoubleField> neuronClass =  NeuronWithDoubleField.class;
    public Class<DoubleSignal> signalClass =  DoubleSignal.class;


    @Override
    public <I extends ISignal> List<I> process(DoubleSignal input, NeuronWithDoubleField neuron) {
        neuron.setDoubleField(neuron.getDoubleField() + input.getValue());
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
        return DoubleProcessor.class;
    }

    @Override
    public Class<NeuronWithDoubleField> getNeuronClass() {
        return NeuronWithDoubleField.class;
    }

    @Override
    public Class<DoubleSignal> getSignalClass() {
        return DoubleSignal.class;
    }
}
