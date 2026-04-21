/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.curiosity;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.curiosity.INoveltyDetectorNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.curiosity.NoveltySignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor that passes a {@link NoveltySignal}'s context hash
 * through the detector; the detector's mutable state records the visit.
 */
public class NoveltyDetectionProcessor implements ISignalProcessor<NoveltySignal, INoveltyDetectorNeuron> {

    private static final String DESCRIPTION = "Hippocampal-CA1-like novelty-detection update";

    @Override
    @SuppressWarnings("unchecked")
    public <I extends ISignal> List<I> process(NoveltySignal input, INoveltyDetectorNeuron neuron) {
        List<I> out = new LinkedList<>();
        if (input == null || neuron == null) return out;
        NoveltySignal emitted = neuron.evaluate(input.getContextHash());
        if (emitted != null) out.add((I) emitted);
        return out;
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return NoveltyDetectionProcessor.class; }
    @Override public Class<INoveltyDetectorNeuron> getNeuronClass() { return INoveltyDetectorNeuron.class; }
    @Override public Class<NoveltySignal> getSignalClass() { return NoveltySignal.class; }
}
