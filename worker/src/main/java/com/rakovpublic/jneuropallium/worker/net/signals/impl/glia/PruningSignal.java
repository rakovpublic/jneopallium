/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.glia;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/**
 * A request emitted by {@link com.rakovpublic.jneuropallium.worker.net.neuron.impl.glia.MicroglialPruningNeuron}
 * to sever a chronically-inactive or damaged connection.
 * <p>Biological analogue: microglial synaptic pruning
 * (Schafer et al. 2012; Paolicelli et al. 2011).
 * <p>ProcessingFrequency: loop=2, epoch=5.
 */
public class PruningSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(5L, 2);

    private long axonSourceId;
    private long axonTargetId;
    private String reason;

    public PruningSignal() {
        super();
        this.loop = 2;
        this.epoch = 5L;
        this.timeAlive = 100;
    }

    public PruningSignal(long axonSourceId, long axonTargetId, String reason) {
        this();
        this.axonSourceId = axonSourceId;
        this.axonTargetId = axonTargetId;
        this.reason = reason;
    }

    public long getAxonSourceId() { return axonSourceId; }
    public void setAxonSourceId(long axonSourceId) { this.axonSourceId = axonSourceId; }

    public long getAxonTargetId() { return axonTargetId; }
    public void setAxonTargetId(long axonTargetId) { this.axonTargetId = axonTargetId; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return PruningSignal.class; }
    @Override public String getDescription() { return "PruningSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        PruningSignal c = new PruningSignal(axonSourceId, axonTargetId, reason);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
