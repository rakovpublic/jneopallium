/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.bci;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci.FeedbackModality;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/**
 * Sensory-feedback stimulation targeted at a specific afferent fibre / nerve
 * contact. Delivered to restore tactile or proprioceptive perception.
 * ProcessingFrequency: loop=1, epoch=1.
 */
public class SensoryFeedbackSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(1L, 1);

    private FeedbackModality modality;
    private int afferentId;
    private double intensity;
    private double duration;

    public SensoryFeedbackSignal() {
        super();
        this.loop = 1;
        this.epoch = 1L;
        this.timeAlive = 5;
        this.modality = FeedbackModality.TACTILE;
    }

    public SensoryFeedbackSignal(FeedbackModality modality, int afferentId, double intensity, double duration) {
        this();
        this.modality = modality == null ? FeedbackModality.TACTILE : modality;
        this.afferentId = afferentId;
        this.intensity = Math.max(0.0, Math.min(1.0, intensity));
        this.duration = Math.max(0.0, duration);
    }

    public FeedbackModality getModality() { return modality; }
    public void setModality(FeedbackModality m) { this.modality = m == null ? FeedbackModality.TACTILE : m; }
    public int getAfferentId() { return afferentId; }
    public void setAfferentId(int a) { this.afferentId = a; }
    public double getIntensity() { return intensity; }
    public void setIntensity(double i) { this.intensity = Math.max(0.0, Math.min(1.0, i)); }
    public double getDuration() { return duration; }
    public void setDuration(double d) { this.duration = Math.max(0.0, d); }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return SensoryFeedbackSignal.class; }
    @Override public String getDescription() { return "SensoryFeedbackSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        SensoryFeedbackSignal c = new SensoryFeedbackSignal(modality, afferentId, intensity, duration);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
