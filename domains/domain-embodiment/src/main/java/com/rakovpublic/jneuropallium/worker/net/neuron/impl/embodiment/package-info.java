/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
/**
 * Embodiment / proprioception subsystem.
 *
 * <p>Biological analogue: posterior parietal cortex body-schema circuits
 * plus cerebellar efference-copy forward models
 * (Wolpert, Miall &amp; Kawato 1998; Maravita &amp; Iriki 2004).
 *
 * <p>Key classes:
 * <ul>
 *   <li>{@link com.rakovpublic.jneuropallium.worker.net.neuron.impl.embodiment.EfferenceCopyNeuron}
 *       — forward-model branch point.</li>
 *   <li>{@link com.rakovpublic.jneuropallium.worker.net.neuron.impl.embodiment.BodySchemaNeuron}
 *       — maintains effector body schema.</li>
 *   <li>{@link com.rakovpublic.jneuropallium.worker.net.neuron.impl.embodiment.ToolIncorporationNeuron}
 *       — extends body schema with tools.</li>
 *   <li>{@link com.rakovpublic.jneuropallium.worker.net.neuron.impl.embodiment.ReafferenceComparatorNeuron}
 *       — compares efference copy against actual proprioceptive input.</li>
 * </ul>
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.embodiment;
