/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.kafka.decoder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.SyscallSignal;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Decoder for osquery scheduled-query results carrying syscall data
 * (08-KAFKA.md §4 row {@code host.syscalls.osquery}).
 *
 * <p>osquery records have a {@code columns} object whose schema depends on
 * the source table. This decoder targets the {@code process_events} table
 * (and equivalents) and pulls {@code syscall}, {@code pid}, {@code path},
 * {@code cmdline} → {@link SyscallSignal}.
 */
public final class OsqueryDecoder implements PayloadDecoder {

    public static final String NAME = "OSQUERY";

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
        JsonNode cols = root.get("columns");
        if (cols == null || !cols.isObject()) {
            // Some osquery emitters use snapshot-style records keyed at root.
            cols = root;
        }

        int syscall = parseInt(cols.get("syscall"));
        int pid = parseInt(cols.get("pid"));
        String proc = textOr(cols, "path", "cmdline", "process_name");

        long[] args = parseArgs(cols);

        SyscallSignal sig = new SyscallSignal(syscall, pid, proc, args);
        if (signalTagPrefix != null && !signalTagPrefix.isEmpty()) sig.setName(signalTagPrefix);
        return List.of(sig);
    }

    @Override public String name() { return NAME; }

    private static int parseInt(JsonNode n) {
        if (n == null) return 0;
        if (n.isNumber()) return n.asInt();
        if (n.isTextual()) {
            try { return Integer.parseInt(n.asText()); } catch (NumberFormatException ignored) {}
        }
        return 0;
    }

    private static String textOr(JsonNode n, String... keys) {
        for (String k : keys) {
            JsonNode v = n.get(k);
            if (v != null && v.isTextual()) return v.asText();
        }
        return null;
    }

    private static long[] parseArgs(JsonNode cols) {
        JsonNode args = cols.get("args");
        if (args == null) return null;
        if (args.isArray()) {
            List<Long> tmp = new ArrayList<>(args.size());
            for (JsonNode a : args) {
                if (a.isNumber()) tmp.add(a.asLong());
                else if (a.isTextual()) {
                    try { tmp.add(Long.parseLong(a.asText())); }
                    catch (NumberFormatException ignored) { /* skip non-numeric arg */ }
                }
            }
            long[] out = new long[tmp.size()];
            for (int i = 0; i < out.length; i++) out[i] = tmp.get(i);
            return out;
        }
        return null;
    }
}
