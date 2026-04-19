/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.tutoring;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/**
 * Emitted when a concept's BKT mastery estimate is updated.
 * ProcessingFrequency: loop=2, epoch=3.
 */
public class MasteryUpdateSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(3L, 2);

    private String conceptId;
    private double newMasteryLevel;

    public MasteryUpdateSignal() {
        super();
        this.loop = 2;
        this.epoch = 3L;
        this.timeAlive = 100;
    }

    public MasteryUpdateSignal(String conceptId, double newMasteryLevel) {
        this();
        this.conceptId = conceptId;
        this.newMasteryLevel = clamp(newMasteryLevel);
    }

    private static double clamp(double v) { return Math.max(0.0, Math.min(1.0, v)); }

    public String getConceptId() { return conceptId; }
    public void setConceptId(String c) { this.conceptId = c; }
    public double getNewMasteryLevel() { return newMasteryLevel; }
    public void setNewMasteryLevel(double m) { this.newMasteryLevel = clamp(m); }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return MasteryUpdateSignal.class; }
    @Override public String getDescription() { return "MasteryUpdateSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        MasteryUpdateSignal c = new MasteryUpdateSignal(conceptId, newMasteryLevel);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
