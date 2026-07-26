/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.bci;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/**
 * Per-channel electrode drift and signal-to-noise estimate. Feeds channel
 * exclusion and calibration scheduling.
 * ProcessingFrequency: loop=2, epoch=5.
 */
public class DriftEstimateSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(5L, 2);

    private int channelId;
    private double drift;
    private double snr;

    public DriftEstimateSignal() {
        super();
        this.loop = 2;
        this.epoch = 5L;
        this.timeAlive = 500;
    }

    public DriftEstimateSignal(int channelId, double drift, double snr) {
        this();
        this.channelId = channelId;
        this.drift = drift;
        this.snr = snr;
    }

    public int getChannelId() { return channelId; }
    public void setChannelId(int c) { this.channelId = c; }
    public double getDrift() { return drift; }
    public void setDrift(double d) { this.drift = d; }
    public double getSnr() { return snr; }
    public void setSnr(double s) { this.snr = s; }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return DriftEstimateSignal.class; }
    @Override public String getDescription() { return "DriftEstimateSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        DriftEstimateSignal c = new DriftEstimateSignal(channelId, drift, snr);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
