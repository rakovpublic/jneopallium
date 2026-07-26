/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.glia;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/**
 * Astrocytic calcium wave — a slow broadcast of local activity integration
 * over a neural region. Amplitude and propagation radius come from the
 * integrating astrocyte.
 * <p>Biological analogue: astrocytic Ca²⁺ waves (Volterra &amp; Meldolesi 2005).
 * <p>ProcessingFrequency: loop=2, epoch=1.
 */
public class CalciumWaveSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(1L, 2);

    private int regionId;
    private double amplitude;
    private double propagationRadius;

    public CalciumWaveSignal() {
        super();
        this.loop = 2;
        this.epoch = 1L;
        this.timeAlive = 100;
    }

    public CalciumWaveSignal(int regionId, double amplitude, double propagationRadius) {
        this();
        this.regionId = regionId;
        this.amplitude = Math.max(0.0, amplitude);
        this.propagationRadius = Math.max(0.0, propagationRadius);
    }

    public int getRegionId() { return regionId; }
    public void setRegionId(int regionId) { this.regionId = regionId; }

    public double getAmplitude() { return amplitude; }
    public void setAmplitude(double amplitude) { this.amplitude = Math.max(0.0, amplitude); }

    public double getPropagationRadius() { return propagationRadius; }
    public void setPropagationRadius(double propagationRadius) { this.propagationRadius = Math.max(0.0, propagationRadius); }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return CalciumWaveSignal.class; }
    @Override public String getDescription() { return "CalciumWaveSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        CalciumWaveSignal c = new CalciumWaveSignal(regionId, amplitude, propagationRadius);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
