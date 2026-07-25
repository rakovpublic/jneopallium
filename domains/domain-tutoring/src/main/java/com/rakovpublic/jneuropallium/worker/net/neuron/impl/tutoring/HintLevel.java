/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.tutoring;

/**
 * Graduated hint levels, from least-informative to most-informative.
 * Ordering matters: {@link #ordinal()} defines progression.
 */
public enum HintLevel {
    META_COGNITIVE, CONCEPTUAL, WORKED_EXAMPLE
}
