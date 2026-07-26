/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.security;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.IncidentReportSignal;

import java.util.HashMap;
import java.util.Map;

/**
 * Layer 7 homeostasis: tracks analyst acknowledgement rate of
 * {@link IncidentReportSignal} entries and exposes a multiplier that
 * should be applied to anomaly thresholds when false-positive rate is
 * high. Loop=2 / Epoch=2.
 */
public class AlertFatigueMonitorNeuron extends ModulatableNeuron implements IAlertFatigueMonitorNeuron {

    private final Map<String, Boolean> outcomes = new HashMap<>();
    private int reports;
    private int acknowledged;
    private int falsePositives;

    public AlertFatigueMonitorNeuron() { super(); }
    public AlertFatigueMonitorNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    @Override
    public void observe(IncidentReportSignal r) {
        if (r == null || r.getIncidentId() == null) return;
        outcomes.putIfAbsent(r.getIncidentId(), null);
        reports++;
    }

    @Override
    public void acknowledge(String incidentId, boolean trueAlert) {
        if (incidentId == null) return;
        if (!outcomes.containsKey(incidentId)) return;
        outcomes.put(incidentId, trueAlert);
        acknowledged++;
        if (!trueAlert) falsePositives++;
    }

    @Override
    public double falsePositiveRate() {
        if (acknowledged == 0) return 0.0;
        return (double) falsePositives / acknowledged;
    }

    @Override
    public double thresholdMultiplier() {
        // FP=0 → 1.0; FP=0.5 → 1.5; FP=1.0 → 2.0
        return 1.0 + falsePositiveRate();
    }

    @Override public int reports() { return reports; }
    @Override public int acknowledged() { return acknowledged; }
}
