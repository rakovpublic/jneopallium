/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.tutoring;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.tutoring.InterventionType;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/**
 * Higher-level pedagogical intervention (take a break, encourage, redirect,
 * escalate to a human).
 * ProcessingFrequency: loop=2, epoch=1.
 */
public class InterventionSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(1L, 2);

    private InterventionType type;
    private String reason;

    public InterventionSignal() {
        super();
        this.loop = 2;
        this.epoch = 1L;
        this.timeAlive = 100;
        this.type = InterventionType.ENCOURAGE;
    }

    public InterventionSignal(InterventionType type, String reason) {
        this();
        this.type = type == null ? InterventionType.ENCOURAGE : type;
        this.reason = reason;
    }

    public InterventionType getType() { return type; }
    public void setType(InterventionType t) { this.type = t == null ? InterventionType.ENCOURAGE : t; }
    public String getReason() { return reason; }
    public void setReason(String r) { this.reason = r; }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return InterventionSignal.class; }
    @Override public String getDescription() { return "InterventionSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        InterventionSignal c = new InterventionSignal(type, reason);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
