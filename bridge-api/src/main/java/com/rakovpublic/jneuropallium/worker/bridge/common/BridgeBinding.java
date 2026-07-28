/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.common;

/**
 * Per-binding metadata the universal output aggregator (00-FRAMEWORK §2.2)
 * needs to enforce safety, clamp, and rate-limit a write. Bridge-specific
 * binding classes adapt their native fields onto this shape.
 */
public interface BridgeBinding {

    /** ISA-95-style tag carried by signals. */
    String signalTag();

    /** Loop / regulatory unit (used to look up effective {@link BridgeSafetyMode}). */
    String loopId();

    BridgeBindingDirection direction();

    /** Value written when an interlock for this binding's loop trips. */
    Double failSafeValue();

    /** Maximum permitted change per second, or {@code null} for no limit. */
    Double rampRateMaxPerSec();

    /** Hard lower clamp, or {@code null} for no clamp. */
    Double minClampValue();

    /** Hard upper clamp, or {@code null} for no clamp. */
    Double maxClampValue();
}
