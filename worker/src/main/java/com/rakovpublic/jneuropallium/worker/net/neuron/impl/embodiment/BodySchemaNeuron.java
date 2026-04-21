/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.embodiment;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.embodiment.BodySchemaUpdateSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.embodiment.ProprioceptiveSignal;

/**
 * Maintains the agent's {@link BodySchema}.
 * Layer 2, loop=2 / epoch=3.
 * <p>Biological analogue: posterior parietal cortex body schema neurons.
 */
public class BodySchemaNeuron extends ModulatableNeuron implements IEmbodied, IBodySchemaNeuron {

    private BodySchema schema;

    public BodySchemaNeuron() {
        super();
        this.schema = new BodySchema();
    }

    public BodySchemaNeuron(Long neuronId, ISignalChain chain, Long run) {
        super(neuronId, chain, run);
        this.schema = new BodySchema();
    }

    @Override public BodySchema currentSchema() { return schema; }

    @Override
    public void onProprioceptive(ProprioceptiveSignal s) {
        if (s == null) return;
        // Verify the effector is known; unseen effector registers as unknown capability
        if (!schema.has(s.getEffectorId())) {
            schema = schema.with(s.getEffectorId(),
                    new EffectorCapability(s.getJointStates().length,
                            new double[s.getJointStates().length],
                            new double[s.getJointStates().length], 1.0, false));
        }
    }

    /**
     * Apply a body-schema update.
     *
     * @param update schema update signal
     */
    public void onUpdate(BodySchemaUpdateSignal update) {
        if (update == null) return;
        if (update.getCapability() == null) {
            schema = schema.without(update.getEffectorId());
        } else {
            schema = schema.with(update.getEffectorId(), update.getCapability());
        }
    }

    /**
     * Register an effector directly.
     */
    public void register(int effectorId, EffectorCapability cap) {
        schema = schema.with(effectorId, cap);
    }
}
