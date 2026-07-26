/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.clinical;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical.ITrendDetectorNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.VitalSignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor: pushes {@link VitalSignal} samples into an
 * {@link ITrendDetectorNeuron}'s rolling-window slope estimator.
 * Emits no follow-up signals — the trend is read on demand.
 */
public class VitalTrendProcessor implements ISignalProcessor<VitalSignal, ITrendDetectorNeuron> {

    private static final String DESCRIPTION = "Per-vital trend detector";

    @Override
    public <I extends ISignal> List<I> process(VitalSignal input, ITrendDetectorNeuron neuron) {
        if (input != null && neuron != null) neuron.observe(input);
        return new LinkedList<>();
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return VitalTrendProcessor.class; }
    @Override public Class<ITrendDetectorNeuron> getNeuronClass() { return ITrendDetectorNeuron.class; }
    @Override public Class<VitalSignal> getSignalClass() { return VitalSignal.class; }
}
