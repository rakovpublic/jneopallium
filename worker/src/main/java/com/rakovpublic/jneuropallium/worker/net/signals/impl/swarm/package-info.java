/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
/**
 * Domain signals for the swarm-robotics module. Peer observations and
 * flocking signals run on the fast loop (1/2, 1/1); peer state, task
 * messages, votes, and anomaly reports on the slow loop (2/1);
 * pheromones and stigmergic traces on 2/2 and 2/5; swarm alerts at
 * 1/1 (must propagate fast). Every peer-bound signal carries a
 * link-quality indicator per spec §3.
 *
 * @see com.rakovpublic.jneuropallium.worker.net.neuron.impl.swarm
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm;
