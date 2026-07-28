/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.clinical;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical.IPatientContextNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.DemographicSignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor: refreshes the per-patient context snapshot
 * (allergies, comorbidities, vulnerability factor) on every
 * {@link DemographicSignal}.
 */
public class DemographicContextProcessor implements ISignalProcessor<DemographicSignal, IPatientContextNeuron> {

    private static final String DESCRIPTION = "Patient demographic / context snapshot";

    @Override
    public <I extends ISignal> List<I> process(DemographicSignal input, IPatientContextNeuron neuron) {
        if (input != null && neuron != null) neuron.update(input);
        return new LinkedList<>();
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return DemographicContextProcessor.class; }
    @Override public Class<IPatientContextNeuron> getNeuronClass() { return IPatientContextNeuron.class; }
    @Override public Class<DemographicSignal> getSignalClass() { return DemographicSignal.class; }
}
