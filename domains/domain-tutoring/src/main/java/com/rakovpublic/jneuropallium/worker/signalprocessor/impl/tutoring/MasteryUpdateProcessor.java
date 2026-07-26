/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.tutoring;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.tutoring.IPrerequisiteGraphNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.tutoring.MasteryUpdateSignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor: ensures the prerequisite graph knows about
 * every concept for which mastery is reported.
 */
public class MasteryUpdateProcessor implements ISignalProcessor<MasteryUpdateSignal, IPrerequisiteGraphNeuron> {

    private static final String DESCRIPTION = "Keeps prerequisite graph in sync with mastery reports";

    @Override
    public <I extends ISignal> List<I> process(MasteryUpdateSignal input, IPrerequisiteGraphNeuron neuron) {
        if (input != null && neuron != null) neuron.observe(input);
        return new LinkedList<>();
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return MasteryUpdateProcessor.class; }
    @Override public Class<IPrerequisiteGraphNeuron> getNeuronClass() { return IPrerequisiteGraphNeuron.class; }
    @Override public Class<MasteryUpdateSignal> getSignalClass() { return MasteryUpdateSignal.class; }
}
