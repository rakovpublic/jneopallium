/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.curiosity;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/**
 * High familiarity / low learning progress marker. Drives inhibition-of-return
 * on over-familiar contexts to force exploration.
 * <p>ProcessingFrequency: loop=2, epoch=2.
 */
public class BoredomSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(2L, 2);

    private String contextHash;
    private double familiarity;

    public BoredomSignal() {
        super();
        this.loop = 2;
        this.epoch = 2L;
        this.timeAlive = 100;
    }

    public BoredomSignal(String contextHash, double familiarity) {
        this();
        this.contextHash = contextHash;
        this.familiarity = clamp01(familiarity);
    }

    private static double clamp01(double v) { return Math.max(0.0, Math.min(1.0, v)); }

    public String getContextHash() { return contextHash; }
    public void setContextHash(String contextHash) { this.contextHash = contextHash; }

    public double getFamiliarity() { return familiarity; }
    public void setFamiliarity(double familiarity) { this.familiarity = clamp01(familiarity); }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return BoredomSignal.class; }
    @Override public String getDescription() { return "BoredomSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        BoredomSignal c = new BoredomSignal(contextHash, familiarity);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
