/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.security;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.security.AlertLevel;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/**
 * Network-wide "inflammation" broadcast. Biological analogue: cytokine
 * release. Instructs nearby neurons (typically the baseline learner) to
 * suspend adaptation during an active-attack window.
 * ProcessingFrequency: loop=2, epoch=1.
 */
public class InflammationBroadcastSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(1L, 2);

    private AlertLevel level;
    private String region;
    private double magnitude;

    public InflammationBroadcastSignal() {
        super();
        this.loop = 2;
        this.epoch = 1L;
        this.timeAlive = 300;
        this.level = AlertLevel.CLEAR;
    }

    public InflammationBroadcastSignal(AlertLevel level, String region, double magnitude) {
        this();
        this.level = level == null ? AlertLevel.CLEAR : level;
        this.region = region;
        this.magnitude = Math.max(0.0, Math.min(1.0, magnitude));
    }

    public AlertLevel getLevel() { return level; }
    public void setLevel(AlertLevel l) { this.level = l == null ? AlertLevel.CLEAR : l; }
    public String getRegion() { return region; }
    public void setRegion(String r) { this.region = r; }
    public double getMagnitude() { return magnitude; }
    public void setMagnitude(double m) { this.magnitude = Math.max(0.0, Math.min(1.0, m)); }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return InflammationBroadcastSignal.class; }
    @Override public String getDescription() { return "InflammationBroadcastSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        InflammationBroadcastSignal c = new InflammationBroadcastSignal(level, region, magnitude);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
