/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.curiosity;

import com.rakovpublic.jneuropallium.ai.signals.fast.AttentionGateSignal;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.curiosity.IBoredomNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.curiosity.BoredomSignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor: records a visit to a context (via
 * {@link BoredomSignal#getContextHash()}) into an {@link IBoredomNeuron}
 * and, if familiarity has saturated, forwards the resulting
 * {@link AttentionGateSignal} to suppress over-familiar inputs.
 */
public class BoredomProcessor implements ISignalProcessor<BoredomSignal, IBoredomNeuron> {

    private static final String DESCRIPTION = "Over-familiarity suppression via context visits";

    @Override
    @SuppressWarnings("unchecked")
    public <I extends ISignal> List<I> process(BoredomSignal input, IBoredomNeuron neuron) {
        List<I> out = new LinkedList<>();
        if (input == null || neuron == null) return out;
        neuron.visit(input.getContextHash());
        AttentionGateSignal gate = neuron.maybeSuppress(input.getContextHash());
        if (gate != null) out.add((I) gate);
        return out;
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return BoredomProcessor.class; }
    @Override public Class<IBoredomNeuron> getNeuronClass() { return IBoredomNeuron.class; }
    @Override public Class<BoredomSignal> getSignalClass() { return BoredomSignal.class; }
}
