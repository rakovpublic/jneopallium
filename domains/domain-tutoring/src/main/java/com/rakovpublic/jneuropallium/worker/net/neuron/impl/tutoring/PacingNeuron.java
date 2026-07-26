/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.tutoring;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;

/**
 * Layer 5 pacing controller. Adjusts the fast/slow {@code CycleNeuron} ratio
 * based on item difficulty and observed flow state.
 * Loop=2 / Epoch=1.
 */
public class PacingNeuron extends ModulatableNeuron implements IPacingNeuron {

    private int fastSlowRatioMin = 5;
    private int fastSlowRatioMax = 20;
    private int currentRatio = 10;

    public PacingNeuron() { super(); }
    public PacingNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    /**
     * Choose a ratio from flow state and content difficulty. Reflection-heavy
     * HARD items tilt slower; FLOW/drill on EASY items tilts faster.
     */
    public int computeRatio(FlowStateKind state, DifficultyLevel difficulty) {
        int ratio = currentRatio;
        if (state == FlowStateKind.OVERLOAD || state == FlowStateKind.FRUSTRATION) {
            ratio -= 2;
        } else if (state == FlowStateKind.BOREDOM) {
            ratio += 2;
        } else if (state == FlowStateKind.FLOW) {
            ratio += 1;
        }
        if (difficulty == DifficultyLevel.HARD) ratio -= 2;
        else if (difficulty == DifficultyLevel.EASY) ratio += 1;

        currentRatio = Math.max(fastSlowRatioMin, Math.min(fastSlowRatioMax, ratio));
        return currentRatio;
    }

    public void setFastSlowRatioMin(int v) { this.fastSlowRatioMin = Math.max(1, v); }
    public void setFastSlowRatioMax(int v) { this.fastSlowRatioMax = Math.max(1, v); }
    public int getCurrentRatio() { return currentRatio; }
    public int getFastSlowRatioMin() { return fastSlowRatioMin; }
    public int getFastSlowRatioMax() { return fastSlowRatioMax; }
}
