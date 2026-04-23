/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A candidate diagnosis with posterior probability and supporting evidence
 * identifiers. Used by the differential-diagnosis neuron.
 * ProcessingFrequency: loop=1, epoch=2.
 */
public class DiagnosisHypothesisSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(2L, 1);

    private String icd10;
    private double posteriorProbability;
    private List<String> supportingEvidenceIds;
    private String patientId;

    public DiagnosisHypothesisSignal() {
        super();
        this.loop = 1;
        this.epoch = 2L;
        this.timeAlive = 200;
        this.supportingEvidenceIds = new ArrayList<>();
    }

    public DiagnosisHypothesisSignal(String icd10, double posteriorProbability,
                                     List<String> supportingEvidenceIds, String patientId) {
        this();
        this.icd10 = icd10;
        this.posteriorProbability = Math.max(0.0, Math.min(1.0, posteriorProbability));
        this.supportingEvidenceIds = supportingEvidenceIds == null
                ? new ArrayList<>() : new ArrayList<>(supportingEvidenceIds);
        this.patientId = patientId;
    }

    public String getIcd10() { return icd10; }
    public void setIcd10(String c) { this.icd10 = c; }
    public double getPosteriorProbability() { return posteriorProbability; }
    public void setPosteriorProbability(double p) { this.posteriorProbability = Math.max(0.0, Math.min(1.0, p)); }
    public List<String> getSupportingEvidenceIds() { return Collections.unmodifiableList(supportingEvidenceIds); }
    public void setSupportingEvidenceIds(List<String> s) {
        this.supportingEvidenceIds = s == null ? new ArrayList<>() : new ArrayList<>(s);
    }
    public String getPatientId() { return patientId; }
    public void setPatientId(String p) { this.patientId = p; }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return DiagnosisHypothesisSignal.class; }
    @Override public String getDescription() { return "DiagnosisHypothesisSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        DiagnosisHypothesisSignal c = new DiagnosisHypothesisSignal(icd10, posteriorProbability, supportingEvidenceIds, patientId);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
