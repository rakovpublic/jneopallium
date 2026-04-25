/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.swarm.PheromoneKind;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/**
 * Spatial neuromodulator broadcast — biologically-validated stigmergy
 * categories. Per spec §6 this is the swarm's "spatial NeuromodulatorSignal".
 * ProcessingFrequency: loop=2, epoch=2.
 */
public class PheromoneSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(2L, 2);

    private PheromoneKind kind;
    private double[] locationGlobal;
    private double strength;
    private long decayTick;

    public PheromoneSignal() {
        super();
        this.loop = 2;
        this.epoch = 2L;
        this.timeAlive = 1000;
        this.kind = PheromoneKind.TRAIL;
    }

    public PheromoneSignal(PheromoneKind kind, double[] locationGlobal, double strength, long decayTick) {
        this();
        this.kind = kind == null ? PheromoneKind.TRAIL : kind;
        this.locationGlobal = locationGlobal == null ? null : locationGlobal.clone();
        this.strength = Math.max(0.0, strength);
        this.decayTick = decayTick;
    }

    public PheromoneKind getKind() { return kind; }
    public void setKind(PheromoneKind k) { this.kind = k == null ? PheromoneKind.TRAIL : k; }
    public double[] getLocationGlobal() { return locationGlobal == null ? null : locationGlobal.clone(); }
    public void setLocationGlobal(double[] v) { this.locationGlobal = v == null ? null : v.clone(); }
    public double getStrength() { return strength; }
    public void setStrength(double s) { this.strength = Math.max(0.0, s); }
    public long getDecayTick() { return decayTick; }
    public void setDecayTick(long d) { this.decayTick = d; }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return PheromoneSignal.class; }
    @Override public String getDescription() { return "PheromoneSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        PheromoneSignal c = new PheromoneSignal(kind, locationGlobal, strength, decayTick);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
