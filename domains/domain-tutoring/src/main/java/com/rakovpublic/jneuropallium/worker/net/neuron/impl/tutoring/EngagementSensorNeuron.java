/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.tutoring;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.tutoring.EngagementSignal;

/**
 * Layer 0 multi-modal engagement sensor. Fuses click-rate, dwell-time, and
 * optional camera/mic engagement scores into a single attention estimate.
 * Biological analogue: pulvinar / brainstem arousal gating.
 * Loop=1 / Epoch=2.
 */
public class EngagementSensorNeuron extends ModulatableNeuron implements IEngagementSensorNeuron {

    private double clickRateScore;
    private double dwellTimeScore;
    private double cameraScore;
    private double micScore;
    private double fusedAttention;

    public EngagementSensorNeuron() { super(); }
    public EngagementSensorNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    /**
     * Fuse available channels. Unavailable channels are passed as NaN.
     */
    public EngagementSignal fuse(double clickRate, double dwellTime,
                                 double camera, double mic) {
        this.clickRateScore = clamp(clickRate);
        this.dwellTimeScore = clamp(dwellTime);
        this.cameraScore = clamp(camera);
        this.micScore = clamp(mic);

        double sum = 0.0;
        int n = 0;
        if (!Double.isNaN(clickRate)) { sum += clamp(clickRate); n++; }
        if (!Double.isNaN(dwellTime)) { sum += clamp(dwellTime); n++; }
        if (!Double.isNaN(camera)) { sum += clamp(camera); n++; }
        if (!Double.isNaN(mic)) { sum += clamp(mic); n++; }
        fusedAttention = n == 0 ? 0.0 : sum / n;

        EngagementSource source = n > 1 ? EngagementSource.MULTI_MODAL
                : (!Double.isNaN(camera) ? EngagementSource.CAMERA
                : (!Double.isNaN(mic) ? EngagementSource.MIC
                : (!Double.isNaN(dwellTime) ? EngagementSource.DWELL_TIME
                : EngagementSource.CLICK_RATE)));

        EngagementSignal s = new EngagementSignal(fusedAttention, source);
        s.setSourceNeuronId(this.getId());
        return s;
    }

    public double getFusedAttention() { return fusedAttention; }
    public double getClickRateScore() { return clickRateScore; }
    public double getDwellTimeScore() { return dwellTimeScore; }
    public double getCameraScore() { return cameraScore; }
    public double getMicScore() { return micScore; }

    private static double clamp(double v) {
        if (Double.isNaN(v)) return 0.0;
        return Math.max(0.0, Math.min(1.0, v));
    }
}
