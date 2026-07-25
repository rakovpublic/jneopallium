/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MeasurementSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.OperatingRegimeSignal;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Builds operating-regime context from process measurements. */
public class OperatingRegimeNeuron extends ModulatableNeuron implements IOperatingRegimeNeuron {

    private static final class State {
        double rpm;
        double load;
        double flow;
        double pressure;
        double temperature;
        double command;
        long timestamp;
    }

    private final Map<String, State> states = new HashMap<>();

    public OperatingRegimeNeuron() { super(); }
    public OperatingRegimeNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    @Override
    public OperatingRegimeSignal classify(String assetId, double rpm, double loadFraction, double flow,
                                          double pressure, double temperature, double actuatorCommand,
                                          long timestamp) {
        double load = MachineSignalMath.clamp01(loadFraction);
        String regime;
        if (rpm <= 1.0 && load < 0.05) regime = "STOPPED";
        else if (pressure > 0.0 && pressure < 0.35 && flow < 0.30) regime = "STARVED_SUCTION";
        else if (load > 0.75) regime = "HIGH_LOAD";
        else if (rpm > 3000.0 || load > 0.65) regime = "HIGH_SPEED";
        else if (rpm > 0.0 && rpm < 1200.0 || load < 0.35) regime = "LOW_SPEED";
        else regime = "NOMINAL";
        double confidence = MachineSignalMath.clamp01(0.30
                + (rpm > 0.0 ? 0.20 : 0.0)
                + (flow > 0.0 ? 0.15 : 0.0)
                + (pressure > 0.0 ? 0.15 : 0.0)
                + (actuatorCommand > 0.0 ? 0.10 : 0.0)
                + (load > 0.0 ? 0.10 : 0.0));
        return new OperatingRegimeSignal(asset(assetId), rpm, load, flow, pressure,
                temperature, actuatorCommand, regime, confidence, timestamp);
    }

    @Override
    public OperatingRegimeSignal observe(String assetId, MeasurementSignal measurement) {
        if (measurement == null || measurement.getTag() == null) return null;
        String asset = asset(assetId);
        State state = states.computeIfAbsent(asset, ignored -> new State());
        String tag = measurement.getTag().toUpperCase(Locale.ROOT);
        double value = measurement.getMeasurement();
        if (tag.contains("RPM")) state.rpm = Math.max(0.0, value);
        else if (tag.contains("SPEED")) state.rpm = Math.max(0.0, value <= 120.0 ? value * 36.0 : value);
        else if (tag.contains("POWER") || tag.contains("LOAD")) state.load = MachineSignalMath.clamp01(value / 10.0);
        else if (tag.contains("FLOW")) {
            state.flow = Math.max(0.0, value);
            state.load = Math.max(state.load, MachineSignalMath.clamp01(value / 2.0));
        } else if (tag.contains("PRESSURE") || tag.contains("SUCTION")) state.pressure = Math.max(0.0, value);
        else if (tag.contains("TEMP") || tag.contains("TIC")) state.temperature = value;
        else if (tag.contains("CMD") || tag.contains("POSITION")) state.command = MachineSignalMath.clamp01(value / 100.0);
        state.timestamp = measurement.getTimestamp();
        return classify(asset, state.rpm, state.load, state.flow, state.pressure,
                state.temperature, state.command, state.timestamp);
    }

    private static String asset(String assetId) {
        return assetId == null || assetId.isBlank() ? "UNKNOWN" : assetId;
    }
}
