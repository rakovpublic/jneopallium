/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.security;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Normalised anomaly score for a given entity (user / host / service)
 * plus the features that contributed to the deviation.
 * ProcessingFrequency: loop=1, epoch=2.
 */
public class AnomalyScoreSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(2L, 1);

    private String entityId;
    private double deviationScore;
    private List<String> contributingFeatures;

    public AnomalyScoreSignal() {
        super();
        this.loop = 1;
        this.epoch = 2L;
        this.timeAlive = 200;
        this.contributingFeatures = new ArrayList<>();
    }

    public AnomalyScoreSignal(String entityId, double deviationScore, List<String> contributingFeatures) {
        this();
        this.entityId = entityId;
        this.deviationScore = Math.max(0.0, Math.min(1.0, deviationScore));
        this.contributingFeatures = contributingFeatures == null
                ? new ArrayList<>() : new ArrayList<>(contributingFeatures);
    }

    public String getEntityId() { return entityId; }
    public void setEntityId(String e) { this.entityId = e; }
    public double getDeviationScore() { return deviationScore; }
    public void setDeviationScore(double d) { this.deviationScore = Math.max(0.0, Math.min(1.0, d)); }
    public List<String> getContributingFeatures() { return Collections.unmodifiableList(contributingFeatures); }
    public void setContributingFeatures(List<String> f) {
        this.contributingFeatures = f == null ? new ArrayList<>() : new ArrayList<>(f);
    }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return AnomalyScoreSignal.class; }
    @Override public String getDescription() { return "AnomalyScoreSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        AnomalyScoreSignal c = new AnomalyScoreSignal(entityId, deviationScore, contributingFeatures);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
