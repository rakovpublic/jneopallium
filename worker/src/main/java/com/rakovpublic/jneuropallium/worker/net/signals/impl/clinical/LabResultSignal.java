/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/**
 * A single lab analyte result (LOINC coded) with its reference range.
 * ProcessingFrequency: loop=2, epoch=3.
 */
public class LabResultSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(3L, 2);

    private String analyteCode;       // LOINC
    private double value;
    private String units;
    private double[] referenceRange;  // [low, high]
    private long resultedAt;
    private String patientId;

    public LabResultSignal() {
        super();
        this.loop = 2;
        this.epoch = 3L;
        this.timeAlive = 500;
    }

    public LabResultSignal(String analyteCode, double value, String units,
                           double[] referenceRange, long resultedAt, String patientId) {
        this();
        this.analyteCode = analyteCode;
        this.value = value;
        this.units = units;
        this.referenceRange = referenceRange;
        this.resultedAt = resultedAt;
        this.patientId = patientId;
    }

    public String getAnalyteCode() { return analyteCode; }
    public void setAnalyteCode(String c) { this.analyteCode = c; }
    public double getMeasurement() { return value; }
    public void setMeasurement(double v) { this.value = v; }
    public String getUnits() { return units; }
    public void setUnits(String u) { this.units = u; }
    public double[] getReferenceRange() { return referenceRange; }
    public void setReferenceRange(double[] r) { this.referenceRange = r; }
    public long getResultedAt() { return resultedAt; }
    public void setResultedAt(long t) { this.resultedAt = t; }
    public String getPatientId() { return patientId; }
    public void setPatientId(String p) { this.patientId = p; }

    public boolean isAbnormal() {
        if (referenceRange == null || referenceRange.length < 2) return false;
        return value < referenceRange[0] || value > referenceRange[1];
    }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return LabResultSignal.class; }
    @Override public String getDescription() { return "LabResultSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        double[] rr = referenceRange == null ? null : referenceRange.clone();
        LabResultSignal c = new LabResultSignal(analyteCode, value, units, rr, resultedAt, patientId);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
