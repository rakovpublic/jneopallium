/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.curiosity;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.curiosity.LearningProgressNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.curiosity.LearningProgressSignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor that records an incoming error sample into the
 * learning-progress neuron's per-domain window.
 */
public class LearningProgressProcessor implements ISignalProcessor<LearningProgressSignal, LearningProgressNeuron> {

    private static final String DESCRIPTION = "Oudeyer-Kaplan learning-progress reward update";

    @Override
    @SuppressWarnings("unchecked")
    public <I extends ISignal> List<I> process(LearningProgressSignal input, LearningProgressNeuron neuron) {
        List<I> out = new LinkedList<>();
        if (input == null || neuron == null) return out;
        LearningProgressSignal emitted = neuron.recordError(input.getDomain(), input.getErrorDerivative());
        if (emitted != null) out.add((I) emitted);
        return out;
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return LearningProgressProcessor.class; }
    @Override public Class<LearningProgressNeuron> getNeuronClass() { return LearningProgressNeuron.class; }
    @Override public Class<LearningProgressSignal> getSignalClass() { return LearningProgressSignal.class; }
}
