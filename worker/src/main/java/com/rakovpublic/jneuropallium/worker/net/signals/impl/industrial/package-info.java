/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
/**
 * Domain signals for the industrial process-control and machine-health
 * module. Measurement and actuator-command signals run on the fast loop
 * (1/1); setpoints and alarms on 1/2; interlocks at 1/1; machine
 * waveforms stay on the fast loop; machine features, operating-regime,
 * domain-shift, fault, efficiency, batch-state, degradation,
 * health-advisory, and maintenance-window signals live on slower loops
 * so the scheduler keeps regulatory latency low. Every signal carries a
 * wall-clock timestamp in addition to the jneopallium tick.
 *
 * @see com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial;
