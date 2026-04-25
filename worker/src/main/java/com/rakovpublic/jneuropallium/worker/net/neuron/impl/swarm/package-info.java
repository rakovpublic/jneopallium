/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
/**
 * Swarm-robotics &amp; distributed multi-agent coordination module.
 * Implements {@code use-case-swarm-robotics.md}: peer sensing, role
 * specialisation, stigmergic coordination, auction-based task
 * allocation, light-weight consensus, Reynolds flocking, formation
 * keeping, Byzantine-tolerant isolation, and collective-harm
 * aggregation.
 *
 * <p>Architectural invariants:
 * <ul>
 *   <li>Quarantine is never permanent locally —
 *       {@link com.rakovpublic.jneuropallium.worker.net.neuron.impl.swarm.IsolationProtocolNeuron}
 *       expires every entry; repeats double the duration but a global
 *       consensus is required for permanent removal.</li>
 *   <li>Anomaly reports require ≥ k independent witnesses before
 *       isolation fires — minimum 3 per
 *       {@link com.rakovpublic.jneuropallium.worker.net.neuron.impl.swarm.SwarmConfig#setAnomalyThresholdVotes(int)};
 *       any value below 3 throws.</li>
 *   <li>Lethal autonomous weapons are out of scope: {@code SwarmConfig.isLawsEnabled()}
 *       returns {@code false} and cannot be flipped at runtime.</li>
 *   <li>Communication is realistic: every peer signal carries a
 *       {@code linkQuality}; processors weight contributions
 *       accordingly.</li>
 * </ul>
 *
 * @see com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.swarm;
