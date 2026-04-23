/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical.VitalType;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/**
 * A single vital-sign sample (HR, SpO₂, BP, TEMP, RESP).
 * ProcessingFrequency: loop=1, epoch=1.
 */
public class VitalSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(1L, 1);

    private VitalType type;
    private double value;
    private long timestamp;
    private String patientId;

    public VitalSignal() {
        super();
        this.loop = 1;
        this.epoch = 1L;
        this.timeAlive = 30;
        this.type = VitalType.HR;
    }

    public VitalSignal(VitalType type, double value, long timestamp, String patientId) {
        this();
        this.type = type == null ? VitalType.HR : type;
        this.value = value;
        this.timestamp = timestamp;
        this.patientId = patientId;
    }

    public VitalType getType() { return type; }
    public void setType(VitalType t) { this.type = t == null ? VitalType.HR : t; }
    public double getMeasurement() { return value; }
    public void setMeasurement(double v) { this.value = v; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long t) { this.timestamp = t; }
    public String getPatientId() { return patientId; }
    public void setPatientId(String p) { this.patientId = p; }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return VitalSignal.class; }
    @Override public String getDescription() { return "VitalSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        VitalSignal c = new VitalSignal(type, value, timestamp, patientId);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
