/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.tutoring;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Layer 7 fairness / accessibility guard. Hard-constraints the tutor against
 * penalising learner response time when an accommodation flag applies
 * (extra-time, screen-reader, reduced-animation, etc.).
 * Loop=2 / Epoch=3.
 */
public class FairnessNeuron extends ModulatableNeuron implements IFairnessNeuron {

    private final Set<String> accommodationFlags = new HashSet<>();
    private boolean responseTimePenalty = true;

    public FairnessNeuron() { super(); }
    public FairnessNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    public void addAccommodation(String flag) {
        if (flag != null) accommodationFlags.add(flag);
        responseTimePenalty = accommodationFlags.isEmpty();
    }

    public void removeAccommodation(String flag) {
        accommodationFlags.remove(flag);
        responseTimePenalty = accommodationFlags.isEmpty();
    }

    public boolean hasAccommodation(String flag) {
        return accommodationFlags.contains(flag);
    }

    public Set<String> accommodations() {
        return Collections.unmodifiableSet(accommodationFlags);
    }

    /**
     * Apply a penalty for slow latency iff no accommodation flag is present.
     * Returns the adjusted score (never smaller than 0).
     */
    public double adjustScoreForLatency(double rawScore, long latencyMs, long expectedMs) {
        if (!responseTimePenalty) return Math.max(0.0, rawScore);
        if (latencyMs <= expectedMs) return Math.max(0.0, rawScore);
        double over = (latencyMs - expectedMs) / (double) Math.max(1, expectedMs);
        return Math.max(0.0, rawScore * Math.max(0.0, 1.0 - 0.1 * over));
    }

    public boolean isResponseTimePenaltyEnabled() { return responseTimePenalty; }

    /** Hard constraint: this neuron never modifies EthicalPriorityNeuron. */
    public boolean wouldModifyEthicalPriority() { return false; }
}
