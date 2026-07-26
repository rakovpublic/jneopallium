/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.kafka.decoder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.security.LogLevel;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.LogEventSignal;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Decoder for Logstash-shaped JSON log records (08-KAFKA.md §4 row
 * {@code logs.security}).
 *
 * <p>Maps the canonical Logstash fields ({@code @timestamp}, {@code level},
 * {@code message}, {@code host}, {@code source}, plus arbitrary {@code fields.*}
 * sub-objects) onto a {@link LogEventSignal}.
 *
 * <p>Severity comes from {@code level} or {@code log.level}; values that do
 * not match a known {@link LogLevel} are normalised to {@link LogLevel#INFO}.
 */
public final class LogstashJsonDecoder implements PayloadDecoder {

    public static final String NAME = "LOGSTASH";

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public List<IInputSignal> decode(String topic, String key, byte[] value, String signalTagPrefix)
            throws DecoderException {
        if (value == null || value.length == 0) {
            throw new DecoderException("empty payload from topic=" + topic);
        }
        JsonNode root;
        try {
            root = mapper.readTree(new String(value, StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new DecoderException("malformed JSON on topic=" + topic, e);
        }
        if (!root.isObject()) {
            throw new DecoderException("expected JSON object on topic=" + topic);
        }

        String source = textOrDefault(root, "source", topic);
        long ts = parseTimestamp(root);
        LogLevel level = parseLevel(root);
        Map<String, String> fields = flattenLeafFields(root);
        if (signalTagPrefix != null && !signalTagPrefix.isEmpty()) {
            fields.put("_tag", signalTagPrefix);
        }
        if (key != null) fields.put("_key", key);

        LogEventSignal sig = new LogEventSignal(source, level, fields, ts);
        return List.of(sig);
    }

    @Override public String name() { return NAME; }

    /* ===== helpers ========================================================= */

    private static String textOrDefault(JsonNode root, String field, String def) {
        JsonNode n = root.get(field);
        return (n != null && n.isTextual()) ? n.asText() : def;
    }

    private static long parseTimestamp(JsonNode root) {
        JsonNode at = root.get("@timestamp");
        if (at != null && at.isNumber()) return at.asLong();
        if (at != null && at.isTextual()) {
            try { return java.time.Instant.parse(at.asText()).toEpochMilli(); }
            catch (RuntimeException ignored) { /* fall through */ }
        }
        JsonNode ts = root.get("timestamp");
        if (ts != null && ts.isNumber()) return ts.asLong();
        return System.currentTimeMillis();
    }

    private static LogLevel parseLevel(JsonNode root) {
        JsonNode l = root.get("level");
        if (l == null || l.isNull()) {
            JsonNode logNode = root.get("log");
            if (logNode != null && logNode.isObject()) l = logNode.get("level");
        }
        if (l == null || !l.isTextual()) return LogLevel.INFO;
        try { return LogLevel.valueOf(l.asText().toUpperCase()); }
        catch (IllegalArgumentException ignored) { return LogLevel.INFO; }
    }

    private static Map<String, String> flattenLeafFields(JsonNode root) {
        Map<String, String> out = new HashMap<>();
        flatten("", root, out);
        return out;
    }

    private static void flatten(String prefix, JsonNode node, Map<String, String> out) {
        if (node == null || node.isNull()) return;
        if (node.isValueNode()) {
            out.put(prefix.isEmpty() ? "value" : prefix, node.asText());
            return;
        }
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> it = node.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> e = it.next();
                String next = prefix.isEmpty() ? e.getKey() : prefix + "." + e.getKey();
                flatten(next, e.getValue(), out);
            }
        }
        // arrays: keep things simple — a single concatenated value
        if (node.isArray()) {
            out.put(prefix, node.toString());
        }
    }
}
