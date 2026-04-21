/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.bci.DriftEstimateSignal;

import java.util.HashMap;
import java.util.Map;

/**
 * Layer 3 electrode-drift tracker. Estimates per-channel waveform drift and
 * rolling SNR; flags channels whose drift exceeds a tolerance for
 * recalibration. Biological analogue: micromotion / glial encapsulation
 * causing gradual signal loss over days/weeks (Perge et al. 2013).
 * Loop=2 / Epoch=5.
 */
public class DriftTrackerNeuron extends ModulatableNeuron implements IDriftTrackerNeuron {

    private final Map<Integer, Double> drift = new HashMap<>();
    private final Map<Integer, Double> snr = new HashMap<>();
    private double driftTolerance = 0.25;

    public DriftTrackerNeuron() { super(); }
    public DriftTrackerNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    /**
     * Update rolling drift estimate using a shift score in [0,1] (e.g. 1 minus
     * cosine similarity of the new waveform template vs. the reference).
     */
    public DriftEstimateSignal observe(int channelId, double shiftScore, double snrLinear) {
        double prev = drift.getOrDefault(channelId, 0.0);
        double updated = 0.9 * prev + 0.1 * shiftScore;
        drift.put(channelId, updated);
        snr.put(channelId, snrLinear);
        return new DriftEstimateSignal(channelId, updated, snrLinear);
    }

    public boolean needsRecalibration(int channelId) {
        return drift.getOrDefault(channelId, 0.0) >= driftTolerance;
    }

    public double driftFor(int channelId) { return drift.getOrDefault(channelId, 0.0); }
    public double snrFor(int channelId) { return snr.getOrDefault(channelId, 0.0); }
    public void setDriftTolerance(double t) { this.driftTolerance = Math.max(0, t); }
}
