/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial;

/** Graduated oscillation-damping interventions from spec §6. */
public enum OscillationIntervention { NONE, SCALE_WEIGHTS, INJECT_INHIBITION, BREAK_CONNECTION, QUARANTINE_NEURON }
