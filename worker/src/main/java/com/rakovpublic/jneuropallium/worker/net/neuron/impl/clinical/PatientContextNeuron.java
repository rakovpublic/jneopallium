/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.DemographicSignal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Layer 2 patient-context neuron. Specialises the autonomous-AI
 * {@code HarmContextNeuron} role by maintaining the current demographic /
 * comorbidity / allergy snapshot for the single patient this network
 * instance represents. Tightens harm thresholds for vulnerable populations
 * (pediatric, geriatric, pregnant, immunocompromised). Loop=2 / Epoch=5.
 */
public class PatientContextNeuron extends ModulatableNeuron implements IPatientContextNeuron {

    private String patientId;
    private int ageYears;
    private com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical.Sex sex =
            com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical.Sex.UNKNOWN;
    private final List<String> comorbidities = new ArrayList<>();
    private final List<String> allergies = new ArrayList<>();
    private boolean pregnant;
    private boolean immunocompromised;
    private double vulnerabilityFactor = 1.0;

    public PatientContextNeuron() { super(); }

    public PatientContextNeuron(Long neuronId, ISignalChain chain, Long run) {
        super(neuronId, chain, run);
    }

    public void update(DemographicSignal d) {
        if (d == null) return;
        this.patientId = d.getPatientId();
        this.ageYears = d.getAgeYears();
        this.sex = d.getSex();
        this.comorbidities.clear();
        this.comorbidities.addAll(d.getComorbidities());
        this.allergies.clear();
        this.allergies.addAll(d.getAllergies());
        this.immunocompromised = this.comorbidities.stream()
                .anyMatch(c -> c != null && (c.contains("IMMUNOCOMPROMISED") || c.startsWith("D80") || c.startsWith("D81") || c.startsWith("D82") || c.startsWith("D83") || c.startsWith("D84")));
        this.pregnant = this.comorbidities.stream()
                .anyMatch(c -> c != null && (c.contains("PREGNANT") || c.startsWith("Z34") || c.startsWith("O")));
        this.vulnerabilityFactor = computeVulnerability(d);
    }

    private double computeVulnerability(DemographicSignal d) {
        double f = 1.0;
        if (d.isPediatric()) f *= (d.getAgeYears() < 2 ? 2.0 : 1.5);
        else if (d.isGeriatric()) f *= (d.getAgeYears() >= 80 ? 1.75 : 1.3);
        if (pregnant) f *= 1.5;
        if (immunocompromised) f *= 1.4;
        if (f > 5.0) f = 5.0;
        return f;
    }

    public boolean hasAllergy(String code) {
        if (code == null) return false;
        for (String a : allergies) if (code.equalsIgnoreCase(a)) return true;
        return false;
    }

    public boolean hasComorbidity(String code) {
        if (code == null) return false;
        for (String c : comorbidities) if (code.equalsIgnoreCase(c)) return true;
        return false;
    }

    public String getPatientId() { return patientId; }
    public int getAgeYears() { return ageYears; }
    public com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical.Sex getSex() { return sex; }
    public List<String> getComorbidities() { return Collections.unmodifiableList(comorbidities); }
    public List<String> getAllergies() { return Collections.unmodifiableList(allergies); }
    public boolean isPregnant() { return pregnant; }
    public boolean isImmunocompromised() { return immunocompromised; }
    public double getVulnerabilityFactor() { return vulnerabilityFactor; }
}
