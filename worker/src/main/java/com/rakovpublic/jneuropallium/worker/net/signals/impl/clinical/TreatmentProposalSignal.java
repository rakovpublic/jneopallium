/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/**
 * An advisory treatment proposal. NEVER emitted with execute=true; the
 * physician is the sole executor. Carries benefit / risk estimates and a
 * short human-readable rationale.
 * ProcessingFrequency: loop=1, epoch=3.
 */
public class TreatmentProposalSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(3L, 1);

    private String rxNormOrProcedureCode;
    private double expectedBenefit;
    private double expectedRisk;
    private String rationale;
    private String patientId;

    public TreatmentProposalSignal() {
        super();
        this.loop = 1;
        this.epoch = 3L;
        this.timeAlive = 500;
    }

    public TreatmentProposalSignal(String rxNormOrProcedureCode, double expectedBenefit,
                                   double expectedRisk, String rationale, String patientId) {
        this();
        this.rxNormOrProcedureCode = rxNormOrProcedureCode;
        this.expectedBenefit = Math.max(0.0, Math.min(1.0, expectedBenefit));
        this.expectedRisk = Math.max(0.0, Math.min(1.0, expectedRisk));
        this.rationale = rationale;
        this.patientId = patientId;
    }

    public String getRxNormOrProcedureCode() { return rxNormOrProcedureCode; }
    public void setRxNormOrProcedureCode(String c) { this.rxNormOrProcedureCode = c; }
    public double getExpectedBenefit() { return expectedBenefit; }
    public void setExpectedBenefit(double b) { this.expectedBenefit = Math.max(0.0, Math.min(1.0, b)); }
    public double getExpectedRisk() { return expectedRisk; }
    public void setExpectedRisk(double r) { this.expectedRisk = Math.max(0.0, Math.min(1.0, r)); }
    public String getRationale() { return rationale; }
    public void setRationale(String r) { this.rationale = r; }
    public String getPatientId() { return patientId; }
    public void setPatientId(String p) { this.patientId = p; }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return TreatmentProposalSignal.class; }
    @Override public String getDescription() { return "TreatmentProposalSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        TreatmentProposalSignal c = new TreatmentProposalSignal(rxNormOrProcedureCode,
                expectedBenefit, expectedRisk, rationale, patientId);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
