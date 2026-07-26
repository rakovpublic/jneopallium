/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.tutoring;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.tutoring.IWellbeingGuardNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.tutoring.InterventionSignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor: forwards an {@link InterventionSignal} to an
 * {@link IWellbeingGuardNeuron} for audit / bookkeeping.
 */
public class InterventionObservationProcessor implements ISignalProcessor<InterventionSignal, IWellbeingGuardNeuron> {

    private static final String DESCRIPTION = "Records intervention events on the wellbeing guard";

    @Override
    public <I extends ISignal> List<I> process(InterventionSignal input, IWellbeingGuardNeuron neuron) {
        if (input != null && neuron != null) neuron.observe(input);
        return new LinkedList<>();
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return InterventionObservationProcessor.class; }
    @Override public Class<IWellbeingGuardNeuron> getNeuronClass() { return IWellbeingGuardNeuron.class; }
    @Override public Class<InterventionSignal> getSignalClass() { return InterventionSignal.class; }
}
