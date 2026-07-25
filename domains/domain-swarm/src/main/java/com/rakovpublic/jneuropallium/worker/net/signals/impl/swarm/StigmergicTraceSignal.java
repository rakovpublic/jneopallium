/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.swarm.TraceKind;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/**
 * Persistent-on-environment marker for hardware-deposit-capable agents.
 * ProcessingFrequency: loop=2, epoch=5.
 */
public class StigmergicTraceSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(5L, 2);

    private double[] locationGlobal;
    private TraceKind kind;
    private double saliency;
    private long tickDeposited;

    public StigmergicTraceSignal() {
        super();
        this.loop = 2;
        this.epoch = 5L;
        this.timeAlive = 5000;
        this.kind = TraceKind.VISITED;
    }

    public StigmergicTraceSignal(double[] locationGlobal, TraceKind kind, double saliency, long tickDeposited) {
        this();
        this.locationGlobal = locationGlobal == null ? null : locationGlobal.clone();
        this.kind = kind == null ? TraceKind.VISITED : kind;
        this.saliency = Math.max(0.0, Math.min(1.0, saliency));
        this.tickDeposited = tickDeposited;
    }

    public double[] getLocationGlobal() { return locationGlobal == null ? null : locationGlobal.clone(); }
    public void setLocationGlobal(double[] v) { this.locationGlobal = v == null ? null : v.clone(); }
    public TraceKind getKind() { return kind; }
    public void setKind(TraceKind k) { this.kind = k == null ? TraceKind.VISITED : k; }
    public double getSaliency() { return saliency; }
    public void setSaliency(double s) { this.saliency = Math.max(0.0, Math.min(1.0, s)); }
    public long getTickDeposited() { return tickDeposited; }
    public void setTickDeposited(long t) { this.tickDeposited = t; }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return StigmergicTraceSignal.class; }
    @Override public String getDescription() { return "StigmergicTraceSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        StigmergicTraceSignal c = new StigmergicTraceSignal(locationGlobal, kind, saliency, tickDeposited);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
