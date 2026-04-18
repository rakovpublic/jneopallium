/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.embodiment;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.embodiment.BodySchemaUpdateSignal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Extends the body schema when the agent holds or mounts a tool.
 * Layer 2, loop=2 / epoch=5.
 * <p>Biological analogue: parieto-premotor tool incorporation
 * (Maravita &amp; Iriki 2004).
 */
public class ToolIncorporationNeuron extends ModulatableNeuron {

    private final Map<Integer, EffectorCapability> priorSchema = new HashMap<>();
    private final Set<Integer> toolEffectors = new HashSet<>();
    private int timeoutTicks = 600;

    public ToolIncorporationNeuron() { super(); }

    public ToolIncorporationNeuron(Long neuronId, ISignalChain chain, Long run) {
        super(neuronId, chain, run);
    }

    /**
     * Incorporate a tool into the body schema, emitting a schema update.
     *
     * @param target existing {@link BodySchemaNeuron} to update
     * @param effectorId id of the tool effector slot
     * @param toolCapability tool's capability description
     */
    public BodySchemaUpdateSignal incorporate(BodySchemaNeuron target, int effectorId, EffectorCapability toolCapability) {
        if (target == null || toolCapability == null) return null;
        EffectorCapability prior = target.currentSchema().get(effectorId);
        if (prior != null) priorSchema.put(effectorId, prior);
        toolEffectors.add(effectorId);
        target.register(effectorId, toolCapability);
        BodySchemaUpdateSignal sig = new BodySchemaUpdateSignal(effectorId, toolCapability, false);
        sig.setTool(true);
        sig.setSourceNeuronId(this.getId());
        return sig;
    }

    /**
     * Reverse a prior tool incorporation, restoring the original effector (or removing it).
     *
     * @param target existing {@link BodySchemaNeuron}
     * @param effectorId tool effector slot
     */
    public BodySchemaUpdateSignal release(BodySchemaNeuron target, int effectorId) {
        if (target == null || !toolEffectors.contains(effectorId)) return null;
        toolEffectors.remove(effectorId);
        EffectorCapability prior = priorSchema.remove(effectorId);
        if (prior == null) {
            target.onUpdate(new BodySchemaUpdateSignal(effectorId, null, false));
        } else {
            target.register(effectorId, prior);
        }
        BodySchemaUpdateSignal sig = new BodySchemaUpdateSignal(effectorId, prior, false);
        sig.setTool(false);
        sig.setSourceNeuronId(this.getId());
        return sig;
    }

    public boolean isToolEffector(int effectorId) { return toolEffectors.contains(effectorId); }

    public int getTimeoutTicks() { return timeoutTicks; }
    public void setTimeoutTicks(int timeoutTicks) { this.timeoutTicks = timeoutTicks; }
}
