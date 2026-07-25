/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
/**
 * Cybersecurity / immune-system module for the jneopallium framework.
 * Implements the biological-immune-system analogue described in
 * {@code use-case-cybersecurity-immune.md}: innate signature detection,
 * adaptive anomaly detection, memory of past encounters, self-tolerance,
 * graduated response (log → alert → rate-limit → quarantine → block),
 * and homeostatic damping of alert storms.
 *
 * <p>The critical architectural invariants enforced by this package:
 * <ul>
 *   <li>Quarantine is never permanent —
 *       {@link com.rakovpublic.jneuropallium.worker.net.neuron.impl.security.QuarantineEntityNeuron}
 *       requires a positive duration on every request and automatically
 *       emits a lift signal at expiry unless reconfirmed.</li>
 *   <li>Hard allow-lists and critical-asset lists live on the gate
 *       neuron and are separate from the soft allow-list updated by
 *       {@code SelfToleranceSignal}; only the soft list is
 *       runtime-mutable.</li>
 *   <li>The {@code affect} and {@code curiosity} modules must stay off
 *       per spec §11; {@link com.rakovpublic.jneuropallium.worker.net.neuron.impl.security.SecurityConfig}
 *       reports this as a program-level flag.</li>
 * </ul>
 *
 * @see com.rakovpublic.jneuropallium.worker.net.signals.impl.security
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.security;
