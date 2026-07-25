/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.VitalSignal;

import java.util.EnumMap;
import java.util.Map;

/**
 * Layer 2 acuity neuron. Computes a simplified NEWS2-style early-warning
 * score from the most recent vital samples and exposes it as a
 * normalised acuity score in [0,1]. High acuity tightens harm thresholds
 * and raises attention salience of abnormal findings. Loop=2 / Epoch=2.
 */
public class AcuityNeuron extends ModulatableNeuron implements IAcuityNeuron {

    private final Map<VitalType, Double> latest = new EnumMap<>(VitalType.class);
    private double acuityScore;
    private int rawScore;

    public AcuityNeuron() { super(); }

    public AcuityNeuron(Long neuronId, ISignalChain chain, Long run) {
        super(neuronId, chain, run);
    }

    public void ingest(VitalSignal v) {
        if (v == null || v.getType() == null) return;
        latest.put(v.getType(), v.getMeasurement());
        recompute();
    }

    private void recompute() {
        int score = 0;
        Double hr = latest.get(VitalType.HR);
        if (hr != null) {
            if (hr < 40) score += 3;
            else if (hr < 50) score += 1;
            else if (hr > 130) score += 3;
            else if (hr > 110) score += 2;
            else if (hr > 90) score += 1;
        }
        Double spo2 = latest.get(VitalType.SPO2);
        if (spo2 != null) {
            if (spo2 < 88) score += 3;
            else if (spo2 < 92) score += 2;
            else if (spo2 < 94) score += 1;
        }
        Double sys = latest.get(VitalType.BP_SYS);
        if (sys != null) {
            if (sys < 90) score += 3;
            else if (sys < 100) score += 2;
            else if (sys < 110) score += 1;
            else if (sys > 220) score += 3;
        }
        Double temp = latest.get(VitalType.TEMP);
        if (temp != null) {
            if (temp < 35.0) score += 3;
            else if (temp < 36.0) score += 1;
            else if (temp > 39.0) score += 2;
            else if (temp > 38.0) score += 1;
        }
        Double resp = latest.get(VitalType.RESP);
        if (resp != null) {
            if (resp < 8) score += 3;
            else if (resp > 24) score += 3;
            else if (resp > 20) score += 2;
            else if (resp < 12) score += 1;
        }
        this.rawScore = score;
        // Clinically: 7+ is "urgent emergency response" — normalise to 1.0.
        this.acuityScore = Math.max(0.0, Math.min(1.0, score / 7.0));
    }

    public double score() { return acuityScore; }
    public int rawScore() { return rawScore; }
    public double harmThresholdMultiplier() {
        // Higher acuity → tighter harm threshold band (multiplier in [1,3]).
        return 1.0 + 2.0 * acuityScore;
    }
    public void reset() { latest.clear(); acuityScore = 0; rawScore = 0; }
}
