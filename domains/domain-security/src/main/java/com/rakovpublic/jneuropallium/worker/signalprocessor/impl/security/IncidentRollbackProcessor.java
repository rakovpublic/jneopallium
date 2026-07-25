/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.security;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.security.IRollbackNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.IncidentReportSignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor: offers each {@link IncidentReportSignal} to the
 * rollback neuron which performs snapshot restore only on HIGH /
 * CRITICAL severities when enabled.
 */
public class IncidentRollbackProcessor implements ISignalProcessor<IncidentReportSignal, IRollbackNeuron> {

    private static final String DESCRIPTION = "Incident-driven snapshot rollback (opt-in)";

    @Override
    public <I extends ISignal> List<I> process(IncidentReportSignal input, IRollbackNeuron neuron) {
        if (input != null && neuron != null) neuron.maybeRollback(input);
        return new LinkedList<>();
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return IncidentRollbackProcessor.class; }
    @Override public Class<IRollbackNeuron> getNeuronClass() { return IRollbackNeuron.class; }
    @Override public Class<IncidentReportSignal> getSignalClass() { return IncidentReportSignal.class; }
}
