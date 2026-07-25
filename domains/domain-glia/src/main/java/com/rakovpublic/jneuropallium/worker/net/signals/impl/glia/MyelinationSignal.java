/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.glia;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/**
 * A request to reduce the propagation delay on a specific axonal
 * connection, reflecting activity-dependent myelination.
 * <p>Biological analogue: oligodendrocyte-mediated activity-dependent
 * myelination (Fields 2015; Gibson et al. 2014).
 * <p>ProcessingFrequency: loop=2, epoch=10.
 */
public class MyelinationSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(10L, 2);

    private long axonSourceId;
    private long axonTargetId;
    private int newDelayTicks;

    public MyelinationSignal() {
        super();
        this.loop = 2;
        this.epoch = 10L;
        this.timeAlive = 100;
    }

    public MyelinationSignal(long axonSourceId, long axonTargetId, int newDelayTicks) {
        this();
        this.axonSourceId = axonSourceId;
        this.axonTargetId = axonTargetId;
        this.newDelayTicks = Math.max(0, newDelayTicks);
    }

    public long getAxonSourceId() { return axonSourceId; }
    public void setAxonSourceId(long axonSourceId) { this.axonSourceId = axonSourceId; }

    public long getAxonTargetId() { return axonTargetId; }
    public void setAxonTargetId(long axonTargetId) { this.axonTargetId = axonTargetId; }

    public int getNewDelayTicks() { return newDelayTicks; }
    public void setNewDelayTicks(int newDelayTicks) { this.newDelayTicks = Math.max(0, newDelayTicks); }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return MyelinationSignal.class; }
    @Override public String getDescription() { return "MyelinationSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        MyelinationSignal c = new MyelinationSignal(axonSourceId, axonTargetId, newDelayTicks);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
