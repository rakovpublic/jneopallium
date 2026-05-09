/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.mqtt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.AlarmPriority;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.Quality;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.AlarmSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MeasurementSignal;
import org.eclipse.tahu.message.SparkplugBPayloadDecoder;
import org.eclipse.tahu.message.SparkplugBPayloadEncoder;
import org.eclipse.tahu.message.model.Metric;
import org.eclipse.tahu.message.model.MetricDataType;
import org.eclipse.tahu.message.model.SparkplugBPayload;
import org.eclipse.tahu.model.MetricDataTypeMap;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Pure (no-IO) functions translating MQTT/Sparkplug payloads into
 * Jneopallium signals (02-MQTT-SPARKPLUG.md §5).
 *
 * <p>Sparkplug B payloads are decoded with {@link SparkplugBPayloadDecoder};
 * each numeric metric becomes a {@link MeasurementSignal} with quality
 * derived from {@code is_historical}/{@code is_transient}/{@code is_null}.
 * Boolean metrics whose name appears in the configured {@code severityMap}
 * become {@link AlarmSignal}s.
 *
 * <p>Plain MQTT payloads are JSON; the per-binding {@code jsonPath} (a small
 * dotted/index expression) selects the value to emit. Quality defaults to
 * {@code GOOD} unless the JSON itself carries a {@code quality} field.
 */
public final class MqttSignalMapper {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final Map<String, MqttBridgeConfig.AlarmPriorityName> severityMap;
    private final SparkplugBPayloadDecoder decoder = new SparkplugBPayloadDecoder();
    private final MetricDataTypeMap typeMap = new MetricDataTypeMap();

    public MqttSignalMapper(MqttBridgeConfig cfg) {
        this.severityMap = cfg.severityMap();
    }

    /** Decode a Sparkplug B payload, registering metric type info into the type map. */
    public SparkplugBPayload decodeSparkplug(byte[] payload) {
        try {
            return decoder.buildFromByteArray(payload, typeMap);
        } catch (Exception e) {
            throw new SignalMapperException("Sparkplug decode failed: " + e.getMessage(), e);
        }
    }

    /**
     * Convert one Sparkplug metric to a typed signal. Returns {@code null} if
     * the metric should be dropped (non-numeric, non-alarm, no binding).
     */
    public IInputSignal toSignal(MqttBridgeConfig.ReadBindingConfig binding,
                                 Metric metric,
                                 long fallbackTsMs) {
        if (binding == null || metric == null) return null;
        long ts = metric.getTimestamp() != null
                ? metric.getTimestamp().getTime() : fallbackTsMs;
        String tag = binding.signalTag() != null ? binding.signalTag() : binding.bindingId();

        if (binding.signalKind() == MqttBridgeConfig.ReadSignalKind.ALARM) {
            AlarmPriority p = AlarmPriority.JOURNAL;
            MqttBridgeConfig.AlarmPriorityName mapped = severityMap.get(metric.getName());
            if (mapped != null) p = AlarmPriority.valueOf(mapped.name());
            String code = booleanFromMetric(metric) ? "ALARM_ACTIVE" : "ALARM_NORMAL";
            return new AlarmSignal(p, tag, code, ts);
        }

        Double v = numericValue(metric);
        if (v == null) return null;  // non-numeric Sparkplug type, drop
        Quality q = qualityFromSparkplug(metric);
        return new MeasurementSignal(tag, v, q, ts);
    }

    /** Decode a plain MQTT JSON payload into one MeasurementSignal. */
    public IInputSignal fromPlainJson(MqttBridgeConfig.ReadBindingConfig binding,
                                      byte[] payload,
                                      long fallbackTsMs) {
        if (binding == null || payload == null) return null;
        JsonNode root;
        try {
            root = JSON.readTree(payload);
        } catch (IOException e) {
            throw new SignalMapperException("plain MQTT JSON decode failed: " + e.getMessage(), e);
        }
        String tag = binding.signalTag() != null ? binding.signalTag() : binding.bindingId();
        if (binding.signalKind() == MqttBridgeConfig.ReadSignalKind.ALARM) {
            JsonNode active = pickPath(root, binding.jsonPath());
            boolean isActive = active != null && active.asBoolean();
            long ts = pickTimestamp(root, fallbackTsMs);
            return new AlarmSignal(AlarmPriority.JOURNAL, tag,
                    isActive ? "ALARM_ACTIVE" : "ALARM_NORMAL", ts);
        }
        JsonNode v = pickPath(root, binding.jsonPath());
        if (v == null || !v.isNumber()) return null;
        long ts = pickTimestamp(root, fallbackTsMs);
        Quality q = qualityFromJson(root);
        return new MeasurementSignal(tag, v.asDouble(), q, ts);
    }

    /**
     * Build a Sparkplug B payload carrying one Double metric — used by the
     * advisory-egress aggregator when {@code sparkplugMetric} is configured.
     */
    public byte[] encodeSparkplugDouble(String metricName, double value, long ts) {
        try {
            Metric m = new Metric(metricName, null, new Date(ts),
                    MetricDataType.Double, false, false, null, null, value);
            SparkplugBPayload p = new SparkplugBPayload(new Date(ts), new ArrayList<>());
            p.addMetric(m);
            return new SparkplugBPayloadEncoder().getBytes(p, false);
        } catch (Exception e) {
            throw new SignalMapperException("Sparkplug encode failed: " + e.getMessage(), e);
        }
    }

    /* ===== helpers ============================================================ */

    private static Double numericValue(Metric m) {
        Object v = m.getValue();
        if (v == null) return null;
        if (Boolean.TRUE.equals(m.getIsNull())) return null;
        if (v instanceof Number n) return n.doubleValue();
        return null;
    }

    private static boolean booleanFromMetric(Metric m) {
        Object v = m.getValue();
        if (v instanceof Boolean b) return b;
        if (v instanceof Number n)  return n.doubleValue() != 0.0;
        return false;
    }

    private static Quality qualityFromSparkplug(Metric m) {
        if (Boolean.TRUE.equals(m.getIsNull())) return Quality.UNCERTAIN;
        if (Boolean.TRUE.equals(m.getIsHistorical())) return Quality.UNCERTAIN;
        if (Boolean.TRUE.equals(m.getIsTransient())) return Quality.UNCERTAIN;
        return Quality.GOOD;
    }

    private static Quality qualityFromJson(JsonNode root) {
        if (root == null) return Quality.GOOD;
        JsonNode q = root.get("quality");
        if (q == null) return Quality.GOOD;
        try {
            return Quality.valueOf(q.asText().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return Quality.UNCERTAIN;
        }
    }

    private static long pickTimestamp(JsonNode root, long fallback) {
        if (root == null) return fallback;
        JsonNode ts = root.get("ts");
        if (ts == null) ts = root.get("timestamp");
        if (ts == null) return fallback;
        if (ts.isNumber()) return ts.asLong();
        if (ts.isTextual()) {
            try {
                return Instant.parse(ts.asText()).toEpochMilli();
            } catch (DateTimeParseException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    /**
     * Minimal JSONPath: only {@code $.foo.bar} and {@code $.list[0]} forms are
     * supported. The bridge does not depend on a heavyweight JSONPath engine
     * for what is essentially a one-field projection.
     */
    static JsonNode pickPath(JsonNode root, String path) {
        if (root == null || path == null || path.isBlank()) return root;
        String s = path.trim();
        if (s.startsWith("$")) s = s.substring(1);
        if (s.startsWith(".")) s = s.substring(1);
        if (s.isEmpty()) return root;
        JsonNode cur = root;
        for (String part : s.split("\\.")) {
            if (cur == null || cur.isMissingNode()) return null;
            int lb = part.indexOf('[');
            if (lb < 0) {
                cur = cur.get(part);
            } else {
                String name = part.substring(0, lb);
                if (!name.isEmpty()) cur = cur.get(name);
                while (lb >= 0) {
                    int rb = part.indexOf(']', lb);
                    if (rb < 0) return null;
                    int idx = Integer.parseInt(part.substring(lb + 1, rb));
                    if (cur == null) return null;
                    cur = cur.get(idx);
                    lb = part.indexOf('[', rb);
                }
            }
        }
        return cur;
    }

    /** Wraps Tahu/Jackson exceptions so the caller can audit uniformly. */
    public static final class SignalMapperException extends RuntimeException {
        public SignalMapperException(String msg, Throwable cause) { super(msg, cause); }
    }
}
