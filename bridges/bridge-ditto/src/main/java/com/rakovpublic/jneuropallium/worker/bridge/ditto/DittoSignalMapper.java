/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.ditto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.AlarmPriority;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.Quality;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.AlarmSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MeasurementSignal;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Pure (no-IO) functions translating Ditto twin payloads into Jneopallium
 * signals (10-DITTO.md §4) and back into twin-command JSON.
 *
 * <p>Ingress: a feature property is a JSON value (number, boolean, object).
 * Numeric properties become {@link MeasurementSignal}s with {@link Quality}
 * derived from a {@code quality} sibling field if present (otherwise
 * {@code GOOD}, or {@code UNCERTAIN} when the bridge has marked the thing as
 * offline). Boolean properties whose feature name appears in the configured
 * {@code severityMap} become {@link AlarmSignal}s.
 *
 * <p>Egress: an advisory write encodes a Ditto twin command in the
 * <a href="https://www.eclipse.org/ditto/protocol-specification.html">Ditto
 * protocol</a> shape — a {@code modify} command targeting
 * {@code things/<thingId>/features/<feature>/properties/<property>} with a
 * scalar value. The actual REST/WS path is composed by the transport.
 */
public final class DittoSignalMapper {

    public static final ObjectMapper JSON = new ObjectMapper();

    private final Map<String, DittoBridgeConfig.AlarmPriorityName> severityMap;

    public DittoSignalMapper(DittoBridgeConfig cfg) {
        this.severityMap = Objects.requireNonNull(cfg, "cfg").severityMap();
    }

    /**
     * Build a typed signal from a property value already extracted from a
     * Ditto twin event (or REST poll).
     *
     * @param binding read binding for which the value was observed
     * @param value   raw JSON node for the property; {@code null} → no signal
     * @param tsMs   wall-clock timestamp from the source (10-DITTO §0/00-FRAMEWORK §0.6)
     * @param twinOffline whether the bridge has flagged the thing as offline; flips quality to {@code UNCERTAIN}
     */
    public IInputSignal toSignal(DittoBridgeConfig.ReadBindingConfig binding,
                                 JsonNode value,
                                 long tsMs,
                                 boolean twinOffline) {
        if (binding == null || value == null || value.isMissingNode() || value.isNull()) return null;
        String tag = (binding.signalTag() == null || binding.signalTag().isBlank())
                ? DittoFeatureBinding.defaultTag(binding.thingId(), binding.feature(), binding.property())
                : binding.signalTag();

        if (binding.signalKind() == DittoBridgeConfig.ReadSignalKind.ALARM) {
            AlarmPriority p = AlarmPriority.JOURNAL;
            DittoBridgeConfig.AlarmPriorityName mapped = severityMap.get(binding.feature());
            if (mapped != null) p = AlarmPriority.valueOf(mapped.name());
            boolean active = value.isBoolean() ? value.asBoolean()
                    : value.isNumber() ? value.asDouble() != 0.0 : false;
            return new AlarmSignal(p, tag, active ? "ALARM_ACTIVE" : "ALARM_NORMAL", tsMs);
        }

        if (!value.isNumber()) return null;
        Quality q = twinOffline ? Quality.UNCERTAIN : Quality.GOOD;
        return new MeasurementSignal(tag, value.asDouble(), q, tsMs);
    }

    /**
     * Build a Ditto protocol envelope for a feature-property modify command.
     * The transport layer dispatches this either via WebSocket or via REST
     * {@code PUT /things/<thingId>/features/<feature>/properties/<property>};
     * the JSON body is identical.
     */
    public byte[] encodeFeaturePropertyCommand(String thingId, String feature,
                                               String property, double value, long ts) {
        Map<String, Object> headers = new LinkedHashMap<>();
        headers.put("response-required", false);
        headers.put("content-type", "application/json");

        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("topic",
                topicForModify(thingId, feature, property));
        envelope.put("headers", headers);
        envelope.put("path", "/features/" + feature + "/properties/" + property);
        envelope.put("value", value);
        envelope.put("timestamp", Instant.ofEpochMilli(ts).toString());
        try {
            return JSON.writeValueAsBytes(envelope);
        } catch (Exception ex) {
            throw new SignalMapperException("Ditto encode failed: " + ex.getMessage(), ex);
        }
    }

    /** REST-style PUT body for the property — a plain JSON scalar. */
    public byte[] encodeFeaturePropertyRestBody(double value) {
        try {
            return JSON.writeValueAsBytes(value);
        } catch (Exception ex) {
            throw new SignalMapperException("Ditto encode failed: " + ex.getMessage(), ex);
        }
    }

    /** Decode a JSON payload (twin event, REST response). */
    public JsonNode decode(byte[] payload) {
        if (payload == null || payload.length == 0) return null;
        try {
            return JSON.readTree(payload);
        } catch (IOException ex) {
            throw new SignalMapperException("Ditto decode failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * Pull a feature-property value out of a Ditto protocol envelope or a
     * raw REST response. Honours both the {@code value} envelope shape
     * (twin events) and a plain {@code {feature: {properties: {prop: x}}}}
     * tree (REST snapshot).
     */
    public JsonNode extractPropertyValue(JsonNode root, String feature, String property) {
        if (root == null) return null;
        // twin event envelope: { value: <scalar> } when path is the property,
        // or { value: {feature: {properties: {prop: x}}} } when path is /
        JsonNode value = root.get("value");
        if (value != null) {
            if (value.isNumber() || value.isBoolean() || value.isTextual() || value.isNull()) {
                return value;
            }
            JsonNode f = value.get(feature);
            if (f != null) {
                JsonNode props = f.get("properties");
                if (props != null) return props.get(property);
            }
        }
        // REST snapshot — the body itself is the property (PUT /properties/<p>) or
        // the whole feature object (GET /features/<f>).
        if (root.isNumber() || root.isBoolean()) return root;
        JsonNode props = root.get("properties");
        if (props != null) return props.get(property);
        return null;
    }

    /**
     * Pull the source-system timestamp out of a Ditto envelope. Falls back to
     * the supplied wall-clock when the envelope carries none (00-FRAMEWORK §0.6).
     */
    public long pickTimestamp(JsonNode root, long fallbackMs) {
        if (root == null) return fallbackMs;
        JsonNode ts = root.get("timestamp");
        if (ts == null) {
            JsonNode headers = root.get("headers");
            if (headers != null) ts = headers.get("timestamp");
        }
        if (ts == null || ts.isNull()) return fallbackMs;
        if (ts.isNumber()) return ts.asLong();
        if (ts.isTextual()) {
            try {
                return Instant.parse(ts.asText()).toEpochMilli();
            } catch (DateTimeParseException ignored) {
                return fallbackMs;
            }
        }
        return fallbackMs;
    }

    /** Ditto protocol topic for a modify-feature-property command. */
    public static String topicForModify(String thingId, String feature, String property) {
        // thingId namespace:name → namespace/name in the topic per Ditto Protocol.
        String[] parts = thingId.split(":", 2);
        String namespace = parts.length == 2 ? parts[0] : "_";
        String name = parts.length == 2 ? parts[1] : thingId;
        return namespace + "/" + name + "/things/twin/commands/modify";
    }

    /** Wraps Jackson exceptions so the caller can audit uniformly. */
    public static final class SignalMapperException extends RuntimeException {
        public SignalMapperException(String msg, Throwable cause) { super(msg, cause); }
    }
}
