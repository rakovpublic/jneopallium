/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.tutoring;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;

/**
 * Layer 2 classifier of moment-to-moment cognitive state (Csikszentmihalyi
 * 1990: flow). Integrates engagement, affect (valence/arousal), and the
 * recent accuracy trajectory to produce one of the {@link FlowStateKind}
 * classes.
 * Loop=2 / Epoch=1.
 */
public class FlowStateNeuron extends ModulatableNeuron {

    private FlowStateKind currentState = FlowStateKind.NEUTRAL;
    private double lastEngagement;
    private double lastValence;
    private double lastArousal;
    private double lastAccuracy;

    public FlowStateNeuron() { super(); }
    public FlowStateNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    public FlowStateKind classify(double engagement, double valence,
                                  double arousal, double recentAccuracy) {
        this.lastEngagement = clamp01(engagement);
        this.lastValence = clamp(valence, -1.0, 1.0);
        this.lastArousal = clamp01(arousal);
        this.lastAccuracy = clamp01(recentAccuracy);

        if (lastEngagement < 0.3 && lastArousal < 0.3) {
            currentState = FlowStateKind.BOREDOM;
        } else if (lastValence < -0.3 && lastArousal > 0.6 && lastAccuracy < 0.4) {
            currentState = FlowStateKind.FRUSTRATION;
        } else if (lastArousal > 0.75 && lastAccuracy < 0.3) {
            currentState = FlowStateKind.OVERLOAD;
        } else if (lastEngagement > 0.6 && lastValence > 0.0
                && lastAccuracy >= 0.6 && lastAccuracy <= 0.85) {
            currentState = FlowStateKind.FLOW;
        } else {
            currentState = FlowStateKind.NEUTRAL;
        }
        return currentState;
    }

    public FlowStateKind getCurrentState() { return currentState; }
    public double getLastEngagement() { return lastEngagement; }
    public double getLastValence() { return lastValence; }
    public double getLastArousal() { return lastArousal; }
    public double getLastAccuracy() { return lastAccuracy; }

    private static double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }
    private static double clamp01(double v) { return clamp(v, 0.0, 1.0); }
}
