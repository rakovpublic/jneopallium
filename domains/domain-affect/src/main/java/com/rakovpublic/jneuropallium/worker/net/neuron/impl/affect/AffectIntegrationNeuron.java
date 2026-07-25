/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.affect;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.affect.AffectStateSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.affect.AppraisalSignal;

/**
 * Combines appraisal + interoceptive summary into a unified AffectState and
 * emits {@link AffectStateSignal}.
 * Layer 2, loop=2 / epoch=1.
 * <p>Biological analogue: ventromedial prefrontal / orbitofrontal integration
 * of cognitive appraisal and interoception.
 */
public class AffectIntegrationNeuron extends ModulatableNeuron implements IAffectiveNeuron, IAffectIntegrationNeuron {

    private double valence;
    private double arousal;
    private long tick;
    private String contextId;
    private double firingThreshold;
    private final double baselineThreshold;

    public AffectIntegrationNeuron() {
        super();
        this.firingThreshold = 0.5;
        this.baselineThreshold = 0.5;
    }

    public AffectIntegrationNeuron(Long neuronId, ISignalChain chain, Long run) {
        super(neuronId, chain, run);
        this.firingThreshold = 0.5;
        this.baselineThreshold = 0.5;
    }

    /**
     * Combine cognitive appraisal with an interoceptive summary and emit a new state.
     *
     * @param goalDelta cognitive goal-progress delta
     * @param novelty context novelty in [0,1]
     * @param controllability perceived control in [0,1]
     * @param homeostaticError current body error in [0,1]
     * @param painMagnitude current pain in [0,1]
     * @return broadcast-ready AffectStateSignal
     */
    public AffectStateSignal integrate(double goalDelta, double novelty,
                                       double controllability,
                                       double homeostaticError,
                                       double painMagnitude) {
        double v = Math.tanh(goalDelta) - 0.5 * clamp01(painMagnitude) - 0.3 * clamp01(homeostaticError);
        double a = clamp01(0.5 * clamp01(novelty) + 0.5 * clamp01(painMagnitude) + 0.25 * (1 - clamp01(controllability)));
        this.valence = clamp(v, -1.0, 1.0);
        this.arousal = a;
        this.tick++;
        AffectStateSignal out = new AffectStateSignal(valence, arousal, contextId);
        out.setSourceNeuronId(this.getId());
        return out;
    }

    @Override public AffectState currentState() { return new AffectState(valence, arousal, tick); }

    @Override
    public void modulateThreshold(double arousalFactor) {
        double factor = clamp01(arousalFactor);
        this.firingThreshold = Math.max(0.05, baselineThreshold * (1.0 - 0.5 * factor));
    }

    @Override
    public void onAppraisal(AppraisalSignal s) {
        if (s == null) return;
        this.valence = clamp(this.valence + 0.3 * s.getGoalDelta(), -1.0, 1.0);
        this.arousal = clamp01(this.arousal + 0.2 * s.getNovelty());
    }

    public String getContextId() { return contextId; }
    public void setContextId(String contextId) { this.contextId = contextId; }

    public double getValence() { return valence; }
    public double getArousal() { return arousal; }
    public double getFiringThreshold() { return firingThreshold; }
    public double getBaselineThreshold() { return baselineThreshold; }

    private static double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }
    private static double clamp01(double v) { return clamp(v, 0.0, 1.0); }
}
