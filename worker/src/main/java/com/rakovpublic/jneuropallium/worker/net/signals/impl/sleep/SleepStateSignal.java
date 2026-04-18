/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.sleep;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.sleep.SleepPhase;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/**
 * Broadcast indicating the current {@link SleepPhase} and sleep depth.
 * Gates fast-loop activity while active.
 * <p>Biological analogue: brainstem / thalamic cortical-arousal control
 * systems (Saper et al. 2005).
 * <p>ProcessingFrequency: loop=2, epoch=10.
 */
public class SleepStateSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(10L, 2);

    private SleepPhase phase;
    private double depth;

    public SleepStateSignal() {
        super();
        this.loop = 2;
        this.epoch = 10L;
        this.timeAlive = 100;
        this.phase = SleepPhase.WAKE;
    }

    public SleepStateSignal(SleepPhase phase, double depth) {
        this();
        this.phase = phase == null ? SleepPhase.WAKE : phase;
        this.depth = clamp01(depth);
    }

    private static double clamp01(double v) { return Math.max(0.0, Math.min(1.0, v)); }

    public SleepPhase getPhase() { return phase; }
    public void setPhase(SleepPhase phase) { this.phase = phase == null ? SleepPhase.WAKE : phase; }

    public double getDepth() { return depth; }
    public void setDepth(double depth) { this.depth = clamp01(depth); }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return SleepStateSignal.class; }
    @Override public String getDescription() { return "SleepStateSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        SleepStateSignal c = new SleepStateSignal(phase, depth);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
