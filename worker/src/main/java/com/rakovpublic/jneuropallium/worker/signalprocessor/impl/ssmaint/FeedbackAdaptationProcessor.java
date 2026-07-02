/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.ssmaint;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.ssmaint.IFeedbackAdaptationNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.ssmaint.OperatorFeedbackSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.ssmaint.ThresholdUpdateSignal;

import java.util.LinkedList;
import java.util.List;

/** Turns operator feedback into a bounded threshold update (or nothing). */
public class FeedbackAdaptationProcessor
        implements ISignalProcessor<OperatorFeedbackSignal, IFeedbackAdaptationNeuron> {

    private static final String DESCRIPTION = "Feedback-driven threshold adaptation";

    @Override
    @SuppressWarnings("unchecked")
    public <I extends ISignal> List<I> process(OperatorFeedbackSignal input, IFeedbackAdaptationNeuron neuron) {
        List<I> out = new LinkedList<>();
        if (input == null || neuron == null) return out;
        ThresholdUpdateSignal update = neuron.onFeedback(input);
        if (update != null) out.add((I) update);
        return out;
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return FeedbackAdaptationProcessor.class; }
    @Override public Class<IFeedbackAdaptationNeuron> getNeuronClass() { return IFeedbackAdaptationNeuron.class; }
    @Override public Class<OperatorFeedbackSignal> getSignalClass() { return OperatorFeedbackSignal.class; }
}
