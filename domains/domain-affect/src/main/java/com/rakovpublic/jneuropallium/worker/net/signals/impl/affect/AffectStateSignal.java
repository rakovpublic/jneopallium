/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.affect;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/**
 * Broadcast summary of the agent's affective state.
 * Biological analogue: limbic output (amygdala + anterior insula integration)
 * modulating cortical and subcortical targets.
 * <p>ProcessingFrequency: loop=2 (slow), epoch=1.
 */
public class AffectStateSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(1L, 2);

    private double valence;
    private double arousal;
    private String contextId;

    public AffectStateSignal() {
        super();
        this.loop = 2;
        this.epoch = 1L;
        this.timeAlive = 100;
    }

    public AffectStateSignal(double valence, double arousal, String contextId) {
        this();
        this.valence = clamp(valence, -1.0, 1.0);
        this.arousal = clamp(arousal, 0.0, 1.0);
        this.contextId = contextId;
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    public double getValence() { return valence; }
    public void setValence(double valence) { this.valence = clamp(valence, -1.0, 1.0); }

    public double getArousal() { return arousal; }
    public void setArousal(double arousal) { this.arousal = clamp(arousal, 0.0, 1.0); }

    public String getContextId() { return contextId; }
    public void setContextId(String contextId) { this.contextId = contextId; }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return AffectStateSignal.class; }
    @Override public String getDescription() { return "AffectStateSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        AffectStateSignal c = new AffectStateSignal(valence, arousal, contextId);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
