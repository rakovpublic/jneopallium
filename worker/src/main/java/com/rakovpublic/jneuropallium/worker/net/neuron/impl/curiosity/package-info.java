/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
/**
 * Intrinsic motivation / curiosity subsystem.
 *
 * <p>Biological analogue: hippocampal novelty detection driving SN/VTA
 * dopaminergic bursts (Lisman &amp; Grace 2005) plus prefrontal
 * controllability / empowerment estimates (Klyubin et al. 2005).
 *
 * <p>Key classes:
 * <ul>
 *   <li>{@link com.rakovpublic.jneuropallium.worker.net.neuron.impl.curiosity.NoveltyDetectorNeuron}
 *       — Bloom-filter novelty estimator.</li>
 *   <li>{@link com.rakovpublic.jneuropallium.worker.net.neuron.impl.curiosity.LearningProgressNeuron}
 *       — Oudeyer &amp; Kaplan (2007) learning-progress reward.</li>
 *   <li>{@link com.rakovpublic.jneuropallium.worker.net.neuron.impl.curiosity.EmpowermentNeuron}
 *       — Klyubin empowerment estimator.</li>
 *   <li>{@link com.rakovpublic.jneuropallium.worker.net.neuron.impl.curiosity.BoredomNeuron}
 *       — habituation-driven inhibition of return.</li>
 * </ul>
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.curiosity;
