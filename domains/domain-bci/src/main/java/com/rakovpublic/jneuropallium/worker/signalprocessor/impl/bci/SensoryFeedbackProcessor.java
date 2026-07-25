/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.bci;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci.IActuatorNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.bci.SensoryFeedbackSignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor: forwards a {@link SensoryFeedbackSignal} to an
 * {@link IActuatorNeuron}'s audit channel. The feedback cue itself is
 * re-emitted downstream for user-facing consumers.
 */
public class SensoryFeedbackProcessor implements ISignalProcessor<SensoryFeedbackSignal, IActuatorNeuron> {

    private static final String DESCRIPTION = "Records sensory-feedback cues for transparency";

    @Override
    @SuppressWarnings("unchecked")
    public <I extends ISignal> List<I> process(SensoryFeedbackSignal input, IActuatorNeuron neuron) {
        List<I> out = new LinkedList<>();
        if (input == null || neuron == null) return out;
        neuron.observeFeedback(input);
        out.add((I) input);
        return out;
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return SensoryFeedbackProcessor.class; }
    @Override public Class<IActuatorNeuron> getNeuronClass() { return IActuatorNeuron.class; }
    @Override public Class<SensoryFeedbackSignal> getSignalClass() { return SensoryFeedbackSignal.class; }
}
