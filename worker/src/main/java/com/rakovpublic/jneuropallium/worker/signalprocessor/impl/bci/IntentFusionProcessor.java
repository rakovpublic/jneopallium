/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.bci;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci.IIntentFusionNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.bci.IntentSignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor: feeds one intent stream (spike-based or
 * LFP-based) into an {@link IIntentFusionNeuron}. When a complementary
 * stream is present, the neuron returns a fused {@link IntentSignal}
 * which this processor forwards.
 */
public class IntentFusionProcessor implements ISignalProcessor<IntentSignal, IIntentFusionNeuron> {

    private static final String DESCRIPTION = "Spike + LFP intent fusion";

    @Override
    @SuppressWarnings("unchecked")
    public <I extends ISignal> List<I> process(IntentSignal input, IIntentFusionNeuron neuron) {
        List<I> out = new LinkedList<>();
        if (input == null || neuron == null) return out;
        IntentSignal fused = neuron.observe(input);
        if (fused != null && fused != input) out.add((I) fused);
        return out;
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return IntentFusionProcessor.class; }
    @Override public Class<IIntentFusionNeuron> getNeuronClass() { return IIntentFusionNeuron.class; }
    @Override public Class<IntentSignal> getSignalClass() { return IntentSignal.class; }
}
