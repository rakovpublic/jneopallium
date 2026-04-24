/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/**
 * Actuator command. {@code execute=false} means shadow-mode — the
 * signal is logged and compared but not written to the field.
 * ProcessingFrequency: loop=1, epoch=1.
 */
public class ActuatorCommandSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(1L, 1);

    private String tag;
    private double targetValue;
    private double currentValue;
    private boolean execute;

    public ActuatorCommandSignal() { super(); this.loop = 1; this.epoch = 1L; this.timeAlive = 30; }

    public ActuatorCommandSignal(String tag, double targetValue, double currentValue, boolean execute) {
        this();
        this.tag = tag;
        this.targetValue = targetValue;
        this.currentValue = currentValue;
        this.execute = execute;
    }

    public String getTag() { return tag; }
    public void setTag(String t) { this.tag = t; }
    public double getTargetValue() { return targetValue; }
    public void setTargetValue(double v) { this.targetValue = v; }
    public double getCurrentValue() { return currentValue; }
    public void setCurrentValue(double v) { this.currentValue = v; }
    public boolean isExecute() { return execute; }
    public void setExecute(boolean e) { this.execute = e; }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return ActuatorCommandSignal.class; }
    @Override public String getDescription() { return "ActuatorCommandSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        ActuatorCommandSignal c = new ActuatorCommandSignal(tag, targetValue, currentValue, execute);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
