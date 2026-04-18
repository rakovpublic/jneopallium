/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.affect;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/**
 * Carries body-state telemetry into the affective subsystem.
 * Biological analogue: ascending interoceptive pathways (vagal, spinothalamic)
 * feeding the anterior insula.
 * <p>ProcessingFrequency: loop=1 (fast), epoch=2.
 */
public class InteroceptiveSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(2L, 1);

    private double energyBudget;
    private double homeostaticError;
    private double painMagnitude;
    private String source;

    public InteroceptiveSignal() {
        super();
        this.loop = 1;
        this.epoch = 2L;
        this.timeAlive = 100;
    }

    public InteroceptiveSignal(double energyBudget, double homeostaticError,
                               double painMagnitude, String source) {
        this();
        this.energyBudget = energyBudget;
        this.homeostaticError = homeostaticError;
        this.painMagnitude = painMagnitude;
        this.source = source;
    }

    public double getEnergyBudget() { return energyBudget; }
    public void setEnergyBudget(double energyBudget) { this.energyBudget = energyBudget; }

    public double getHomeostaticError() { return homeostaticError; }
    public void setHomeostaticError(double homeostaticError) { this.homeostaticError = homeostaticError; }

    public double getPainMagnitude() { return painMagnitude; }
    public void setPainMagnitude(double painMagnitude) { this.painMagnitude = painMagnitude; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return InteroceptiveSignal.class; }
    @Override public String getDescription() { return "InteroceptiveSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        InteroceptiveSignal c = new InteroceptiveSignal(energyBudget, homeostaticError, painMagnitude, source);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
