/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
/**
 * Glial support subsystem — astrocytes, microglia, oligodendrocytes.
 * Introduces activity-dependent per-connection propagation delay
 * ({@link com.rakovpublic.jneuropallium.worker.net.neuron.impl.glia.DelayedAxon})
 * which is the headline capability of this module.
 *
 * <p>Biological analogues and citations:
 * <ul>
 *   <li>Astrocytic calcium waves &amp; gliotransmission: Volterra &amp;
 *       Meldolesi 2005; Araque et al. 2014.</li>
 *   <li>Microglial synaptic pruning: Schafer et al. 2012; Paolicelli et al.
 *       2011.</li>
 *   <li>Activity-dependent myelination: Fields 2015; Gibson et al. 2014.</li>
 * </ul>
 *
 * <p>Key classes:
 * <ul>
 *   <li>{@link com.rakovpublic.jneuropallium.worker.net.neuron.impl.glia.DelayedAxon}
 *       — axon with per-connection myelination delays.</li>
 *   <li>{@link com.rakovpublic.jneuropallium.worker.net.neuron.impl.glia.AstrocyteNeuron}
 *       — local activity integrator.</li>
 *   <li>{@link com.rakovpublic.jneuropallium.worker.net.neuron.impl.glia.MicroglialPruningNeuron}
 *       — emits {@code PruningSignal}s for inactive connections.</li>
 *   <li>{@link com.rakovpublic.jneuropallium.worker.net.neuron.impl.glia.MyelinationNeuron}
 *       — accelerates frequently-used paths.</li>
 *   <li>{@link com.rakovpublic.jneuropallium.worker.net.neuron.impl.glia.DelayQueue}
 *       — dispatcher-side delay queue for delayed signal delivery.</li>
 * </ul>
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.glia;
