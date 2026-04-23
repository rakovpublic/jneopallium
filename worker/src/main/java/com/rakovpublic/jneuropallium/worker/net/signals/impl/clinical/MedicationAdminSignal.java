/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/**
 * Record of an administered medication (for context, not orders).
 * ProcessingFrequency: loop=2, epoch=2.
 */
public class MedicationAdminSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(2L, 2);

    private String rxNormCode;
    private double dose;
    private String units;
    private String route;
    private long administeredAt;
    private String patientId;

    public MedicationAdminSignal() {
        super();
        this.loop = 2;
        this.epoch = 2L;
        this.timeAlive = 1000;
    }

    public MedicationAdminSignal(String rxNormCode, double dose, String units,
                                 String route, long administeredAt, String patientId) {
        this();
        this.rxNormCode = rxNormCode;
        this.dose = dose;
        this.units = units;
        this.route = route;
        this.administeredAt = administeredAt;
        this.patientId = patientId;
    }

    public String getRxNormCode() { return rxNormCode; }
    public void setRxNormCode(String c) { this.rxNormCode = c; }
    public double getDose() { return dose; }
    public void setDose(double d) { this.dose = d; }
    public String getUnits() { return units; }
    public void setUnits(String u) { this.units = u; }
    public String getRoute() { return route; }
    public void setRoute(String r) { this.route = r; }
    public long getAdministeredAt() { return administeredAt; }
    public void setAdministeredAt(long t) { this.administeredAt = t; }
    public String getPatientId() { return patientId; }
    public void setPatientId(String p) { this.patientId = p; }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return MedicationAdminSignal.class; }
    @Override public String getDescription() { return "MedicationAdminSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        MedicationAdminSignal c = new MedicationAdminSignal(rxNormCode, dose, units, route, administeredAt, patientId);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
