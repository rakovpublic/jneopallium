/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.curiosity;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.ai.signals.fast.AttentionGateSignal;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.curiosity.BoredomSignal;

import java.util.HashMap;
import java.util.Map;

/**
 * Tracks how frequently a context has been visited. When familiarity
 * crosses a threshold, emits an {@link AttentionGateSignal} suppressing
 * attention to that context (inhibition-of-return) to force exploration.
 * Layer 2, loop=2 / epoch=2.
 * <p>Biological analogue: habituation in inferior-temporal cortex driving
 * inhibition-of-return in superior colliculus (Itti &amp; Koch 2001).
 */
public class BoredomNeuron extends ModulatableNeuron {

    private final Map<String, Integer> visitCount = new HashMap<>();
    private double familiarityThreshold = 0.7;
    private int saturationVisits = 20;

    public BoredomNeuron() { super(); }

    public BoredomNeuron(Long neuronId, ISignalChain chain, Long run) {
        super(neuronId, chain, run);
    }

    public BoredomSignal visit(String contextHash) {
        if (contextHash == null) return null;
        int count = visitCount.getOrDefault(contextHash, 0) + 1;
        visitCount.put(contextHash, count);
        double familiarity = Math.min(1.0, (double) count / saturationVisits);
        BoredomSignal b = new BoredomSignal(contextHash, familiarity);
        b.setSourceNeuronId(this.getId());
        return b;
    }

    /**
     * If the context is over-familiar, emit an attention-gate suppressing it;
     * otherwise return null.
     */
    public AttentionGateSignal maybeSuppress(String contextHash) {
        Integer count = visitCount.get(contextHash);
        if (count == null) return null;
        double familiarity = Math.min(1.0, (double) count / saturationVisits);
        if (familiarity < familiarityThreshold) return null;
        AttentionGateSignal gate = new AttentionGateSignal(familiarity, contextHash, true);
        gate.setSourceNeuronId(this.getId());
        return gate;
    }

    public void reset(String contextHash) {
        if (contextHash != null) visitCount.remove(contextHash);
    }

    public double getFamiliarityThreshold() { return familiarityThreshold; }
    public void setFamiliarityThreshold(double t) { this.familiarityThreshold = Math.max(0.0, Math.min(1.0, t)); }
    public int getSaturationVisits() { return saturationVisits; }
    public void setSaturationVisits(int s) { this.saturationVisits = Math.max(1, s); }
    public int visitsFor(String contextHash) { return visitCount.getOrDefault(contextHash, 0); }
}
