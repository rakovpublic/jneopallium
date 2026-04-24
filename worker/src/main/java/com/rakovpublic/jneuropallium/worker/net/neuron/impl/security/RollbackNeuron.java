/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.security;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.IncidentReportSignal;

/**
 * Layer 5 rollback coordinator. Gated off by default per spec §7
 * ({@code rollback-enabled: false}); a deployment must opt in per-site.
 * Only acts on HIGH or CRITICAL incidents. Loop=2 / Epoch=1.
 */
public class RollbackNeuron extends ModulatableNeuron implements IRollbackNeuron {

    private boolean enabled;
    private int attempted;
    private int succeeded;

    public RollbackNeuron() { super(); }
    public RollbackNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    @Override public boolean isEnabled() { return enabled; }
    @Override public void setEnabled(boolean v) { this.enabled = v; }

    @Override
    public boolean maybeRollback(IncidentReportSignal incident) {
        if (!enabled || incident == null) return false;
        Severity sev = incident.getSeverity();
        if (sev != Severity.HIGH && sev != Severity.CRITICAL) return false;
        attempted++;
        succeeded++; // stub: in production, fan out to snapshot subsystem
        return true;
    }

    @Override public int attempted() { return attempted; }
    @Override public int succeeded() { return succeeded; }
}
