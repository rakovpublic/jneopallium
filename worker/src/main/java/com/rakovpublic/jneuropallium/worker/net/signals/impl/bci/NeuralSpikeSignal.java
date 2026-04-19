/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.bci;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/**
 * A single sorted spike event from an intracortical electrode / Neuropixels
 * channel. Sub-millisecond timing is carried in {@code timestampNs}.
 * ProcessingFrequency: loop=1, epoch=1.
 */
public class NeuralSpikeSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(1L, 1);

    private int channelId;
    private int unitId;
    private double[] waveformSnippet;
    private long timestampNs;

    public NeuralSpikeSignal() {
        super();
        this.loop = 1;
        this.epoch = 1L;
        this.timeAlive = 2;
    }

    public NeuralSpikeSignal(int channelId, int unitId, double[] waveformSnippet, long timestampNs) {
        this();
        this.channelId = channelId;
        this.unitId = unitId;
        this.waveformSnippet = waveformSnippet;
        this.timestampNs = timestampNs;
    }

    public int getChannelId() { return channelId; }
    public void setChannelId(int channelId) { this.channelId = channelId; }
    public int getUnitId() { return unitId; }
    public void setUnitId(int unitId) { this.unitId = unitId; }
    public double[] getWaveformSnippet() { return waveformSnippet; }
    public void setWaveformSnippet(double[] w) { this.waveformSnippet = w; }
    public long getTimestampNs() { return timestampNs; }
    public void setTimestampNs(long t) { this.timestampNs = t; }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return NeuralSpikeSignal.class; }
    @Override public String getDescription() { return "NeuralSpikeSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        double[] w = waveformSnippet == null ? null : waveformSnippet.clone();
        NeuralSpikeSignal c = new NeuralSpikeSignal(channelId, unitId, w, timestampNs);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
