/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.DiagnosisHypothesisSignal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Layer 3 differential-diagnosis neuron. Maintains a bounded ranked list
 * of candidate ICD-10 diagnoses and performs an approximate Bayesian
 * update whenever new evidence arrives. The prior is uniform; each
 * observation multiplies the current posterior by a likelihood ratio
 * derived from the strength parameter of the evidence. Posterior is
 * renormalised across all active candidates. Loop=1 / Epoch=2.
 */
public class DifferentialDiagnosisNeuron extends ModulatableNeuron implements IDifferentialDiagnosisNeuron {

    private final Map<String, Double> posterior = new HashMap<>();
    private final Map<String, List<String>> evidenceRefs = new HashMap<>();
    private int maxCandidates = 10;
    private double posteriorThreshold = 0.1;
    private String patientId;

    public DifferentialDiagnosisNeuron() { super(); }

    public DifferentialDiagnosisNeuron(Long neuronId, ISignalChain chain, Long run) {
        super(neuronId, chain, run);
    }

    public void setMaxCandidates(int n) { this.maxCandidates = Math.max(1, n); }
    public int getMaxCandidates() { return maxCandidates; }
    public void setPosteriorThreshold(double t) {
        this.posteriorThreshold = Math.max(0.0, Math.min(1.0, t));
    }
    public double getPosteriorThreshold() { return posteriorThreshold; }
    public void setPatientId(String p) { this.patientId = p; }
    public String getPatientId() { return patientId; }

    /**
     * Introduce a candidate with a uniform prior if unseen; otherwise,
     * updates the stored evidence list without touching posteriors.
     */
    public void seed(String icd10) {
        if (icd10 == null || icd10.isEmpty()) return;
        if (!posterior.containsKey(icd10)) {
            // Insert at the current uniform share so that uniformly-seeded
            // candidates remain uniformly weighted after renormalisation.
            double share = posterior.isEmpty() ? 1.0 : 1.0 / posterior.size();
            posterior.put(icd10, share);
            evidenceRefs.put(icd10, new ArrayList<>());
            renormalise();
            enforceBound();
        }
    }

    /**
     * Bayesian-style update. likelihoodRatio &gt; 1 supports the hypothesis,
     * &lt; 1 reduces it.
     */
    public void update(String icd10, double likelihoodRatio, String evidenceId) {
        if (icd10 == null || icd10.isEmpty()) return;
        double lr = Math.max(1e-6, likelihoodRatio);
        posterior.merge(icd10, lr, (old, add) -> old * add);
        evidenceRefs.computeIfAbsent(icd10, k -> new ArrayList<>());
        if (evidenceId != null) evidenceRefs.get(icd10).add(evidenceId);
        renormalise();
        enforceBound();
    }

    private void renormalise() {
        double sum = 0;
        for (Double v : posterior.values()) sum += v;
        if (sum <= 0) return;
        final double total = sum;
        posterior.replaceAll((k, v) -> v / total);
    }

    private void enforceBound() {
        if (posterior.size() <= maxCandidates) return;
        List<Map.Entry<String, Double>> list = new ArrayList<>(posterior.entrySet());
        list.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        for (int i = maxCandidates; i < list.size(); i++) {
            posterior.remove(list.get(i).getKey());
            evidenceRefs.remove(list.get(i).getKey());
        }
        renormalise();
    }

    /**
     * Snapshot of the current ranked list, filtered by the posterior
     * threshold.
     */
    public List<DiagnosisHypothesisSignal> ranked() {
        List<DiagnosisHypothesisSignal> out = new ArrayList<>();
        List<Map.Entry<String, Double>> list = new ArrayList<>(posterior.entrySet());
        list.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        for (Map.Entry<String, Double> e : list) {
            if (e.getValue() < posteriorThreshold) continue;
            out.add(new DiagnosisHypothesisSignal(e.getKey(), e.getValue(),
                    evidenceRefs.getOrDefault(e.getKey(), new ArrayList<>()), patientId));
        }
        return out;
    }

    public Double posteriorOf(String icd10) { return posterior.get(icd10); }
    public int size() { return posterior.size(); }
    public void reset() { posterior.clear(); evidenceRefs.clear(); }

    /** True when the top candidate's posterior is at least {@code margin} above the second. */
    public boolean hasConfidentWinner(double margin) {
        List<Double> vals = new ArrayList<>(posterior.values());
        vals.sort(Comparator.reverseOrder());
        if (vals.isEmpty()) return false;
        if (vals.size() == 1) return vals.get(0) >= posteriorThreshold;
        return (vals.get(0) - vals.get(1)) >= margin;
    }
}
