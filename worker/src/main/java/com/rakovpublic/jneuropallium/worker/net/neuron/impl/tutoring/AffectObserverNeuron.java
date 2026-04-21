/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.tutoring;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.tutoring.AffectObservationSignal;

/**
 * Layer 0 coarse-grained affect inference. Uses only behavioural proxies
 * (engagement trajectory, response-accuracy trajectory). Feeds the affect
 * module; not a psychological diagnosis.
 * Loop=1 / Epoch=2.
 */
public class AffectObserverNeuron extends ModulatableNeuron implements IAffectObserverNeuron {

    private double engagementEwma = 0.5;
    private double accuracyEwma = 0.5;
    private final double alpha = 0.3;

    public AffectObserverNeuron() { super(); }
    public AffectObserverNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    /**
     * Infer coarse affect from a new engagement sample and recent accuracy.
     * Valence rises with accuracy; arousal rises with engagement variance.
     */
    public AffectObservationSignal infer(double engagement, double accuracy) {
        double prevEng = engagementEwma;
        engagementEwma = (1 - alpha) * engagementEwma + alpha * clamp01(engagement);
        accuracyEwma = (1 - alpha) * accuracyEwma + alpha * clamp01(accuracy);

        double valence = 2.0 * (accuracyEwma - 0.5);
        double arousal = clamp01(0.5 * engagementEwma + 0.5 * Math.abs(engagementEwma - prevEng) * 4.0);
        double confidence = 0.5;
        AffectObservationSignal s = new AffectObservationSignal(valence, arousal, confidence);
        s.setSourceNeuronId(this.getId());
        return s;
    }

    public double getEngagementEwma() { return engagementEwma; }
    public double getAccuracyEwma() { return accuracyEwma; }

    private static double clamp01(double v) { return Math.max(0.0, Math.min(1.0, v)); }
}
