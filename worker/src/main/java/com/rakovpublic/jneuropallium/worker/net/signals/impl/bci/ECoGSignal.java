/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.bci;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/**
 * Electrocorticography sample from a subdural or epidural contact.
 * ProcessingFrequency: loop=1, epoch=1.
 */
public class ECoGSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(1L, 1);

    private int electrodeId;
    private double voltage;
    private long timestampNs;

    public ECoGSignal() {
        super();
        this.loop = 1;
        this.epoch = 1L;
        this.timeAlive = 5;
    }

    public ECoGSignal(int electrodeId, double voltage, long timestampNs) {
        this();
        this.electrodeId = electrodeId;
        this.voltage = voltage;
        this.timestampNs = timestampNs;
    }

    public int getElectrodeId() { return electrodeId; }
    public void setElectrodeId(int e) { this.electrodeId = e; }
    public double getVoltage() { return voltage; }
    public void setVoltage(double v) { this.voltage = v; }
    public long getTimestampNs() { return timestampNs; }
    public void setTimestampNs(long t) { this.timestampNs = t; }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return ECoGSignal.class; }
    @Override public String getDescription() { return "ECoGSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        ECoGSignal c = new ECoGSignal(electrodeId, voltage, timestampNs);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
