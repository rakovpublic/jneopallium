/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.curiosity;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/**
 * Signals how novel a given context is.
 * Biological analogue: hippocampal novelty detection (CA1/dentate) that
 * gates dopaminergic bursts from VTA during new-environment exploration.
 * <p>ProcessingFrequency: loop=1, epoch=2.
 */
public class NoveltySignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(2L, 1);

    private double noveltyScore;
    private String contextHash;

    public NoveltySignal() {
        super();
        this.loop = 1;
        this.epoch = 2L;
        this.timeAlive = 100;
    }

    public NoveltySignal(double noveltyScore, String contextHash) {
        this();
        this.noveltyScore = clamp01(noveltyScore);
        this.contextHash = contextHash;
    }

    private static double clamp01(double v) { return Math.max(0.0, Math.min(1.0, v)); }

    public double getNoveltyScore() { return noveltyScore; }
    public void setNoveltyScore(double noveltyScore) { this.noveltyScore = clamp01(noveltyScore); }

    public String getContextHash() { return contextHash; }
    public void setContextHash(String contextHash) { this.contextHash = contextHash; }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return NoveltySignal.class; }
    @Override public String getDescription() { return "NoveltySignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        NoveltySignal c = new NoveltySignal(noveltyScore, contextHash);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
