/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/**
 * Setpoint update for a control loop with an optional ramp rate.
 * ProcessingFrequency: loop=1, epoch=2.
 */
public class SetpointSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(2L, 1);

    private String tag;
    private double setpoint;
    private double rampRate;
    private String source;

    public SetpointSignal() { super(); this.loop = 1; this.epoch = 2L; this.timeAlive = 100; }

    public SetpointSignal(String tag, double setpoint, double rampRate, String source) {
        this();
        this.tag = tag;
        this.setpoint = setpoint;
        this.rampRate = rampRate;
        this.source = source;
    }

    public String getTag() { return tag; }
    public void setTag(String t) { this.tag = t; }
    public double getSetpoint() { return setpoint; }
    public void setSetpoint(double v) { this.setpoint = v; }
    public double getRampRate() { return rampRate; }
    public void setRampRate(double r) { this.rampRate = r; }
    public String getSource() { return source; }
    public void setSource(String s) { this.source = s; }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return SetpointSignal.class; }
    @Override public String getDescription() { return "SetpointSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        SetpointSignal c = new SetpointSignal(tag, setpoint, rampRate, source);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
