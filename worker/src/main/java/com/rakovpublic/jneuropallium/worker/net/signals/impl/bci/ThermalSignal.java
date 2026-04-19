/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.bci;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/**
 * Implant temperature sample. Sustained increase &gt; 1 °C triggers cool-down;
 * &gt; 2 °C triggers shutdown.
 * ProcessingFrequency: loop=2, epoch=1.
 */
public class ThermalSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(1L, 2);

    private int sensorId;
    private double temperatureC;
    private double deltaFromBaseline;

    public ThermalSignal() {
        super();
        this.loop = 2;
        this.epoch = 1L;
        this.timeAlive = 100;
    }

    public ThermalSignal(int sensorId, double temperatureC, double deltaFromBaseline) {
        this();
        this.sensorId = sensorId;
        this.temperatureC = temperatureC;
        this.deltaFromBaseline = deltaFromBaseline;
    }

    public int getSensorId() { return sensorId; }
    public void setSensorId(int s) { this.sensorId = s; }
    public double getTemperatureC() { return temperatureC; }
    public void setTemperatureC(double t) { this.temperatureC = t; }
    public double getDeltaFromBaseline() { return deltaFromBaseline; }
    public void setDeltaFromBaseline(double d) { this.deltaFromBaseline = d; }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return ThermalSignal.class; }
    @Override public String getDescription() { return "ThermalSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        ThermalSignal c = new ThermalSignal(sensorId, temperatureC, deltaFromBaseline);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
