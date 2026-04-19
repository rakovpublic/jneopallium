/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.bci;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/**
 * Local field potential band powers for one channel. {@code bandPowers} is
 * expected in order delta, theta, alpha, beta, low-gamma, high-gamma.
 * ProcessingFrequency: loop=1, epoch=1.
 */
public class LFPSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(1L, 1);

    public static final int DELTA = 0, THETA = 1, ALPHA = 2, BETA = 3, LOW_GAMMA = 4, HIGH_GAMMA = 5;

    private int channelId;
    private double[] bandPowers;
    private long timestampNs;

    public LFPSignal() {
        super();
        this.loop = 1;
        this.epoch = 1L;
        this.timeAlive = 5;
    }

    public LFPSignal(int channelId, double[] bandPowers, long timestampNs) {
        this();
        this.channelId = channelId;
        this.bandPowers = bandPowers;
        this.timestampNs = timestampNs;
    }

    public int getChannelId() { return channelId; }
    public void setChannelId(int c) { this.channelId = c; }
    public double[] getBandPowers() { return bandPowers; }
    public void setBandPowers(double[] b) { this.bandPowers = b; }
    public long getTimestampNs() { return timestampNs; }
    public void setTimestampNs(long t) { this.timestampNs = t; }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return LFPSignal.class; }
    @Override public String getDescription() { return "LFPSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        double[] b = bandPowers == null ? null : bandPowers.clone();
        LFPSignal c = new LFPSignal(channelId, b, timestampNs);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
