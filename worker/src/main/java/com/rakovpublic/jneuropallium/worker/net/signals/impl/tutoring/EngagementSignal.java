/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.tutoring;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.tutoring.EngagementSource;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/**
 * Fused attention/engagement estimate from a sensing channel.
 * ProcessingFrequency: loop=1, epoch=2.
 */
public class EngagementSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(2L, 1);

    private double attentionScore;
    private EngagementSource source;

    public EngagementSignal() {
        super();
        this.loop = 1;
        this.epoch = 2L;
        this.timeAlive = 100;
        this.source = EngagementSource.MULTI_MODAL;
    }

    public EngagementSignal(double attentionScore, EngagementSource source) {
        this();
        this.attentionScore = clamp(attentionScore);
        this.source = source == null ? EngagementSource.MULTI_MODAL : source;
    }

    private static double clamp(double v) { return Math.max(0.0, Math.min(1.0, v)); }

    public double getAttentionScore() { return attentionScore; }
    public void setAttentionScore(double a) { this.attentionScore = clamp(a); }
    public EngagementSource getSource() { return source; }
    public void setSource(EngagementSource s) { this.source = s == null ? EngagementSource.MULTI_MODAL : s; }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return EngagementSignal.class; }
    @Override public String getDescription() { return "EngagementSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        EngagementSignal c = new EngagementSignal(attentionScore, source);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
