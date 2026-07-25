/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;

/**
 * Layer 4 clinical consequence model. Specialises the autonomous-AI
 * {@code ConsequenceModelNeuron} with a minimal pharmacokinetic /
 * pharmacodynamic forward model: one-compartment exponential decay for
 * plasma concentration, sigmoidal Emax response for effect. Used by
 * {@link TreatmentPlanningNeuron} to produce expected-benefit / expected-
 * risk for each candidate {@code TreatmentProposalSignal}. Loop=1 /
 * Epoch=2.
 */
public class ClinicalConsequenceModelNeuron extends ModulatableNeuron implements IClinicalConsequenceModelNeuron {

    /** Minimal PK/PD parameters for a single drug. */
    public static final class PkPdParams {
        public final double bioavailability;     // F ∈ [0,1]
        public final double volumeOfDistribution; // L
        public final double clearance;            // L/h
        public final double emax;                 // max effect ∈ [0,1]
        public final double ec50;                 // concentration at half-max effect (mg/L)
        public final double toxicThreshold;       // mg/L above which risk accumulates

        public PkPdParams(double f, double vd, double cl, double emax, double ec50, double toxic) {
            this.bioavailability = clamp01(f);
            this.volumeOfDistribution = Math.max(0.1, vd);
            this.clearance = Math.max(0.01, cl);
            this.emax = clamp01(emax);
            this.ec50 = Math.max(1e-6, ec50);
            this.toxicThreshold = Math.max(this.ec50, toxic);
        }
        private static double clamp01(double v) { return Math.max(0.0, Math.min(1.0, v)); }
    }

    /** Immutable forward-model forecast. */
    public static final class Forecast {
        public final double peakConcentration;
        public final double effectAtPeak;
        public final double toxicityRisk;
        public final double expectedBenefit;
        public final double expectedRisk;

        public Forecast(double peak, double effect, double tox, double benefit, double risk) {
            this.peakConcentration = peak;
            this.effectAtPeak = effect;
            this.toxicityRisk = tox;
            this.expectedBenefit = benefit;
            this.expectedRisk = risk;
        }
    }

    public ClinicalConsequenceModelNeuron() { super(); }

    public ClinicalConsequenceModelNeuron(Long neuronId, ISignalChain chain, Long run) {
        super(neuronId, chain, run);
    }

    /**
     * Deterministic one-shot forecast for a single oral/iv dose.
     *
     * @param doseMg          administered dose in milligrams
     * @param weightKg        patient weight in kilograms (>0)
     * @param p               PK/PD parameters
     * @param vulnerability   PatientContextNeuron#getVulnerabilityFactor (≥1)
     */
    public Forecast simulate(double doseMg, double weightKg, PkPdParams p, double vulnerability) {
        if (p == null || doseMg <= 0 || weightKg <= 0) {
            return new Forecast(0, 0, 0, 0, 0);
        }
        double v = vulnerability < 1.0 ? 1.0 : vulnerability;
        double peak = (p.bioavailability * doseMg) / (p.volumeOfDistribution * weightKg);
        double effect = p.emax * peak / (p.ec50 + peak);           // Emax model
        double tox = peak > p.toxicThreshold
                ? Math.min(1.0, (peak - p.toxicThreshold) / p.toxicThreshold)
                : 0.0;
        double benefit = Math.max(0.0, Math.min(1.0, effect / v));
        double risk = Math.max(0.0, Math.min(1.0, tox * v));
        return new Forecast(peak, effect, tox, benefit, risk);
    }
}
