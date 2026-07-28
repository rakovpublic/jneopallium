/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical;

import com.rakovpublic.jneuropallium.ai.enums.HarmVerdict;
import com.rakovpublic.jneuropallium.ai.model.AlternativeAction;
import com.rakovpublic.jneuropallium.ai.signals.fast.HarmVetoSignal;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Clinical specialisation of {@link HarmVetoSignal}: carries a guideline
 * citation so that every veto remains auditable. Intentionally extends the
 * existing harm-veto type so that all safety vetoes flow through a single
 * audit path (precautionary principle).
 * ProcessingFrequency: loop=1, epoch=1.
 */
public class ClinicalVetoSignal extends HarmVetoSignal {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(1L, 1);

    private String guidelineCitation;
    private List<String> alternativeCodes;
    private String patientId;

    public ClinicalVetoSignal() {
        super();
        this.alternativeCodes = new ArrayList<>();
    }

    public ClinicalVetoSignal(String actionPlanId, String vetoReason, HarmVerdict severity,
                              String guidelineCitation, List<String> alternativeCodes, String patientId) {
        super(actionPlanId, vetoReason, severity, null);
        this.guidelineCitation = guidelineCitation;
        this.alternativeCodes = alternativeCodes == null
                ? new ArrayList<>() : new ArrayList<>(alternativeCodes);
        this.patientId = patientId;
        if (!this.alternativeCodes.isEmpty()) {
            AlternativeAction[] aa = new AlternativeAction[this.alternativeCodes.size()];
            for (int i = 0; i < aa.length; i++) {
                aa[i] = new AlternativeAction(this.alternativeCodes.get(i), this.alternativeCodes.get(i), 0.0);
            }
            setSuggestions(aa);
        }
    }

    public String getGuidelineCitation() { return guidelineCitation; }
    public void setGuidelineCitation(String c) { this.guidelineCitation = c; }
    public List<String> getAlternativeCodes() { return Collections.unmodifiableList(alternativeCodes); }
    public void setAlternativeCodes(List<String> a) {
        this.alternativeCodes = a == null ? new ArrayList<>() : new ArrayList<>(a);
    }
    public String getPatientId() { return patientId; }
    public void setPatientId(String p) { this.patientId = p; }

    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return ClinicalVetoSignal.class; }
    @Override public String getDescription() { return "ClinicalVetoSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        ClinicalVetoSignal c = new ClinicalVetoSignal(getActionPlanId(), getVetoReason(), getSeverity(),
                guidelineCitation, alternativeCodes, patientId);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
