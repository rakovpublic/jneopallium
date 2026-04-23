/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical.Sex;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Slow-changing patient demographics and history.
 * ProcessingFrequency: loop=2, epoch=10.
 */
public class DemographicSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(10L, 2);

    private int ageYears;
    private Sex sex;
    private List<String> comorbidities;
    private List<String> allergies;
    private String patientId;

    public DemographicSignal() {
        super();
        this.loop = 2;
        this.epoch = 10L;
        this.timeAlive = 100000;
        this.sex = Sex.UNKNOWN;
        this.comorbidities = new ArrayList<>();
        this.allergies = new ArrayList<>();
    }

    public DemographicSignal(int ageYears, Sex sex, List<String> comorbidities,
                             List<String> allergies, String patientId) {
        this();
        this.ageYears = Math.max(0, ageYears);
        this.sex = sex == null ? Sex.UNKNOWN : sex;
        this.comorbidities = comorbidities == null ? new ArrayList<>() : new ArrayList<>(comorbidities);
        this.allergies = allergies == null ? new ArrayList<>() : new ArrayList<>(allergies);
        this.patientId = patientId;
    }

    public int getAgeYears() { return ageYears; }
    public void setAgeYears(int a) { this.ageYears = Math.max(0, a); }
    public Sex getSex() { return sex; }
    public void setSex(Sex s) { this.sex = s == null ? Sex.UNKNOWN : s; }
    public List<String> getComorbidities() { return Collections.unmodifiableList(comorbidities); }
    public void setComorbidities(List<String> c) { this.comorbidities = c == null ? new ArrayList<>() : new ArrayList<>(c); }
    public List<String> getAllergies() { return Collections.unmodifiableList(allergies); }
    public void setAllergies(List<String> a) { this.allergies = a == null ? new ArrayList<>() : new ArrayList<>(a); }
    public String getPatientId() { return patientId; }
    public void setPatientId(String p) { this.patientId = p; }

    public boolean isPediatric() { return ageYears < 18; }
    public boolean isGeriatric() { return ageYears >= 65; }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return DemographicSignal.class; }
    @Override public String getDescription() { return "DemographicSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        DemographicSignal c = new DemographicSignal(ageYears, sex, comorbidities, allergies, patientId);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
