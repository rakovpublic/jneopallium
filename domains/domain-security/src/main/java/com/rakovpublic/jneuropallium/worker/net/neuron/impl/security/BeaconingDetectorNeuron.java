/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.security;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.AnomalyScoreSignal;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * Layer 2 C2-beacon detector. Flags a sequence of connection timestamps
 * whose inter-arrival coefficient of variation is below {@code
 * jitterTolerance} — a low CV is the hallmark of scheduled beaconing.
 * Loop=1 / Epoch=3.
 */
public class BeaconingDetectorNeuron extends ModulatableNeuron implements IBeaconingDetectorNeuron {

    private final Map<String, Deque<Long>> history = new HashMap<>();
    private int minSamples = 8;
    private int windowSize = 64;
    private double jitterTolerance = 0.15;

    public BeaconingDetectorNeuron() { super(); }
    public BeaconingDetectorNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    public void setWindowSize(int n) { this.windowSize = Math.max(this.minSamples, n); }

    @Override
    public void observe(String entityId, long tick) {
        if (entityId == null) return;
        Deque<Long> d = history.computeIfAbsent(entityId, k -> new ArrayDeque<>());
        d.addLast(tick);
        while (d.size() > windowSize) d.removeFirst();
    }

    @Override
    public AnomalyScoreSignal assess(String entityId) {
        Deque<Long> d = entityId == null ? null : history.get(entityId);
        if (d == null || d.size() < minSamples) return null;
        Long[] arr = d.toArray(new Long[0]);
        Arrays.sort(arr);
        double[] gaps = new double[arr.length - 1];
        double mean = 0.0;
        for (int i = 1; i < arr.length; i++) {
            gaps[i - 1] = arr[i] - arr[i - 1];
            mean += gaps[i - 1];
        }
        mean /= gaps.length;
        if (mean == 0) return null;
        double var = 0.0;
        for (double g : gaps) { double diff = g - mean; var += diff * diff; }
        var /= gaps.length;
        double cv = Math.sqrt(var) / mean;
        if (cv <= jitterTolerance) {
            double score = 1.0 - cv / Math.max(1e-6, jitterTolerance);
            score = Math.max(0.0, Math.min(1.0, score));
            ArrayList<String> features = new ArrayList<>();
            features.add("beacon");
            features.add("cv=" + cv);
            return new AnomalyScoreSignal(entityId, score, features);
        }
        return null;
    }

    @Override public void setMinSamples(int n) { this.minSamples = Math.max(3, n); }
    @Override public int getMinSamples() { return minSamples; }
    @Override public void setJitterTolerance(double j) { this.jitterTolerance = Math.max(1e-6, j); }
    @Override public double getJitterTolerance() { return jitterTolerance; }
}
