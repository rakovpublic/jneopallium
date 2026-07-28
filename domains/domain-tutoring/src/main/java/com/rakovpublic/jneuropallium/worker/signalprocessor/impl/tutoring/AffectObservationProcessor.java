/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.tutoring;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.tutoring.IFlowStateNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.tutoring.AffectObservationSignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor: feeds an {@link AffectObservationSignal} into an
 * {@link IFlowStateNeuron} so the flow-state classification reflects
 * the learner's current valence / arousal.
 */
public class AffectObservationProcessor implements ISignalProcessor<AffectObservationSignal, IFlowStateNeuron> {

    private static final String DESCRIPTION = "Updates flow-state with learner affect";

    @Override
    public <I extends ISignal> List<I> process(AffectObservationSignal input, IFlowStateNeuron neuron) {
        if (input != null && neuron != null) neuron.observeAffect(input);
        return new LinkedList<>();
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return AffectObservationProcessor.class; }
    @Override public Class<IFlowStateNeuron> getNeuronClass() { return IFlowStateNeuron.class; }
    @Override public Class<AffectObservationSignal> getSignalClass() { return AffectObservationSignal.class; }
}
