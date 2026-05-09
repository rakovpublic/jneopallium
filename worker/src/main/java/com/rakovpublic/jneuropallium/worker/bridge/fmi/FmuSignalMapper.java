/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.fmi;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.AlarmPriority;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.Quality;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.AlarmSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MeasurementSignal;

/**
 * Converts raw FMU variable values into Jneopallium input signals
 * (03-FMI-FMU.md §5).
 *
 * <p>Quality is always {@link Quality#GOOD} for FMU outputs — simulation is
 * deterministic and has no concept of sensor degradation. To test quality
 * propagation, configure the FMU itself to emit a quality output and bind it
 * to a separate MeasurementSignal.
 */
public final class FmuSignalMapper {

    private FmuSignalMapper() {}

    /**
     * Map a FMU Real (double) output to a {@link MeasurementSignal}.
     *
     * @param cfg       the read binding config
     * @param value     raw FMU variable value
     * @param inputName name of the parent IInitInput (for signal routing)
     * @param timestamp wall-clock epoch-millis of this tick
     */
    public static MeasurementSignal toMeasurement(
            FmiBridgeConfig.ReadBindingConfig cfg,
            double value,
            String inputName,
            long timestamp) {
        MeasurementSignal s = new MeasurementSignal(cfg.signalTag(), value, Quality.GOOD, timestamp);
        s.setInputName(inputName);
        s.setName(cfg.bindingId());
        return s;
    }

    /**
     * Map a FMU Boolean output to an {@link AlarmSignal} when the flag is
     * {@code true}. Returns {@code null} when the alarm is not active (false).
     *
     * @param cfg       the event binding config
     * @param active    current Boolean value of the FMU variable
     * @param inputName name of the parent IInitInput
     * @param timestamp wall-clock epoch-millis of this tick
     */
    public static AlarmSignal toAlarm(
            FmiBridgeConfig.EventBindingConfig cfg,
            boolean active,
            String inputName,
            long timestamp) {
        if (!active) return null;
        AlarmPriority priority = severityToAlarmPriority(cfg.severity());
        AlarmSignal s = new AlarmSignal(priority, cfg.signalTag(), cfg.bindingId(), timestamp);
        s.setInputName(inputName);
        s.setName(cfg.bindingId());
        return s;
    }

    private static AlarmPriority severityToAlarmPriority(String severity) {
        if (severity == null) return AlarmPriority.HIGH;
        return switch (severity.toUpperCase()) {
            case "CRITICAL", "URGENT" -> AlarmPriority.URGENT;
            case "HIGH" -> AlarmPriority.HIGH;
            case "LOW" -> AlarmPriority.LOW;
            default -> AlarmPriority.JOURNAL;
        };
    }
}
