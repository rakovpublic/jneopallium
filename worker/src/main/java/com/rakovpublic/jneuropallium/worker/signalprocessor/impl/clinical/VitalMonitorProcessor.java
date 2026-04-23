/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.clinical;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical.IVitalMonitorNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.AdverseEventAlertSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.VitalSignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor: feeds {@link VitalSignal} into an
 * {@link IVitalMonitorNeuron} and forwards any guardrail-excursion alert.
 */
public class VitalMonitorProcessor implements ISignalProcessor<VitalSignal, IVitalMonitorNeuron> {

    private static final String DESCRIPTION = "Vital-sign guardrail monitor";

    @Override
    @SuppressWarnings("unchecked")
    public <I extends ISignal> List<I> process(VitalSignal input, IVitalMonitorNeuron neuron) {
        List<I> out = new LinkedList<>();
        if (input == null || neuron == null) return out;
        AdverseEventAlertSignal alert = neuron.observe(input);
        if (alert != null) out.add((I) alert);
        return out;
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return VitalMonitorProcessor.class; }
    @Override public Class<IVitalMonitorNeuron> getNeuronClass() { return IVitalMonitorNeuron.class; }
    @Override public Class<VitalSignal> getSignalClass() { return VitalSignal.class; }
}
