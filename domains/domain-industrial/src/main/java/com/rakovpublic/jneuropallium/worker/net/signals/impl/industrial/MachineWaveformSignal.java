/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.Arrays;

/**
 * Raw high-frequency frame from an industrial asset. It is intentionally
 * advisory-only input: feature neurons compress it before any slow health
 * correlation sees it. ProcessingFrequency: loop=1, epoch=1.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MachineWaveformSignal extends AbstractSignal<Void> implements ISignal<Void>, IInputSignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(1L, 1);

    public static final String CHANNEL_ACOUSTIC = "ACOUSTIC";
    public static final String CHANNEL_VIBRATION = "VIBRATION";
    public static final String CHANNEL_MOTOR_CURRENT = "MOTOR_CURRENT";

    private String assetId;
    private String channel;
    private double[] samples;
    private double sampleRateHz;
    private double rotationalSpeedRpm;
    private long timestamp;

    public MachineWaveformSignal() {
        super();
        this.loop = 1;
        this.epoch = 1L;
        this.timeAlive = 0;
        this.samples = new double[0];
    }

    public MachineWaveformSignal(String assetId, String channel, double[] samples,
                                 double sampleRateHz, double rotationalSpeedRpm, long timestamp) {
        this();
        this.assetId = assetId;
        this.channel = channel;
        this.samples = samples == null ? new double[0] : Arrays.copyOf(samples, samples.length);
        this.sampleRateHz = Math.max(0.0, sampleRateHz);
        this.rotationalSpeedRpm = Math.max(0.0, rotationalSpeedRpm);
        this.timestamp = timestamp;
    }

    public String getAssetId() { return assetId; }
    public void setAssetId(String assetId) { this.assetId = assetId; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public double[] getSamples() { return Arrays.copyOf(samples, samples.length); }
    public void setSamples(double[] samples) {
        this.samples = samples == null ? new double[0] : Arrays.copyOf(samples, samples.length);
    }
    public double getSampleRateHz() { return sampleRateHz; }
    public void setSampleRateHz(double sampleRateHz) { this.sampleRateHz = Math.max(0.0, sampleRateHz); }
    public double getRotationalSpeedRpm() { return rotationalSpeedRpm; }
    public void setRotationalSpeedRpm(double rotationalSpeedRpm) {
        this.rotationalSpeedRpm = Math.max(0.0, rotationalSpeedRpm);
    }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return MachineWaveformSignal.class; }
    @Override public String getDescription() { return "MachineWaveformSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        MachineWaveformSignal c = new MachineWaveformSignal(assetId, channel, samples,
                sampleRateHz, rotationalSpeedRpm, timestamp);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
