/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.kafka.decoder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.security.ThreatCategory;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.SignatureMatchSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.ThreatHypothesisSignal;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Decoder for Suricata EVE-JSON alerts (08-KAFKA.md §4 row
 * {@code net.suricata.alert}).
 *
 * <p>Each {@code event_type=alert} record yields one
 * {@link SignatureMatchSignal} (always) plus a {@link ThreatHypothesisSignal}
 * when the alert's severity warrants escalation (severity {@code <= 2} on
 * Suricata's 1-3 scale). Other event types are dropped silently — they are
 * valid Suricata records, just not threat signals.
 *
 * @see <a href="https://docs.suricata.io/en/latest/output/eve/eve-json-format.html">Suricata EVE</a>
 */
public final class SuricataEveDecoder implements PayloadDecoder {

    public static final String NAME = "SURICATA_EVE";

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
        JsonNode type = root.get("event_type");
        if (type == null || !type.isTextual() || !"alert".equals(type.asText())) {
            // Not an alert — valid input but not a signal source.
            return List.of();
        }

        JsonNode alert = root.get("alert");
        if (alert == null || !alert.isObject()) {
            throw new DecoderException("alert record on topic=" + topic + " missing alert object");
        }

        String sigId = textOrNull(alert, "signature_id");
        String family = textOrNull(alert, "category");
        String signature = textOrNull(alert, "signature");
        int severity = alert.path("severity").asInt(3);

        // Suricata's severity is 1 (high) → 3 (low). Confidence is an inverse map.
        double confidence = switch (severity) {
            case 1 -> 0.9;
            case 2 -> 0.7;
            case 3 -> 0.4;
            default -> 0.4;
        };

        SignatureMatchSignal sm = new SignatureMatchSignal(
                sigId, family, confidence, signature);
        if (signalTagPrefix != null && !signalTagPrefix.isEmpty()) sm.setName(signalTagPrefix);

        List<IInputSignal> out = new ArrayList<>(2);
        out.add(sm);

        if (severity <= 2) {
            ThreatCategory cat = mapCategory(family);
            ThreatHypothesisSignal th = new ThreatHypothesisSignal(
                    "suricata-" + (sigId == null ? "unk" : sigId),
                    cat,
                    confidence,
                    sigId == null ? List.of() : List.of(sigId));
            if (signalTagPrefix != null && !signalTagPrefix.isEmpty()) th.setName(signalTagPrefix);
            out.add(th);
        }
        return out;
    }

    @Override public String name() { return NAME; }

    /* ===== helpers ========================================================= */

    private static String textOrNull(JsonNode n, String f) {
        JsonNode v = n.get(f);
        if (v == null || v.isNull()) return null;
        if (v.isNumber()) return v.asText();
        return v.isTextual() ? v.asText() : null;
    }

    /**
     * Best-effort mapping of Suricata category strings to MITRE ATT&amp;CK
     * tactics. Unknown categories fall through to {@link ThreatCategory#UNKNOWN};
     * this keeps the bridge usable on any rule pack.
     */
    private static ThreatCategory mapCategory(String family) {
        if (family == null) return ThreatCategory.UNKNOWN;
        String f = family.toLowerCase();
        if (f.contains("trojan") || f.contains("malware")) return ThreatCategory.EXECUTION;
        if (f.contains("scan") || f.contains("recon")) return ThreatCategory.RECONNAISSANCE;
        if (f.contains("exploit")) return ThreatCategory.INITIAL_ACCESS;
        if (f.contains("c2") || f.contains("command")) return ThreatCategory.COMMAND_AND_CONTROL;
        if (f.contains("exfil")) return ThreatCategory.EXFILTRATION;
        if (f.contains("dos") || f.contains("denial")) return ThreatCategory.IMPACT;
        return ThreatCategory.UNKNOWN;
    }
}
