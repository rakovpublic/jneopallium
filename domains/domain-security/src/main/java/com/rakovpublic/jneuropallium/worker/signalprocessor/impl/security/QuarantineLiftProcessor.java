/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.security;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.security.IQuarantineEntityNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.QuarantineLiftSignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor: an externally-issued {@link QuarantineLiftSignal}
 * re-confirms the automatic-lift of the named entity by driving a
 * {@code tick} on the {@link IQuarantineEntityNeuron} at the signal's
 * epoch. Any internally-emitted lift signals that fall due are
 * forwarded.
 */
public class QuarantineLiftProcessor implements ISignalProcessor<QuarantineLiftSignal, IQuarantineEntityNeuron> {

    private static final String DESCRIPTION = "Quarantine automatic-lift reaper";

    @Override
    @SuppressWarnings("unchecked")
    public <I extends ISignal> List<I> process(QuarantineLiftSignal input, IQuarantineEntityNeuron neuron) {
        List<I> out = new LinkedList<>();
        if (input == null || neuron == null) return out;
        List<QuarantineLiftSignal> lifts = neuron.tick(input.getEpoch());
        if (lifts != null) {
            for (QuarantineLiftSignal l : lifts) if (l != null) out.add((I) l);
        }
        return out;
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return QuarantineLiftProcessor.class; }
    @Override public Class<IQuarantineEntityNeuron> getNeuronClass() { return IQuarantineEntityNeuron.class; }
    @Override public Class<QuarantineLiftSignal> getSignalClass() { return QuarantineLiftSignal.class; }
}
