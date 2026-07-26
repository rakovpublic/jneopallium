/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.security;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.AnomalyScoreSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.SignatureMatchSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.ThreatHypothesisSignal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Layer 4 threat-hypothesis neuron. Maintains posteriors over seeded
 * hypotheses and applies Bayesian updates from signature + anomaly
 * evidence. Loop=1 / Epoch=3.
 */
public class ThreatHypothesisNeuron extends ModulatableNeuron implements IThreatHypothesisNeuron {

    private static final class Hyp {
        final ThreatCategory category;
        double posterior;
        List<String> evidence = new ArrayList<>();
        Hyp(ThreatCategory c, double p) { this.category = c; this.posterior = p; }
    }

    private final Map<String, Hyp> hypotheses = new HashMap<>();
    private double posteriorThreshold = 0.1;

    public ThreatHypothesisNeuron() { super(); }
    public ThreatHypothesisNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    @Override
    public void seed(String hypothesisId, ThreatCategory category) {
        if (hypothesisId == null) return;
        if (hypotheses.containsKey(hypothesisId)) return;
        double share = hypotheses.isEmpty() ? 1.0 : 1.0 / hypotheses.size();
        hypotheses.put(hypothesisId, new Hyp(category == null ? ThreatCategory.UNKNOWN : category, share));
        renormalise();
    }

    @Override
    public ThreatHypothesisSignal updateFromSignature(SignatureMatchSignal m, String hypothesisId) {
        if (m == null || hypothesisId == null) return null;
        Hyp h = hypotheses.get(hypothesisId);
        if (h == null) return null;
        double lr = 1.0 + 4.0 * m.getConfidence();
        h.posterior *= lr;
        if (m.getSignatureId() != null) h.evidence.add("sig:" + m.getSignatureId());
        renormalise();
        return emit(hypothesisId, h);
    }

    @Override
    public ThreatHypothesisSignal updateFromAnomaly(AnomalyScoreSignal a, String hypothesisId) {
        if (a == null || hypothesisId == null) return null;
        Hyp h = hypotheses.get(hypothesisId);
        if (h == null) return null;
        double lr = 1.0 + 2.0 * a.getDeviationScore();
        h.posterior *= lr;
        if (a.getEntityId() != null) h.evidence.add("anom:" + a.getEntityId());
        renormalise();
        return emit(hypothesisId, h);
    }

    private ThreatHypothesisSignal emit(String id, Hyp h) {
        return new ThreatHypothesisSignal(id, h.category, h.posterior, h.evidence);
    }

    private void renormalise() {
        double sum = 0.0;
        for (Hyp h : hypotheses.values()) sum += h.posterior;
        if (sum <= 0) return;
        final double total = sum;
        hypotheses.forEach((k, v) -> v.posterior /= total);
    }

    @Override
    public List<ThreatHypothesisSignal> ranked() {
        List<Map.Entry<String, Hyp>> list = new ArrayList<>(hypotheses.entrySet());
        list.sort((a, b) -> Double.compare(b.getValue().posterior, a.getValue().posterior));
        List<ThreatHypothesisSignal> out = new ArrayList<>();
        for (Map.Entry<String, Hyp> e : list) {
            if (e.getValue().posterior < posteriorThreshold) continue;
            out.add(emit(e.getKey(), e.getValue()));
        }
        return out;
    }

    @Override public void setPosteriorThreshold(double t) { this.posteriorThreshold = Math.max(0.0, Math.min(1.0, t)); }
    @Override public double getPosteriorThreshold() { return posteriorThreshold; }
    @Override public Double posteriorOf(String hypothesisId) {
        Hyp h = hypotheses.get(hypothesisId);
        return h == null ? null : h.posterior;
    }
    @Override public int size() { return hypotheses.size(); }
}
