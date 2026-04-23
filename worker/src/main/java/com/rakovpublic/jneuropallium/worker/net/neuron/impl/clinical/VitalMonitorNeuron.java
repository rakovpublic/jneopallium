/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.AdverseEventAlertSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.VitalSignal;

import java.util.EnumMap;
import java.util.Map;

/**
 * Layer 0 vital-sign monitor. Streams {@link VitalSignal}s into a rolling
 * per-channel store and emits {@link AdverseEventAlertSignal} when a sample
 * crosses a configured guardrail band. Biological analogue: brainstem
 * baroreceptor / chemoreceptor gating of autonomic reflexes.
 * Loop=1 / Epoch=1.
 */
public class VitalMonitorNeuron extends ModulatableNeuron implements IVitalMonitorNeuron {

    private final Map<VitalType, double[]> guardrails = new EnumMap<>(VitalType.class);
    private final Map<VitalType, Double> lastValue = new EnumMap<>(VitalType.class);

    public VitalMonitorNeuron() {
        super();
        defaults();
    }

    public VitalMonitorNeuron(Long neuronId, ISignalChain chain, Long run) {
        super(neuronId, chain, run);
        defaults();
    }

    private void defaults() {
        guardrails.put(VitalType.HR, new double[]{40, 150});
        guardrails.put(VitalType.SPO2, new double[]{88, 100});
        guardrails.put(VitalType.BP_SYS, new double[]{80, 200});
        guardrails.put(VitalType.BP_DIA, new double[]{40, 120});
        guardrails.put(VitalType.TEMP, new double[]{35.0, 39.5});
        guardrails.put(VitalType.RESP, new double[]{8, 30});
    }

    public void setGuardrail(VitalType t, double min, double max) {
        if (t == null) return;
        guardrails.put(t, new double[]{Math.min(min, max), Math.max(min, max)});
    }

    /**
     * Ingest one vital sample. If the value is outside the guardrail band,
     * return a non-null {@link AdverseEventAlertSignal}.
     */
    public AdverseEventAlertSignal observe(VitalSignal v) {
        if (v == null) return null;
        lastValue.put(v.getType(), v.getMeasurement());
        double[] band = guardrails.get(v.getType());
        if (band == null) return null;
        if (v.getMeasurement() < band[0] || v.getMeasurement() > band[1]) {
            AlertSeverity sev = severityFor(v, band);
            AdverseEventAlertSignal a = new AdverseEventAlertSignal(sev, v.getType().name() + "_OUT_OF_RANGE", v.getPatientId());
            a.setDetail("value=" + v.getMeasurement() + " band=[" + band[0] + "," + band[1] + "]");
            return a;
        }
        return null;
    }

    private AlertSeverity severityFor(VitalSignal v, double[] band) {
        double mag = v.getMeasurement() < band[0]
                ? (band[0] - v.getMeasurement()) / Math.max(1e-6, band[0])
                : (v.getMeasurement() - band[1]) / Math.max(1e-6, band[1]);
        if (mag >= 0.40) return AlertSeverity.CRITICAL;
        if (mag >= 0.20) return AlertSeverity.URGENT;
        if (mag >= 0.05) return AlertSeverity.WARNING;
        return AlertSeverity.INFO;
    }

    public Double lastValue(VitalType t) { return lastValue.get(t); }
    public double[] getGuardrail(VitalType t) {
        double[] g = guardrails.get(t);
        return g == null ? null : g.clone();
    }
}
