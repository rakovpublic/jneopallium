/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.embodiment;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.embodiment.EffectorCapability;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/**
 * Update to the agent's body schema.
 * Biological analogue: slow plasticity of the posterior parietal body map
 * following injury or tool use.
 * <p>ProcessingFrequency: loop=2, epoch=3.
 */
public class BodySchemaUpdateSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(3L, 2);

    private int effectorId;
    private EffectorCapability capability;
    private boolean damaged;
    private boolean tool;

    public BodySchemaUpdateSignal() {
        super();
        this.loop = 2;
        this.epoch = 3L;
        this.timeAlive = 100;
    }

    public BodySchemaUpdateSignal(int effectorId, EffectorCapability capability, boolean damaged) {
        this();
        this.effectorId = effectorId;
        this.capability = capability;
        this.damaged = damaged;
    }

    public int getEffectorId() { return effectorId; }
    public void setEffectorId(int effectorId) { this.effectorId = effectorId; }

    public EffectorCapability getCapability() { return capability; }
    public void setCapability(EffectorCapability capability) { this.capability = capability; }

    public boolean isDamaged() { return damaged; }
    public void setDamaged(boolean damaged) { this.damaged = damaged; }

    public boolean isTool() { return tool; }
    public void setTool(boolean tool) { this.tool = tool; }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return BodySchemaUpdateSignal.class; }
    @Override public String getDescription() { return "BodySchemaUpdateSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        BodySchemaUpdateSignal c = new BodySchemaUpdateSignal(effectorId, capability, damaged);
        c.tool = this.tool;
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
