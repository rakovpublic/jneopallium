/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.tutoring;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.tutoring.IHintGenerationNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.tutoring.HintSignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor: records an externally-issued {@link HintSignal}
 * on an {@link IHintGenerationNeuron} so its escalation counter stays
 * consistent.
 */
public class HintObservationProcessor implements ISignalProcessor<HintSignal, IHintGenerationNeuron> {

    private static final String DESCRIPTION = "Records externally-issued hints for escalation counting";

    @Override
    public <I extends ISignal> List<I> process(HintSignal input, IHintGenerationNeuron neuron) {
        if (input != null && neuron != null) neuron.observe(input);
        return new LinkedList<>();
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return HintObservationProcessor.class; }
    @Override public Class<IHintGenerationNeuron> getNeuronClass() { return IHintGenerationNeuron.class; }
    @Override public Class<HintSignal> getSignalClass() { return HintSignal.class; }
}
