/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.security;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/**
 * Update to the soft allow-list: {@code entityPattern} matches allow
 * (or revocation when {@code allow=false}). These are separate from the
 * hard constraints held by {@code EthicalPriorityNeuron} — those cannot
 * be changed at runtime.
 * ProcessingFrequency: loop=2, epoch=5.
 */
public class SelfToleranceSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(5L, 2);

    private String entityPattern;
    private boolean allow;

    public SelfToleranceSignal() { super(); this.loop = 2; this.epoch = 5L; this.timeAlive = 2000; }

    public SelfToleranceSignal(String entityPattern, boolean allow) {
        this();
        this.entityPattern = entityPattern;
        this.allow = allow;
    }

    public String getEntityPattern() { return entityPattern; }
    public void setEntityPattern(String e) { this.entityPattern = e; }
    public boolean isAllow() { return allow; }
    public void setAllow(boolean a) { this.allow = a; }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return SelfToleranceSignal.class; }
    @Override public String getDescription() { return "SelfToleranceSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        SelfToleranceSignal c = new SelfToleranceSignal(entityPattern, allow);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
