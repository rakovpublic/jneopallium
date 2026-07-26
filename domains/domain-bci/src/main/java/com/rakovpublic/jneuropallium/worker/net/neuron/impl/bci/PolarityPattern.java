/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci;

/**
 * Stimulation pulse polarity pattern. Safe, charge-balanced waveforms
 * (cathodic-first biphasic) are the default on neural microelectrodes.
 */
public enum PolarityPattern {
    CATHODIC_FIRST_BIPHASIC,
    ANODIC_FIRST_BIPHASIC,
    BIPHASIC_ASYMMETRIC,
    MONOPHASIC
}
