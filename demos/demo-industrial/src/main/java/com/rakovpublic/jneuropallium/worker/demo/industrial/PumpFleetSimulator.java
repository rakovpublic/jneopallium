/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.demo.industrial;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Toy population model for the demo-02 pump fleet
 * ({@code demo-02-pump-fleet-predictive-maintenance.md}).
 *
 * <p>Each pump has bearing vibration RMS (mm/s) and bearing temperature
 * (°C). The vibration baseline drifts upward at a configurable per-pump
 * rate ({@link #setVibrationRamp}); a flat pump keeps a steady value, a
 * "wearing" pump's RMS rises with each tick — the degradation model
 * downstream turns that trend into a falling RUL estimate.
 *
 * <p>Temperature defaults to a noise-free 60 °C — the spec mostly cares
 * about the vibration signal, but the model still ingests temperature so
 * pumps look multi-channel to the bridge.
 */
public final class PumpFleetSimulator {

    /** Per-pump tunable state. */
    static final class Pump {
        double vibrationRms;
        double vibrationRampPerSec;
        double bearingTemp;
    }

    public static final double DEFAULT_VIB_BASELINE = 1.0;     // mm/s
    public static final double DEFAULT_BEARING_TEMP = 60.0;    // °C

    private final Map<String, Pump> pumps = new HashMap<>();
    private final List<String> pumpIds;

    public PumpFleetSimulator(List<String> pumpIds) {
        this.pumpIds = List.copyOf(Objects.requireNonNull(pumpIds, "pumpIds"));
        for (String id : this.pumpIds) {
            Pump p = new Pump();
            p.vibrationRms = DEFAULT_VIB_BASELINE;
            p.vibrationRampPerSec = 0.0;
            p.bearingTemp = DEFAULT_BEARING_TEMP;
            pumps.put(id, p);
        }
    }

    public List<String> pumpIds() { return pumpIds; }

    /** Set the per-second vibration ramp for one pump (positive ⇒ wearing). */
    public void setVibrationRamp(String pumpId, double mmPerSecPerSec) {
        Pump p = pumps.get(pumpId);
        if (p == null) return;
        p.vibrationRampPerSec = mmPerSecPerSec;
    }

    /** Hard-set the current vibration value (used to seed scenarios). */
    public void setVibration(String pumpId, double mmPerSec) {
        Pump p = pumps.get(pumpId);
        if (p == null) return;
        p.vibrationRms = Math.max(0.0, mmPerSec);
    }

    /** Hard-set the current bearing temperature value. */
    public void setBearingTemp(String pumpId, double celsius) {
        Pump p = pumps.get(pumpId);
        if (p == null) return;
        p.bearingTemp = celsius;
    }

    public double vibration(String pumpId) {
        Pump p = pumps.get(pumpId);
        return p == null ? Double.NaN : p.vibrationRms;
    }

    public double bearingTemp(String pumpId) {
        Pump p = pumps.get(pumpId);
        return p == null ? Double.NaN : p.bearingTemp;
    }

    /** Advance every pump by {@code dtSeconds}; vibration follows its ramp. */
    public void step(double dtSeconds) {
        for (Pump p : pumps.values()) {
            p.vibrationRms = Math.max(0.0, p.vibrationRms + p.vibrationRampPerSec * dtSeconds);
        }
    }
}
