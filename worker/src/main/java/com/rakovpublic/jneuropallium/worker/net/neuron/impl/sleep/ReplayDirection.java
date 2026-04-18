/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.sleep;

/**
 * Direction in which a hippocampal replay traverses a recorded episode.
 * Biological default is {@link #REVERSE} during rest immediately after
 * experience (Foster &amp; Wilson 2006).
 */
public enum ReplayDirection {
    FORWARD,
    REVERSE,
    SHUFFLED
}
