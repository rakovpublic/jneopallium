/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.ssmaint;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.ssmaint.ISsAdvisoryGateNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.ssmaint.ThresholdUpdateSignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Applies a feedback-derived threshold update to the running advisory gate. This
 * is the wiring that lets the deployed model keep learning in place: it mutates
 * gate state and emits nothing further.
 */
public class ThresholdUpdateProcessor
        implements ISignalProcessor<ThresholdUpdateSignal, ISsAdvisoryGateNeuron> {

    private static final String DESCRIPTION = "Live threshold update into gate";

    @Override
    public <I extends ISignal> List<I> process(ThresholdUpdateSignal input, ISsAdvisoryGateNeuron neuron) {
        if (input != null && neuron != null) {
            neuron.onThresholdUpdate(input);
        }
        return new LinkedList<>();
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return ThresholdUpdateProcessor.class; }
    @Override public Class<ISsAdvisoryGateNeuron> getNeuronClass() { return ISsAdvisoryGateNeuron.class; }
    @Override public Class<ThresholdUpdateSignal> getSignalClass() { return ThresholdUpdateSignal.class; }
}
