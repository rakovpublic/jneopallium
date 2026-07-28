/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.ClinicalVetoSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.TreatmentProposalSignal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Layer 5 recommendation neuron. Specialises the autonomous-AI
 * {@code ActionSelectionNeuron} for clinical use. Filters incoming
 * candidate {@link TreatmentProposalSignal}s through the contraindication
 * gate, ranks the survivors by benefit − risk, and emits them with an
 * explicit "advisory only" rationale. Loop=1 / Epoch=1.
 *
 * <p><b>Non-negotiable invariant:</b> this neuron must never emit a
 * {@code MotorCommandSignal} with {@code execute=true}. Execution flows
 * through a separate physician-confirmation path.
 */
public class RecommendationNeuron extends ModulatableNeuron implements IRecommendationNeuron {

    /** Explicit, read-only advisory mode flag — cannot be flipped programmatically. */
    public static final String MODE = "advisory";

    /** Immutable evaluated proposal with citation + status. */
    public static final class Recommendation {
        public final TreatmentProposalSignal proposal;
        public final boolean vetoed;
        public final ClinicalVetoSignal veto;
        public final String mode;
        public Recommendation(TreatmentProposalSignal proposal, ClinicalVetoSignal veto) {
            this.proposal = proposal;
            this.veto = veto;
            this.vetoed = veto != null;
            this.mode = MODE;
        }
    }

    private IContraindicationNeuron contraindication;
    private int topK = 3;
    private double minBenefitMinusRisk = 0.0;

    public RecommendationNeuron() { super(); }

    public RecommendationNeuron(Long neuronId, ISignalChain chain, Long run) {
        super(neuronId, chain, run);
    }

    public void setContraindicationFilter(IContraindicationNeuron c) { this.contraindication = c; }
    public void setTopK(int k) { this.topK = Math.max(1, k); }
    public int getTopK() { return topK; }
    public void setMinBenefitMinusRisk(double v) { this.minBenefitMinusRisk = v; }
    public double getMinBenefitMinusRisk() { return minBenefitMinusRisk; }
    public String getMode() { return MODE; }

    /**
     * Evaluate each candidate, attaching any veto. Vetoed candidates are
     * retained in the returned list with {@code vetoed=true} so that the
     * audit dashboard can show the rejected options.
     */
    public List<Recommendation> evaluate(List<TreatmentProposalSignal> candidates) {
        List<Recommendation> out = new ArrayList<>();
        if (candidates == null) return out;
        for (TreatmentProposalSignal p : candidates) {
            if (p == null) continue;
            ClinicalVetoSignal v = contraindication == null ? null : contraindication.evaluate(p);
            out.add(new Recommendation(p, v));
        }
        return out;
    }

    /** Returns ranked surviving recommendations, capped at {@code topK}. */
    public List<Recommendation> recommend(List<TreatmentProposalSignal> candidates) {
        List<Recommendation> all = evaluate(candidates);
        List<Recommendation> survivors = new ArrayList<>();
        for (Recommendation r : all) {
            if (r.vetoed) continue;
            double score = r.proposal.getExpectedBenefit() - r.proposal.getExpectedRisk();
            if (score < minBenefitMinusRisk) continue;
            survivors.add(r);
        }
        survivors.sort(Comparator.comparingDouble(
                (Recommendation r) -> r.proposal.getExpectedBenefit() - r.proposal.getExpectedRisk()).reversed());
        if (survivors.size() > topK) return survivors.subList(0, topK);
        return survivors;
    }

    /** Helper for audit trail: all vetoed proposals with their citations. */
    public List<Recommendation> rejected(List<TreatmentProposalSignal> candidates) {
        List<Recommendation> out = new ArrayList<>();
        for (Recommendation r : evaluate(candidates)) if (r.vetoed) out.add(r);
        return out;
    }
}
