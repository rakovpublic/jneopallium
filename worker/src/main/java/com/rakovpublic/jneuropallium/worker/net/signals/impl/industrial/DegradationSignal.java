/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/**
 * Remaining-useful-life estimate for an asset (fatigue, corrosion,
 * catalyst age). ProcessingFrequency: loop=2, epoch=3.
 */
public class DegradationSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(3L, 2);

    private String assetId;
    private double remainingUsefulLifeHours;
    private double confidence;

    public DegradationSignal() { super(); this.loop = 2; this.epoch = 3L; this.timeAlive = 10_000; }

    public DegradationSignal(String assetId, double remainingUsefulLifeHours, double confidence) {
        this();
        this.assetId = assetId;
        this.remainingUsefulLifeHours = Math.max(0.0, remainingUsefulLifeHours);
        this.confidence = Math.max(0.0, Math.min(1.0, confidence));
    }

    public String getAssetId() { return assetId; }
    public void setAssetId(String a) { this.assetId = a; }
    public double getRemainingUsefulLifeHours() { return remainingUsefulLifeHours; }
    public void setRemainingUsefulLifeHours(double r) { this.remainingUsefulLifeHours = Math.max(0.0, r); }
    public double getConfidence() { return confidence; }
    public void setConfidence(double c) { this.confidence = Math.max(0.0, Math.min(1.0, c)); }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return DegradationSignal.class; }
    @Override public String getDescription() { return "DegradationSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        DegradationSignal c = new DegradationSignal(assetId, remainingUsefulLifeHours, confidence);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
