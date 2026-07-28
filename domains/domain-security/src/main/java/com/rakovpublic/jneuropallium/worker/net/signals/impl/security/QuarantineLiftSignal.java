/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.security;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/**
 * Automatic lift of a quarantine when its duration expires or a
 * reconfirmation window closes without new evidence.
 * ProcessingFrequency: loop=1, epoch=1.
 */
public class QuarantineLiftSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(1L, 1);

    private String entityId;
    private String reason;

    public QuarantineLiftSignal() { super(); this.loop = 1; this.epoch = 1L; this.timeAlive = 50; }

    public QuarantineLiftSignal(String entityId, String reason) {
        this();
        this.entityId = entityId;
        this.reason = reason;
    }

    public String getEntityId() { return entityId; }
    public void setEntityId(String e) { this.entityId = e; }
    public String getReason() { return reason; }
    public void setReason(String r) { this.reason = r; }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return QuarantineLiftSignal.class; }
    @Override public String getDescription() { return "QuarantineLiftSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        QuarantineLiftSignal c = new QuarantineLiftSignal(entityId, reason);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
