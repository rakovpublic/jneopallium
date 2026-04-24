/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
/**
 * Domain signals for the industrial process-control module. Measurement
 * and actuator-command signals run on the fast loop (1/1); setpoints
 * and alarms on 1/2; interlocks at 1/1; efficiency, batch-state,
 * degradation, and maintenance-window signals live on the slow loop so
 * the scheduler keeps regulatory latency low. Every signal carries a
 * wall-clock timestamp in addition to the jneopallium tick, per spec §3.
 *
 * @see com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial;
