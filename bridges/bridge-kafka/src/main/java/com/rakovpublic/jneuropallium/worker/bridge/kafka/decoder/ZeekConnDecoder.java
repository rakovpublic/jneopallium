/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.kafka.decoder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.security.NetworkTuple;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.PacketSignal;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Decoder for Zeek conn-log JSON records (08-KAFKA.md §4 row
 * {@code net.zeek.conn}).
 *
 * <p>Maps the standard Zeek conn fields ({@code id.orig_h}, {@code id.resp_h},
 * {@code id.orig_p}, {@code id.resp_p}, {@code proto}, {@code uid}, plus
 * traffic counters) onto a flow-summary {@link PacketSignal}. The signal's
 * {@code summary} field carries a UTF-8 truncated JSON of the record so the
 * pipeline does not have to re-parse it.
 *
 * @see <a href="https://docs.zeek.org/en/master/logs/conn.html">Zeek conn log</a>
 */
public final class ZeekConnDecoder implements PayloadDecoder {

    public static final String NAME = "ZEEK_CONN";

    /** Cap the kept summary so a chatty packet log doesn't bloat memory. */
    public static final int SUMMARY_MAX_BYTES = 2048;

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

        String src = textValue(root, "id.orig_h", "src_ip");
        String dst = textValue(root, "id.resp_h", "dst_ip");
        String proto = textValue(root, "proto", "protocol");
        int srcPort = intValue(root, "id.orig_p", "src_port");
        int dstPort = intValue(root, "id.resp_p", "dst_port");
        long ts = timestampMillis(root);

        if (src == null || dst == null) {
            throw new DecoderException(
                    "Zeek record missing required tuple fields (id.orig_h / id.resp_h) on topic=" + topic);
        }

        NetworkTuple tuple = new NetworkTuple(src, dst, proto, srcPort, dstPort);
        byte[] summary = truncate(value, SUMMARY_MAX_BYTES);
        PacketSignal sig = new PacketSignal(summary, tuple, ts);
        if (signalTagPrefix != null && !signalTagPrefix.isEmpty()) {
            sig.setName(signalTagPrefix);
        }
        return List.of(sig);
    }

    @Override public String name() { return NAME; }

    /* ===== helpers ========================================================= */

    private static String textValue(JsonNode root, String dotted, String alt) {
        JsonNode n = traverse(root, dotted);
        if (n != null && n.isTextual()) return n.asText();
        if (alt != null) {
            JsonNode a = root.get(alt);
            if (a != null && a.isTextual()) return a.asText();
        }
        return null;
    }

    private static int intValue(JsonNode root, String dotted, String alt) {
        JsonNode n = traverse(root, dotted);
        if (n != null && n.isNumber()) return n.asInt();
        if (alt != null) {
            JsonNode a = root.get(alt);
            if (a != null && a.isNumber()) return a.asInt();
        }
        return 0;
    }

    private static JsonNode traverse(JsonNode root, String dotted) {
        JsonNode cur = root;
        for (String part : dotted.split("\\.")) {
            if (cur == null) return null;
            JsonNode next = cur.get(part);
            // Zeek often flattens id.orig_h etc into a single field name —
            // try the literal key as a fallback at the root.
            if (next == null && cur == root) {
                next = root.get(dotted);
                return next;
            }
            cur = next;
        }
        return cur;
    }

    private static long timestampMillis(JsonNode root) {
        JsonNode ts = root.get("ts");
        if (ts != null && ts.isNumber()) {
            // Zeek timestamps are seconds since epoch with millisecond precision.
            return Math.round(ts.asDouble() * 1000.0);
        }
        return System.currentTimeMillis();
    }

    private static byte[] truncate(byte[] value, int max) {
        if (value.length <= max) return value.clone();
        byte[] out = new byte[max];
        System.arraycopy(value, 0, out, 0, max);
        return out;
    }
}
