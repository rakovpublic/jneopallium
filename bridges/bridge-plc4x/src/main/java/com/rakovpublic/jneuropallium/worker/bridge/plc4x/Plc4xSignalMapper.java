/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.plc4x;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.AlarmPriority;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.Quality;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.AlarmSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MeasurementSignal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Pure functions mapping PLC4X read responses onto Jneopallium input signals
 * (01-PLC4X.md §4).
 *
 * <p>Quality mapping table:
 * <pre>
 *   PlcResponseCode.OK              → Quality.GOOD
 *   PlcResponseCode.ACCESS_DENIED   → Quality.UNCERTAIN
 *   PlcResponseCode.NOT_FOUND       → Quality.UNCERTAIN
 *   PlcResponseCode.INVALID_*       → Quality.BAD
 *   anything else                   → Quality.BAD
 * </pre>
 *
 * <p>Per 00-FRAMEWORK §0.5 ("Quality propagates"), the value is passed through
 * unchanged regardless of quality — downstream neurons decide what to do with
 * a BAD/UNCERTAIN measurement.
 */
public final class Plc4xSignalMapper {

    private Plc4xSignalMapper() {}

    public static Quality toQuality(Plc4xResponseCode code) {
        if (code == null) return Quality.BAD;
        return switch (code) {
            case OK -> Quality.GOOD;
            case ACCESS_DENIED, NOT_FOUND, RESPONSE_PENDING -> Quality.UNCERTAIN;
            case INVALID_ADDRESS, INVALID_DATATYPE, INVALID_DATA,
                 INTERNAL_ERROR, REMOTE_ERROR -> Quality.BAD;
        };
    }

    /**
     * Coerce a PLC4X-native value into a {@code double} the
     * {@link MeasurementSignal} expects.
     *
     * <p>{@link Boolean} → 1.0/0.0; {@link Number} → {@link Number#doubleValue()};
     * everything else → {@code Double.NaN} (downstream filters such inputs out).
     */
    public static double coerceToDouble(Object value) {
        if (value == null) return Double.NaN;
        if (value instanceof Boolean b) return b ? 1.0 : 0.0;
        if (value instanceof Number n) return n.doubleValue();
        if (value instanceof String s) {
            try { return Double.parseDouble(s); }
            catch (NumberFormatException ex) { return Double.NaN; }
        }
        return Double.NaN;
    }

    /** Map one PLC4X read response onto a {@link MeasurementSignal}. */
    public static MeasurementSignal toMeasurement(
            Plc4xConfig.ReadBindingConfig cfg,
            Plc4xDriver.ReadResponse response,
            String inputName,
            long timestampMs) {
        Quality q = toQuality(response.code());
        double v = coerceToDouble(response.value());
        MeasurementSignal s = new MeasurementSignal(cfg.signalTag(), v, q, timestampMs);
        s.setInputName(inputName);
        s.setName(cfg.bindingId());
        return s;
    }

    /**
     * Decode one polled alarm word against its severity map and produce one
     * {@link AlarmSignal} per matching set bit (01-PLC4X.md §5 / S11).
     *
     * <p>Severity-map keys are parsed as integers (hex with the {@code 0x}
     * prefix or decimal). A bit is "matching" when {@code (value & mask) == mask}.
     */
    public static List<AlarmSignal> decodeAlarmWord(
            Plc4xConfig.EventBindingConfig cfg,
            Plc4xDriver.ReadResponse response,
            String inputName,
            long timestampMs) {
        List<AlarmSignal> out = new ArrayList<>();
        if (response.code() != Plc4xResponseCode.OK || response.value() == null) {
            return out;
        }

        long word = (long) coerceToDouble(response.value());
        for (Map.Entry<String, String> entry : cfg.severityMap().entrySet()) {
            long mask;
            try { mask = parseMask(entry.getKey()); }
            catch (NumberFormatException ex) { continue; }
            if (mask == 0) continue;
            if ((word & mask) == mask) {
                AlarmPriority priority = severityToPriority(entry.getValue());
                AlarmSignal s = new AlarmSignal(
                        priority,
                        cfg.signalTag() + "." + entry.getKey(),
                        cfg.bindingId() + ":" + entry.getKey(),
                        timestampMs);
                s.setInputName(inputName);
                s.setName(cfg.bindingId());
                out.add(s);
            }
        }
        return out;
    }

    static long parseMask(String maskKey) {
        String s = maskKey.trim();
        if (s.startsWith("0x") || s.startsWith("0X")) {
            return Long.parseLong(s.substring(2), 16);
        }
        return Long.parseLong(s);
    }

    static AlarmPriority severityToPriority(String severity) {
        if (severity == null) return AlarmPriority.JOURNAL;
        return switch (severity.trim().toUpperCase()) {
            case "CRITICAL", "URGENT" -> AlarmPriority.URGENT;
            case "HIGH" -> AlarmPriority.HIGH;
            case "LOW" -> AlarmPriority.LOW;
            default -> AlarmPriority.JOURNAL;
        };
    }
}
