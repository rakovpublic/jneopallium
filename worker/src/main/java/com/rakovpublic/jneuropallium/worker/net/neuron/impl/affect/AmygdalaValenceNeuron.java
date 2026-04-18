/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.affect;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.affect.AppraisalSignal;

/**
 * Amygdala-like fast threat/reward tagging neuron.
 * Layer 2, loop=1 / epoch=1 — runs on the fast sensory loop.
 * <p>Biological analogue: basolateral &amp; central amygdala nuclei
 * (LeDoux 1998).
 */
public class AmygdalaValenceNeuron extends ModulatableNeuron implements IAffectiveNeuron {

    private double valence;
    private double arousal;
    private long lastTick;
    private double firingThreshold;
    private final double baselineThreshold;

    public AmygdalaValenceNeuron() {
        super();
        this.firingThreshold = 0.5;
        this.baselineThreshold = 0.5;
    }

    public AmygdalaValenceNeuron(Long neuronId, ISignalChain chain, Long run) {
        super(neuronId, chain, run);
        this.firingThreshold = 0.5;
        this.baselineThreshold = 0.5;
    }

    /**
     * Tag an incoming spike magnitude with valence. Threat-positive input
     * (high magnitude combined with low controllability) produces a
     * negative-valence emission.
     *
     * @param spikeMagnitude incoming spike magnitude
     * @param threatCue non-negative cue strength (pain or threat)
     * @return the updated valence in [-1,1]
     */
    public double tag(double spikeMagnitude, double threatCue) {
        double v = -clamp01(threatCue) + 0.5 * Math.tanh(spikeMagnitude - 1.0);
        this.valence = clamp(v, -1.0, 1.0);
        this.arousal = clamp01(Math.max(threatCue, Math.abs(spikeMagnitude)));
        this.lastTick++;
        return this.valence;
    }

    @Override
    public AffectState currentState() {
        return new AffectState(valence, arousal, lastTick);
    }

    @Override
    public void modulateThreshold(double arousalFactor) {
        double factor = clamp01(arousalFactor);
        this.firingThreshold = Math.max(0.05, baselineThreshold * (1.0 - 0.5 * factor));
    }

    @Override
    public void onAppraisal(AppraisalSignal s) {
        if (s == null) return;
        this.valence = clamp(this.valence + 0.5 * s.getGoalDelta(), -1.0, 1.0);
        this.arousal = clamp01(this.arousal + 0.25 * s.getNovelty());
    }

    public double getValence() { return valence; }
    public double getArousal() { return arousal; }
    public double getFiringThreshold() { return firingThreshold; }
    public double getBaselineThreshold() { return baselineThreshold; }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
    private static double clamp01(double v) { return clamp(v, 0.0, 1.0); }
}
