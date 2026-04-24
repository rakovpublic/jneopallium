/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
/**
 * Industrial process control &amp; digital twin module. Implements
 * {@code use-case-industrial-process-control.md}: regulatory control
 * (PID, cascade, feed-forward), supervisory layer (setpoint optimiser,
 * alarm aggregation, plant-mode state machine), process / degradation
 * / product-quality models, MPC and campaign planning, safety gate /
 * actuator with operator override, hard-wired interlocks, oscillation
 * monitor with graduated interventions, and energy accounting.
 *
 * <p>Architectural invariants enforced at the package level:
 * <ul>
 *   <li>Operator override always wins for regulatory control; an
 *       interlock trip always wins for safety.
 *       {@link com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.ActuatorNeuron}
 *       honours the most-recent
 *       {@link com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.OperatorOverrideSignal}.</li>
 *   <li>{@link com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.InterlockNeuron}
 *       rules are compiled at construction; {@code addInterlock} throws
 *       after {@code seal()} — the SRS is immutable at runtime.</li>
 *   <li>Per-loop deployment mode is
 *       {@code SHADOW / ADVISORY / AUTONOMOUS}; the SafetyGate
 *       downgrades {@code execute} accordingly (spec §7).</li>
 * </ul>
 *
 * @see com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial;
