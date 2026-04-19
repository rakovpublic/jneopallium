/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci;

/**
 * Type of pre-ictal or ictal EEG/ECoG pattern that triggered a seizure risk
 * estimate.
 */
public enum SeizureMarker {
    NONE,
    HIGH_FREQUENCY_OSCILLATION,
    SPIKE_WAVE_DISCHARGE,
    GAMMA_SURGE,
    RHYTHMIC_THETA,
    PLI_ABNORMAL
}
