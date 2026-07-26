/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.TreatmentProposalSignal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Layer 4 treatment-planning neuron. Specialises the autonomous-AI
 * {@code PlanningNeuron}. For each candidate treatment code the planner
 * simulates a forward PK/PD forecast via {@link ClinicalConsequenceModelNeuron}
 * and packages the result as a {@link TreatmentProposalSignal} with a
 * short free-text rationale. Loop=1 / Epoch=3.
 *
 * <p><b>Non-autonomous:</b> this neuron never emits a {@code MotorCommandSignal}
 * nor any signal with {@code execute=true}. Execution belongs to the clinician.
 */
public class TreatmentPlanningNeuron extends ModulatableNeuron implements ITreatmentPlanningNeuron {

    /** Candidate treatment description used as planner input. */
    public static final class Candidate {
        public final String rxNormOrProcedureCode;
        public final double doseMg;
        public final ClinicalConsequenceModelNeuron.PkPdParams params;
        public final String icd10Target;

        public Candidate(String code, double doseMg,
                         ClinicalConsequenceModelNeuron.PkPdParams params, String icd10Target) {
            this.rxNormOrProcedureCode = code;
            this.doseMg = doseMg;
            this.params = params;
            this.icd10Target = icd10Target;
        }
    }

    private final IClinicalConsequenceModelNeuron consequenceModel;
    private double weightKg = 70.0;
    private double vulnerabilityFactor = 1.0;
    private String patientId;

    public TreatmentPlanningNeuron() {
        super();
        this.consequenceModel = new ClinicalConsequenceModelNeuron();
    }

    public TreatmentPlanningNeuron(Long neuronId, ISignalChain chain, Long run) {
        super(neuronId, chain, run);
        this.consequenceModel = new ClinicalConsequenceModelNeuron();
    }

    public TreatmentPlanningNeuron(IClinicalConsequenceModelNeuron model) {
        super();
        this.consequenceModel = model == null ? new ClinicalConsequenceModelNeuron() : model;
    }

    public void setWeightKg(double kg) { this.weightKg = Math.max(1.0, kg); }
    public double getWeightKg() { return weightKg; }
    public void setVulnerabilityFactor(double f) { this.vulnerabilityFactor = Math.max(1.0, f); }
    public double getVulnerabilityFactor() { return vulnerabilityFactor; }
    public void setPatientId(String p) { this.patientId = p; }
    public String getPatientId() { return patientId; }

    /**
     * Produce one {@link TreatmentProposalSignal} per candidate, sorted by
     * descending benefit-minus-risk. All proposals are advisory only.
     */
    public List<TreatmentProposalSignal> plan(List<Candidate> candidates) {
        List<TreatmentProposalSignal> out = new ArrayList<>();
        if (candidates == null) return out;
        for (Candidate c : candidates) {
            if (c == null) continue;
            ClinicalConsequenceModelNeuron.Forecast f =
                    consequenceModel.simulate(c.doseMg, weightKg, c.params, vulnerabilityFactor);
            String rationale = "PK/PD: peak=" + round(f.peakConcentration)
                    + " effect=" + round(f.effectAtPeak)
                    + " tox=" + round(f.toxicityRisk)
                    + " icd10=" + c.icd10Target
                    + " (advisory only; physician confirmation required)";
            out.add(new TreatmentProposalSignal(c.rxNormOrProcedureCode,
                    f.expectedBenefit, f.expectedRisk, rationale, patientId));
        }
        out.sort(Comparator.comparingDouble(
                (TreatmentProposalSignal p) -> p.getExpectedBenefit() - p.getExpectedRisk()).reversed());
        return out;
    }

    private static double round(double v) { return Math.round(v * 1000.0) / 1000.0; }
}
