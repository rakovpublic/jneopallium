/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.security;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.security.IResponseGateNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.QuarantineRequestSignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor: every proposed {@link QuarantineRequestSignal}
 * passes through the {@link IResponseGateNeuron} which enforces hard
 * allow-lists, critical-asset protection, and the configured response
 * mode. Requests that survive are forwarded for actuation.
 */
public class QuarantineGateProcessor implements ISignalProcessor<QuarantineRequestSignal, IResponseGateNeuron> {

    private static final String DESCRIPTION = "Hard self-tolerance gate for proposed quarantines";

    @Override
    @SuppressWarnings("unchecked")
    public <I extends ISignal> List<I> process(QuarantineRequestSignal input, IResponseGateNeuron neuron) {
        List<I> out = new LinkedList<>();
        if (input == null || neuron == null) return out;
        QuarantineRequestSignal kept = neuron.gate(input, 1.0);
        if (kept != null) out.add((I) kept);
        return out;
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return QuarantineGateProcessor.class; }
    @Override public Class<IResponseGateNeuron> getNeuronClass() { return IResponseGateNeuron.class; }
    @Override public Class<QuarantineRequestSignal> getSignalClass() { return QuarantineRequestSignal.class; }
}
