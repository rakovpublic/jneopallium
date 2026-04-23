/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.tutoring;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.tutoring.IForgettingCurveNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.tutoring.ReviewScheduleSignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor: records an external review schedule on an
 * {@link IForgettingCurveNeuron} so both agents agree on target
 * retention.
 */
public class ReviewScheduleProcessor implements ISignalProcessor<ReviewScheduleSignal, IForgettingCurveNeuron> {

    private static final String DESCRIPTION = "Aligns forgetting-curve with external review schedule";

    @Override
    public <I extends ISignal> List<I> process(ReviewScheduleSignal input, IForgettingCurveNeuron neuron) {
        if (input != null && neuron != null) neuron.observe(input);
        return new LinkedList<>();
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return ReviewScheduleProcessor.class; }
    @Override public Class<IForgettingCurveNeuron> getNeuronClass() { return IForgettingCurveNeuron.class; }
    @Override public Class<ReviewScheduleSignal> getSignalClass() { return ReviewScheduleSignal.class; }
}
