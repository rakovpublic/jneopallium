/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */

/**
 * Runnable implementation of {@code demo-01-reactor-cascade-control.md} — the
 * flagship closed-loop control demo (OPC UA bridge, AUTONOMOUS per-loop).
 *
 * <p>A {@link com.rakovpublic.jneuropallium.worker.demo.industrial.ReactorPlantSimulator}
 * (FOPDT CSTR) is driven through the real OPC UA bridge + industrial neuron
 * pipeline by
 * {@link com.rakovpublic.jneuropallium.worker.demo.industrial.ReactorCascadeController},
 * with writes committed only by
 * {@code OpcUaCommandOutputAggregator}. The closed loop runs in-process via
 * {@link com.rakovpublic.jneuropallium.worker.demo.industrial.SimulatedReactorOpcUaService}
 * (no network), or against a real OPC UA server (the {@code asyncua} plant in
 * {@code src/test/python/reactor}) by substituting a plain
 * {@code MiloOpcUaClientService}.
 *
 * @see com.rakovpublic.jneuropallium.worker.demo.industrial.Demo01ReactorCascadeControl
 */
package com.rakovpublic.jneuropallium.worker.demo.industrial;
