/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
/**
 * Domain signals for the cybersecurity / immune-system module. Every
 * signal declares its {@code ProcessingFrequency(loop, epoch)}
 * statically so the scheduler can place it on the correct biological
 * timescale — packets / syscalls at loop 1 / epoch 1, logs at loop 1 /
 * epoch 2, anomaly scores at loop 1 / epoch 2, hypothesis and
 * response-planning output at loop 2.
 *
 * @see com.rakovpublic.jneuropallium.worker.net.neuron.impl.security
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.security;
