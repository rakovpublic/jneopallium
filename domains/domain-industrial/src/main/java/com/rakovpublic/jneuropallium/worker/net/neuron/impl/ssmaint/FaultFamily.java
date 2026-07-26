/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.ssmaint;

/**
 * Fault families the label-free model can attribute a degradation to. These are
 * heuristic hypotheses derived from which sensors dominate the reconstruction
 * residual — not learned class labels. {@link #UNKNOWN_ANOMALY} is the honest
 * fallback when the pattern matches no known family.
 */
public enum FaultFamily {
    BEARING_DAMAGE("bearing_damage"),
    CAVITATION("cavitation"),
    SENSOR_FAULT("sensor_fault"),
    ENERGY("energy"),
    OSCILLATION("oscillation"),
    UNKNOWN_ANOMALY("unknown_anomaly");

    private final String key;

    FaultFamily(String key) { this.key = key; }

    public String key() { return key; }

    public static FaultFamily fromKey(String key) {
        if (key != null) {
            for (FaultFamily f : values()) {
                if (f.key.equalsIgnoreCase(key)) return f;
            }
        }
        return UNKNOWN_ANOMALY;
    }
}
