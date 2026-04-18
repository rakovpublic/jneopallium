/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.sleep;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A recombined episode generated during REM sleep. Carries the bindings
 * it was composed from and a novelty score used to gate whether the
 * candidate plan is forwarded to {@code PlanningNeuron}.
 * <p>Biological analogue: REM-sleep dreaming and episode recombination
 * (Wamsley &amp; Stickgold 2011).
 * <p>ProcessingFrequency: loop=2, epoch=5.
 */
public class DreamSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(5L, 2);

    private List<Long> episodeBindings = new ArrayList<>();
    private double noveltyScore;

    public DreamSignal() {
        super();
        this.loop = 2;
        this.epoch = 5L;
        this.timeAlive = 100;
    }

    public DreamSignal(List<Long> episodeBindings, double noveltyScore) {
        this();
        this.episodeBindings = (episodeBindings == null) ? new ArrayList<>() : new ArrayList<>(episodeBindings);
        this.noveltyScore = clamp01(noveltyScore);
    }

    private static double clamp01(double v) { return Math.max(0.0, Math.min(1.0, v)); }

    public List<Long> getEpisodeBindings() { return Collections.unmodifiableList(episodeBindings); }
    public void setEpisodeBindings(List<Long> episodeBindings) {
        this.episodeBindings = (episodeBindings == null) ? new ArrayList<>() : new ArrayList<>(episodeBindings);
    }

    public double getNoveltyScore() { return noveltyScore; }
    public void setNoveltyScore(double noveltyScore) { this.noveltyScore = clamp01(noveltyScore); }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return DreamSignal.class; }
    @Override public String getDescription() { return "DreamSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        DreamSignal c = new DreamSignal(episodeBindings, noveltyScore);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
