/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.tutoring;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.tutoring.ContentRecommendationSignal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Layer 4 zone-of-proximal-development planner (Vygotsky 1978). Scores
 * candidate items by the proximity of predicted success probability to a
 * target success rate (default 0.75) — not-too-easy, not-too-hard.
 * Loop=1 / Epoch=3.
 */
public class ZPDPlanningNeuron extends ModulatableNeuron implements IZPDPlanningNeuron {

    public static final class Candidate {
        public final String itemId;
        public final String conceptId;
        public final double predictedSuccess;
        public Candidate(String itemId, String conceptId, double predictedSuccess) {
            this.itemId = itemId;
            this.conceptId = conceptId;
            this.predictedSuccess = predictedSuccess;
        }
    }

    private double targetSuccessRate = 0.75;

    public ZPDPlanningNeuron() { super(); }
    public ZPDPlanningNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    /**
     * Score each candidate by ZPD fit and emit a ContentRecommendationSignal
     * for the best item. Returns null if no candidates.
     */
    public ContentRecommendationSignal plan(List<Candidate> candidates) {
        if (candidates == null || candidates.isEmpty()) return null;
        List<double[]> scored = new ArrayList<>();
        for (int i = 0; i < candidates.size(); i++) {
            Candidate c = candidates.get(i);
            double fit = 1.0 - Math.abs(c.predictedSuccess - targetSuccessRate);
            scored.add(new double[]{i, fit});
        }
        Collections.sort(scored, (a, b) -> Double.compare(b[1], a[1]));
        int best = (int) scored.get(0)[0];
        Candidate winner = candidates.get(best);
        ContentRecommendationSignal s = new ContentRecommendationSignal(
                winner.itemId,
                "zpd-fit:" + winner.conceptId,
                scored.get(0)[1]);
        s.setSourceNeuronId(this.getId());
        return s;
    }

    public void setTargetSuccessRate(double r) {
        this.targetSuccessRate = Math.max(0.0, Math.min(1.0, r));
    }
    public double getTargetSuccessRate() { return targetSuccessRate; }
}
