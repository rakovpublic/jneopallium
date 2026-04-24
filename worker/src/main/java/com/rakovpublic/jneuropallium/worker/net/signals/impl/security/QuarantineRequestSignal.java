/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.security;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.security.EntityKind;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/**
 * Request to quarantine an entity for a bounded number of ticks. Per
 * spec §6 quarantine is <b>never permanent</b>; {@link #getDurationTicks()}
 * is always positive and {@link QuarantineLiftSignal} fires when it
 * expires.
 * ProcessingFrequency: loop=1, epoch=1.
 */
public class QuarantineRequestSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(1L, 1);

    private String entityId;
    private EntityKind kind;
    private int durationTicks;
    private String reason;

    public QuarantineRequestSignal() {
        super();
        this.loop = 1;
        this.epoch = 1L;
        this.timeAlive = 50;
        this.kind = EntityKind.CONNECTION;
        this.durationTicks = 1;
    }

    public QuarantineRequestSignal(String entityId, EntityKind kind, int durationTicks, String reason) {
        this();
        this.entityId = entityId;
        this.kind = kind == null ? EntityKind.CONNECTION : kind;
        this.durationTicks = Math.max(1, durationTicks);
        this.reason = reason;
    }

    public String getEntityId() { return entityId; }
    public void setEntityId(String e) { this.entityId = e; }
    public EntityKind getKind() { return kind; }
    public void setKind(EntityKind k) { this.kind = k == null ? EntityKind.CONNECTION : k; }
    public int getDurationTicks() { return durationTicks; }
    public void setDurationTicks(int d) { this.durationTicks = Math.max(1, d); }
    public String getReason() { return reason; }
    public void setReason(String r) { this.reason = r; }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return QuarantineRequestSignal.class; }
    @Override public String getDescription() { return "QuarantineRequestSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        QuarantineRequestSignal c = new QuarantineRequestSignal(entityId, kind, durationTicks, reason);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
