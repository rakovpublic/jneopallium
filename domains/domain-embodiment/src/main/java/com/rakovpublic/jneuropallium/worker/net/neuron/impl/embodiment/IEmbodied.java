/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.embodiment;

import com.rakovpublic.jneuropallium.worker.net.neuron.INeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.embodiment.ProprioceptiveSignal;

/**
 * Contract for neurons that own or reflect a body schema.
 * Biological analogue: posterior parietal cortex body-schema neurons.
 */
public interface IEmbodied extends INeuron {

    /**
     * @return the current body schema snapshot.
     */
    BodySchema currentSchema();

    /**
     * React to proprioceptive feedback.
     *
     * @param s proprioceptive signal
     */
    void onProprioceptive(ProprioceptiveSignal s);
}
