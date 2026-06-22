/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.security;

/** Timestamped evidence retained by the temporal threat correlator. */
public record TemporalThreatEvidence(
        String type,
        String entityId,
        long eventTick,
        double confidence,
        String source,
        String attackTechnique
) {
    public TemporalThreatEvidence {
        confidence = Math.max(0.0, Math.min(1.0, confidence));
    }
}
