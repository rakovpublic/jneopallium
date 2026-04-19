/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.tutoring;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/**
 * Coarse-grained affect observation inferred from behavioural patterns
 * (engagement trajectory + response accuracy). Feeds the affect module.
 * ProcessingFrequency: loop=1, epoch=2.
 */
public class AffectObservationSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(2L, 1);

    private double valence;
    private double arousal;
    private double confidence;

    public AffectObservationSignal() {
        super();
        this.loop = 1;
        this.epoch = 2L;
        this.timeAlive = 100;
    }

    public AffectObservationSignal(double valence, double arousal, double confidence) {
        this();
        this.valence = clamp(valence, -1.0, 1.0);
        this.arousal = clamp(arousal, 0.0, 1.0);
        this.confidence = clamp(confidence, 0.0, 1.0);
    }

    private static double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }

    public double getValence() { return valence; }
    public void setValence(double v) { this.valence = clamp(v, -1.0, 1.0); }
    public double getArousal() { return arousal; }
    public void setArousal(double a) { this.arousal = clamp(a, 0.0, 1.0); }
    public double getConfidence() { return confidence; }
    public void setConfidence(double c) { this.confidence = clamp(c, 0.0, 1.0); }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return AffectObservationSignal.class; }
    @Override public String getDescription() { return "AffectObservationSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        AffectObservationSignal c = new AffectObservationSignal(valence, arousal, confidence);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
