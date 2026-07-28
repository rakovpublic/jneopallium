/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.curiosity;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.curiosity.LearningProgressSignal;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * Tracks per-domain {@code d(error)/dt} over a sliding window and emits an
 * intrinsic-reward {@link LearningProgressSignal} whose magnitude is positive
 * when error is decreasing (learning) and negative when it grows.
 * Layer 6, loop=2 / epoch=2.
 * <p>Biological analogue: dopaminergic reward-prediction-error scaling with
 * positive learning progress (Oudeyer &amp; Kaplan 2007).
 */
public class LearningProgressNeuron extends ModulatableNeuron implements ILearningProgressNeuron {

    private final Map<String, Deque<Double>> errorsByDomain = new HashMap<>();
    private int windowTicks = 200;

    public LearningProgressNeuron() { super(); }

    public LearningProgressNeuron(Long neuronId, ISignalChain chain, Long run) {
        super(neuronId, chain, run);
    }

    public LearningProgressSignal recordError(String domain, double error) {
        if (domain == null) return null;
        Deque<Double> window = errorsByDomain.computeIfAbsent(domain, k -> new ArrayDeque<>());
        window.addLast(error);
        while (window.size() > windowTicks) window.removeFirst();
        double derivative = 0;
        if (window.size() >= 2) {
            double first = window.peekFirst();
            double last = window.peekLast();
            derivative = (last - first) / Math.max(1, window.size() - 1);
        }
        LearningProgressSignal sig = new LearningProgressSignal(domain, derivative);
        sig.setSourceNeuronId(this.getId());
        return sig;
    }

    /**
     * Intrinsic reward — positive for decreasing error.
     */
    public double intrinsicReward(String domain) {
        Deque<Double> window = errorsByDomain.get(domain);
        if (window == null || window.size() < 2) return 0.0;
        double first = window.peekFirst();
        double last = window.peekLast();
        double derivative = (last - first) / Math.max(1, window.size() - 1);
        return -derivative;
    }

    public int getWindowTicks() { return windowTicks; }
    public void setWindowTicks(int windowTicks) { this.windowTicks = Math.max(2, windowTicks); }
    public int domainCount() { return errorsByDomain.size(); }
}
