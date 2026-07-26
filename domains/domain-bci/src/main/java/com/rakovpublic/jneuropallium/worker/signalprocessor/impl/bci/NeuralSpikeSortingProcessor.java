/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.bci;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci.ISpikeSortingNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.bci.NeuralSpikeSignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor: assigns the unit id for an incoming
 * {@link NeuralSpikeSignal} via an {@link ISpikeSortingNeuron}'s
 * template-matching sorter. Emits no follow-up signals; the resulting
 * unit id is carried in the signal itself for downstream processors.
 */
public class NeuralSpikeSortingProcessor implements ISignalProcessor<NeuralSpikeSignal, ISpikeSortingNeuron> {

    private static final String DESCRIPTION = "Online spike sorting (unit assignment)";

    @Override
    public <I extends ISignal> List<I> process(NeuralSpikeSignal input, ISpikeSortingNeuron neuron) {
        if (input != null && neuron != null) neuron.sort(input);
        return new LinkedList<>();
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return NeuralSpikeSortingProcessor.class; }
    @Override public Class<ISpikeSortingNeuron> getNeuronClass() { return ISpikeSortingNeuron.class; }
    @Override public Class<NeuralSpikeSignal> getSignalClass() { return NeuralSpikeSignal.class; }
}
