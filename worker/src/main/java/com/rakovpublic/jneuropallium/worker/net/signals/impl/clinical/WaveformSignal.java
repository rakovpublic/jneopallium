/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical.WaveformType;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/**
 * A continuous physiological waveform window (ECG / PPG / EEG).
 * ProcessingFrequency: loop=1, epoch=1.
 */
public class WaveformSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(1L, 1);

    private WaveformType type;
    private double[] samples;
    private double sampleRateHz;
    private String patientId;

    public WaveformSignal() {
        super();
        this.loop = 1;
        this.epoch = 1L;
        this.timeAlive = 30;
        this.type = WaveformType.ECG;
    }

    public WaveformSignal(WaveformType type, double[] samples, double sampleRateHz, String patientId) {
        this();
        this.type = type == null ? WaveformType.ECG : type;
        this.samples = samples;
        this.sampleRateHz = sampleRateHz;
        this.patientId = patientId;
    }

    public WaveformType getType() { return type; }
    public void setType(WaveformType t) { this.type = t == null ? WaveformType.ECG : t; }
    public double[] getSamples() { return samples; }
    public void setSamples(double[] s) { this.samples = s; }
    public double getSampleRateHz() { return sampleRateHz; }
    public void setSampleRateHz(double r) { this.sampleRateHz = r; }
    public String getPatientId() { return patientId; }
    public void setPatientId(String p) { this.patientId = p; }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return WaveformSignal.class; }
    @Override public String getDescription() { return "WaveformSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        double[] s = samples == null ? null : samples.clone();
        WaveformSignal c = new WaveformSignal(type, s, sampleRateHz, patientId);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
