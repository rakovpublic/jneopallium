/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.tutoring;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;

import java.util.List;

/**
 * Layer 5 action selection for tutoring content. Softmax over candidate items
 * combining ZPD fit and an intrinsic-curiosity novelty term.
 * Loop=1 / Epoch=1.
 */
public class ContentSelectionNeuron extends ModulatableNeuron implements IContentSelectionNeuron {

    public static final class ScoredItem {
        public final String itemId;
        public final double zpdFit;
        public final double novelty;
        public ScoredItem(String itemId, double zpdFit, double novelty) {
            this.itemId = itemId;
            this.zpdFit = zpdFit;
            this.novelty = novelty;
        }
    }

    private double betaNovelty = 0.15;
    private double temperature = 0.7;

    public ContentSelectionNeuron() { super(); }
    public ContentSelectionNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    public String select(List<ScoredItem> items) {
        if (items == null || items.isEmpty()) return null;
        double[] logits = new double[items.size()];
        double max = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < items.size(); i++) {
            ScoredItem it = items.get(i);
            logits[i] = (it.zpdFit + betaNovelty * it.novelty) / Math.max(1e-6, temperature);
            if (logits[i] > max) max = logits[i];
        }
        double sum = 0.0;
        double[] expv = new double[items.size()];
        for (int i = 0; i < items.size(); i++) { expv[i] = Math.exp(logits[i] - max); sum += expv[i]; }
        double u = Math.random() * sum;
        double acc = 0.0;
        for (int i = 0; i < items.size(); i++) {
            acc += expv[i];
            if (u <= acc) return items.get(i).itemId;
        }
        return items.get(items.size() - 1).itemId;
    }

    /** Deterministic argmax over composite score, for tests. */
    public String argmax(List<ScoredItem> items) {
        if (items == null || items.isEmpty()) return null;
        int best = 0;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < items.size(); i++) {
            ScoredItem it = items.get(i);
            double s = it.zpdFit + betaNovelty * it.novelty;
            if (s > bestScore) { bestScore = s; best = i; }
        }
        return items.get(best).itemId;
    }

    public void setBetaNovelty(double b) { this.betaNovelty = Math.max(0.0, b); }
    public double getBetaNovelty() { return betaNovelty; }
    public void setTemperature(double t) { this.temperature = Math.max(1e-6, t); }
    public double getTemperature() { return temperature; }
}
