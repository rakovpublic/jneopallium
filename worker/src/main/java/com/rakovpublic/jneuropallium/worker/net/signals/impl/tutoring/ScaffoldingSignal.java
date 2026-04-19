/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.tutoring;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.tutoring.ScaffoldType;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/**
 * Scaffolding intervention (outline, analogy, worked steps, reminder) issued
 * when the learner shows signs of overload.
 * ProcessingFrequency: loop=1, epoch=2.
 */
public class ScaffoldingSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(2L, 1);

    private ScaffoldType type;
    private Object scaffoldPayload;

    public ScaffoldingSignal() {
        super();
        this.loop = 1;
        this.epoch = 2L;
        this.timeAlive = 100;
        this.type = ScaffoldType.OUTLINE;
    }

    public ScaffoldingSignal(ScaffoldType type, Object scaffoldPayload) {
        this();
        this.type = type == null ? ScaffoldType.OUTLINE : type;
        this.scaffoldPayload = scaffoldPayload;
    }

    public ScaffoldType getType() { return type; }
    public void setType(ScaffoldType t) { this.type = t == null ? ScaffoldType.OUTLINE : t; }
    public Object getScaffoldPayload() { return scaffoldPayload; }
    public void setScaffoldPayload(Object p) { this.scaffoldPayload = p; }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return ScaffoldingSignal.class; }
    @Override public String getDescription() { return "ScaffoldingSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        ScaffoldingSignal c = new ScaffoldingSignal(type, scaffoldPayload);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
