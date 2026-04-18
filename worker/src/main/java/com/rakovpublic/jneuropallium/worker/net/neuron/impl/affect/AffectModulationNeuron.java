/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.affect;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.affect.AffectStateSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.affect.AppraisalSignal;

/**
 * Broadcasts {@link AffectStateSignal} as a neuromodulatory gain on
 * learning rates and harm thresholds.
 * Layer 7, loop=2 / epoch=1.
 * <p>Biological analogue: brainstem/diencephalic neuromodulatory nuclei
 * (locus coeruleus, raphe nuclei, VTA) broadcasting global state.
 */
public class AffectModulationNeuron extends ModulatableNeuron implements IAffectiveNeuron {

    private double valence;
    private double arousal;
    private long lastUpdateTick;
    private double firingThreshold;
    private final double baselineThreshold;

    /** Short-term learning scale = 1 + arousal. */
    private double shortTermLearningScale;
    /** Long-term consolidation scale = 1 - 0.5 * arousal. */
    private double longTermConsolidationScale;
    /** Exploration bonus multiplier derived from valence. */
    private double explorationBonus;
    /** Harm threshold multiplier derived from valence + arousal, clamped. */
    private double harmThresholdMultiplier;

    private double harmClampMin = 1.0;
    private double harmClampMax = 5.0;

    public AffectModulationNeuron() {
        super();
        this.firingThreshold = 0.5;
        this.baselineThreshold = 0.5;
        this.shortTermLearningScale = 1.0;
        this.longTermConsolidationScale = 1.0;
        this.explorationBonus = 1.0;
        this.harmThresholdMultiplier = 1.0;
    }

    public AffectModulationNeuron(Long neuronId, ISignalChain chain, Long run) {
        super(neuronId, chain, run);
        this.firingThreshold = 0.5;
        this.baselineThreshold = 0.5;
        this.shortTermLearningScale = 1.0;
        this.longTermConsolidationScale = 1.0;
        this.explorationBonus = 1.0;
        this.harmThresholdMultiplier = 1.0;
    }

    /**
     * Accept a new {@link AffectStateSignal} and update modulation scales.
     */
    public void onAffect(AffectStateSignal s) {
        if (s == null) return;
        this.valence = s.getValence();
        this.arousal = s.getArousal();
        this.lastUpdateTick++;
        // Per CLAUDE.md §4.5:
        this.shortTermLearningScale = 1.0 + arousal;
        this.longTermConsolidationScale = Math.max(0.0, 1.0 - 0.5 * arousal);
        this.explorationBonus = valence < 0 ? Math.max(0.0, 1.0 + valence) : 1.0;
        double raw = (1.0 - valence) / 2.0 + arousal;
        this.harmThresholdMultiplier = clamp(raw, harmClampMin, harmClampMax);
    }

    @Override public AffectState currentState() { return new AffectState(valence, arousal, lastUpdateTick); }

    @Override
    public void modulateThreshold(double arousalFactor) {
        double f = clamp(arousalFactor, 0.0, 1.0);
        this.firingThreshold = Math.max(0.05, baselineThreshold * (1.0 - 0.5 * f));
    }

    @Override
    public void onAppraisal(AppraisalSignal s) {
        if (s == null) return;
        this.valence = clamp(this.valence + 0.2 * s.getGoalDelta(), -1.0, 1.0);
        this.arousal = clamp(this.arousal + 0.2 * s.getNovelty(), 0.0, 1.0);
    }

    public double getShortTermLearningScale() { return shortTermLearningScale; }
    public double getLongTermConsolidationScale() { return longTermConsolidationScale; }
    public double getExplorationBonus() { return explorationBonus; }
    public double getHarmThresholdMultiplier() { return harmThresholdMultiplier; }

    public double getHarmClampMin() { return harmClampMin; }
    public void setHarmClampMin(double harmClampMin) { this.harmClampMin = harmClampMin; }
    public double getHarmClampMax() { return harmClampMax; }
    public void setHarmClampMax(double harmClampMax) { this.harmClampMax = harmClampMax; }

    public double getValence() { return valence; }
    public double getArousal() { return arousal; }
    public double getFiringThreshold() { return firingThreshold; }
    public double getBaselineThreshold() { return baselineThreshold; }

    private static double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }
}
