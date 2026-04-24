/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.security;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.AnomalyScoreSignal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Layer 2 adaptive anomaly detector. Computes per-entity deviation as
 * the normalised L2 distance between the observed feature vector and
 * the running baseline. Loop=1 / Epoch=2.
 */
public class AnomalyDetectorNeuron extends ModulatableNeuron implements IAnomalyDetectorNeuron {

    private final Map<String, double[]> baseline = new HashMap<>();
    private double softThreshold = 0.7;
    private double hardThreshold = 0.9;

    public AnomalyDetectorNeuron() { super(); }
    public AnomalyDetectorNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    /** Inject / replace the baseline for {@code entityId}. */
    public void setBaseline(String entityId, double[] vector) {
        if (entityId == null || vector == null) return;
        baseline.put(entityId, vector.clone());
    }

    @Override
    public AnomalyScoreSignal score(String entityId, double[] featureVector) {
        if (entityId == null || featureVector == null) return null;
        double[] b = baseline.get(entityId);
        if (b == null) {
            // No baseline yet → treat as uninformative, score 0.
            return new AnomalyScoreSignal(entityId, 0.0, new ArrayList<>());
        }
        int n = Math.min(b.length, featureVector.length);
        double distSq = 0.0;
        double normB = 0.0;
        List<String> contributors = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            double d = featureVector[i] - b[i];
            distSq += d * d;
            normB += b[i] * b[i];
            if (Math.abs(d) > 2.0 * Math.abs(b[i]) + 1e-3) contributors.add("f" + i);
        }
        double dist = Math.sqrt(distSq);
        double denom = Math.max(1e-6, Math.sqrt(normB) + dist);
        double score = Math.max(0.0, Math.min(1.0, dist / denom));
        return new AnomalyScoreSignal(entityId, score, contributors);
    }

    @Override public void setScoreThresholdSoft(double t) { this.softThreshold = Math.max(0.0, Math.min(1.0, t)); }
    @Override public void setScoreThresholdHard(double t) { this.hardThreshold = Math.max(0.0, Math.min(1.0, t)); }
    @Override public double getScoreThresholdSoft() { return softThreshold; }
    @Override public double getScoreThresholdHard() { return hardThreshold; }
}
