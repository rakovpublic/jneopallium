/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.security;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.security.IAlertFatigueMonitorNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.IncidentReportSignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor: tracks every {@link IncidentReportSignal} in the
 * fatigue monitor so its false-positive rate and threshold multiplier
 * stay current.
 */
public class IncidentFatigueProcessor implements ISignalProcessor<IncidentReportSignal, IAlertFatigueMonitorNeuron> {

    private static final String DESCRIPTION = "Alert-fatigue bookkeeping";

    @Override
    public <I extends ISignal> List<I> process(IncidentReportSignal input, IAlertFatigueMonitorNeuron neuron) {
        if (input != null && neuron != null) neuron.observe(input);
        return new LinkedList<>();
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return IncidentFatigueProcessor.class; }
    @Override public Class<IAlertFatigueMonitorNeuron> getNeuronClass() { return IAlertFatigueMonitorNeuron.class; }
    @Override public Class<IncidentReportSignal> getSignalClass() { return IncidentReportSignal.class; }
}
