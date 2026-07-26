/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.fmi;

import java.util.HashMap;
import java.util.Map;

/**
 * Pure-Java simulation of a heated-tank model for use in FMI bridge tests
 * (03-FMI-FMU.md scenarios S7–S12).
 *
 * <h2>Physical model</h2>
 * <pre>
 *   dT/dt = (Q - h_loss * (T - T_amb)) / C_thermal
 *
 *   C_thermal = 5.0  J/K   (thermal capacity)
 *   h_loss    = 1.0  W/K   (heat loss coefficient)
 *   T_amb     = 20.0 °C    (ambient temperature)
 *   T_alarm   = 80.0 °C    (over-temperature threshold)
 * </pre>
 *
 * <p>Steady state for input Q: T_ss = T_amb + Q / h_loss
 * At Q = 40 W → T_ss = 60 °C. Time constant τ = C/h = 5 s.
 *
 * <h2>Value references</h2>
 * <pre>
 *   0 → "tank.T"                  Real output (temperature)
 *   1 → "heater.Q"                Real input  (heater power, W)
 *   2 → "alarm.over_temperature"  Boolean output (true when T > T_alarm)
 * </pre>
 *
 * <h2>Test control</h2>
 * Set {@link #failOnNextDoStep} to {@code true} to simulate an FMU ERROR
 * return, exercising scenario S11.
 */
public final class StubFmuDriver implements FmuDriver {

    public static final int VR_TEMP   = 0;
    public static final int VR_HEATER = 1;
    public static final int VR_ALARM  = 2;

    private static final double C_THERMAL = 5.0;   // J/K
    private static final double H_LOSS    = 1.0;   // W/K
    private static final double T_AMB     = 20.0;  // °C
    private static final double T_ALARM   = 80.0;  // °C

    // State
    private double temperature = 20.0;  // current tank temperature
    private double heaterPower = 0.0;   // input

    // Test hooks
    public volatile boolean failOnNextDoStep = false;
    public volatile boolean closed = false;

    private final Map<Integer, Double> realInputs = new HashMap<>();

    @Override
    public void instantiate(String instanceName, boolean loggingOn) {
        // no-op for stub
    }

    @Override
    public void setupExperiment(double startTime, boolean toleranceDefined, double tolerance) {
        // no-op
    }

    @Override
    public void enterInitializationMode() { /* no-op */ }

    @Override
    public void exitInitializationMode() { /* no-op */ }

    @Override
    public double getReal(int valueReference) {
        if (valueReference == VR_TEMP) return temperature;
        if (valueReference == VR_HEATER) return heaterPower;
        return 0.0;
    }

    @Override
    public boolean getBoolean(int valueReference) {
        if (valueReference == VR_ALARM) return temperature > T_ALARM;
        return false;
    }

    @Override
    public int getInteger(int valueReference) { return 0; }

    @Override
    public void setReal(int valueReference, double value) {
        realInputs.put(valueReference, value);
        if (valueReference == VR_HEATER) heaterPower = value;
    }

    @Override
    public void setBoolean(int valueReference, boolean value) { /* no inputs */ }

    @Override
    public FmiStatus doStep(double currentTime, double stepSize) {
        if (failOnNextDoStep) {
            failOnNextDoStep = false;
            return FmiStatus.ERROR;
        }
        // Euler integration: dT = (Q - h*(T - T_amb)) / C * dt
        double dT = (heaterPower - H_LOSS * (temperature - T_AMB)) / C_THERMAL * stepSize;
        temperature += dT;
        return FmiStatus.OK;
    }

    @Override
    public void terminate() { /* no-op */ }

    @Override
    public void close() { closed = true; }

    /** Direct read of internal temperature state (for test assertions). */
    public double temperature() { return temperature; }

    /** Override heater power without going through the FMI API (test setup). */
    public void forceTemperature(double t) { this.temperature = t; }

    /**
     * Build a {@link FmuModelDescription} matching this stub's value references.
     */
    public static FmuModelDescription modelDescription() {
        return FmuModelDescription.of("StubTankModel", "2.0", Map.of(
                "tank.T", VR_TEMP,
                "heater.Q", VR_HEATER,
                "alarm.over_temperature", VR_ALARM
        ));
    }
}
