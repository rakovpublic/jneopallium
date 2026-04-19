/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci;

/**
 * What a calibration session is trying to adjust.
 */
public enum CalibrationTarget {
    DECODER_WEIGHTS,
    CHANNEL_SELECTION,
    INTENT_MAPPING,
    FEEDBACK_INTENSITY,
    FULL_RECALIBRATION
}
