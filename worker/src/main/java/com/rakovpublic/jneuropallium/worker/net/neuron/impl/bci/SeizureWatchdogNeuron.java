/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.bci.LFPSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.bci.SeizureRiskSignal;

/**
 * Layer 5 seizure watchdog. Monitors LFP high-frequency oscillations,
 * rhythmic theta / spike-wave markers and broadband gamma surges; when the
 * aggregate pre-ictal risk crosses a threshold, it emits a
 * {@link SeizureRiskSignal} that forces a lockout of the stimulation safety
 * gate (Worrell et al. 2008).
 * Loop=1 / Epoch=1.
 */
public class SeizureWatchdogNeuron extends ModulatableNeuron implements ISeizureWatchdogNeuron {

    private double riskThreshold = 0.8;
    private long lockoutDurationTicks = 60_000;   // ≈ 60s @ 1 kHz
    private double lastRisk;
    private SeizureMarker lastMarker = SeizureMarker.NONE;

    public SeizureWatchdogNeuron() { super(); }
    public SeizureWatchdogNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    /**
     * Assess risk from one LFP window. Heuristic weights: high-gamma surge
     * dominates, theta and beta contribute, delta is protective.
     */
    public SeizureRiskSignal assess(LFPSignal lfp, int regionId) {
        if (lfp == null) return new SeizureRiskSignal(0, SeizureMarker.NONE, regionId);
        double[] p = lfp.getBandPowers();
        double highGamma = p[LFPSignal.HIGH_GAMMA];
        double lowGamma = p[LFPSignal.LOW_GAMMA];
        double theta = p[LFPSignal.THETA];
        double beta = p[LFPSignal.BETA];
        double delta = p[LFPSignal.DELTA];
        double total = highGamma + lowGamma + theta + beta + delta + 1e-9;
        double risk = (2.0 * highGamma + 1.0 * lowGamma + 0.6 * theta + 0.3 * beta - 0.2 * delta) / total;
        risk = Math.max(0, Math.min(1, risk));
        SeizureMarker marker = SeizureMarker.NONE;
        if (highGamma > 0.5 * total) marker = SeizureMarker.HIGH_FREQUENCY_OSCILLATION;
        else if (theta > 0.4 * total) marker = SeizureMarker.RHYTHMIC_THETA;
        else if (lowGamma > 0.4 * total) marker = SeizureMarker.GAMMA_SURGE;
        lastRisk = risk;
        lastMarker = marker;
        return new SeizureRiskSignal(risk, marker, regionId);
    }

    public boolean shouldTriggerLockout(double risk) { return risk >= riskThreshold; }
    public long getLockoutDurationTicks() { return lockoutDurationTicks; }
    public void setRiskThreshold(double t) { this.riskThreshold = Math.max(0, Math.min(1, t)); }
    public void setLockoutDurationTicks(long t) { this.lockoutDurationTicks = Math.max(0, t); }
    public double getLastRisk() { return lastRisk; }
    public SeizureMarker getLastMarker() { return lastMarker; }
}
