/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.clinical;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical.IAcuityNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.VitalSignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor: ingests {@link VitalSignal} samples into the
 * NEWS2-style {@link IAcuityNeuron}. The acuity score is read on
 * demand by downstream attention / harm-threshold modulators.
 */
public class VitalAcuityProcessor implements ISignalProcessor<VitalSignal, IAcuityNeuron> {

    private static final String DESCRIPTION = "Per-tick acuity score update";

    @Override
    public <I extends ISignal> List<I> process(VitalSignal input, IAcuityNeuron neuron) {
        if (input != null && neuron != null) neuron.ingest(input);
        return new LinkedList<>();
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return VitalAcuityProcessor.class; }
    @Override public Class<IAcuityNeuron> getNeuronClass() { return IAcuityNeuron.class; }
    @Override public Class<VitalSignal> getSignalClass() { return VitalSignal.class; }
}
