/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.bci;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci.SeizureMarker;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/**
 * Estimate of pre-ictal / ictal risk, derived from LFP or ECoG biomarkers.
 * Risk &gt; 0.8 triggers stimulation lockout.
 * ProcessingFrequency: loop=1, epoch=1.
 */
public class SeizureRiskSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(1L, 1);

    private double risk;
    private SeizureMarker marker;
    private int region;

    public SeizureRiskSignal() {
        super();
        this.loop = 1;
        this.epoch = 1L;
        this.timeAlive = 50;
        this.marker = SeizureMarker.NONE;
    }

    public SeizureRiskSignal(double risk, SeizureMarker marker, int region) {
        this();
        this.risk = Math.max(0.0, Math.min(1.0, risk));
        this.marker = marker == null ? SeizureMarker.NONE : marker;
        this.region = region;
    }

    public double getRisk() { return risk; }
    public void setRisk(double r) { this.risk = Math.max(0.0, Math.min(1.0, r)); }
    public SeizureMarker getMarker() { return marker; }
    public void setMarker(SeizureMarker m) { this.marker = m == null ? SeizureMarker.NONE : m; }
    public int getRegion() { return region; }
    public void setRegion(int r) { this.region = r; }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return SeizureRiskSignal.class; }
    @Override public String getDescription() { return "SeizureRiskSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        SeizureRiskSignal c = new SeizureRiskSignal(risk, marker, region);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
