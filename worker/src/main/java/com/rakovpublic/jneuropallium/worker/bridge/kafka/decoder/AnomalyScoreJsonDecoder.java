/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.kafka.decoder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.AnomalyScoreSignal;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Decoder for upstream anomaly-score JSON records (08-KAFKA.md §4 row
 * {@code endpoint.scores}).
 *
 * <p>Expects {@code {entityId, score, features:[...]}}; produces one
 * {@link AnomalyScoreSignal}. The Avro variant lives in
 * {@link AvroSchemaRegistryDecoder}, which delegates to this decoder once
 * the record has been deserialised to JSON.
 */
public final class AnomalyScoreJsonDecoder implements PayloadDecoder {

    public static final String NAME = "ANOMALY_JSON";

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public List<IInputSignal> decode(String topic, String key, byte[] value, String signalTagPrefix)
            throws DecoderException {
        if (value == null || value.length == 0) {
            throw new DecoderException("empty payload on topic=" + topic);
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

        String entity = root.path("entityId").asText(null);
        if (entity == null) entity = root.path("entity").asText(key);
        double score = root.path("score").asDouble(
                root.path("deviationScore").asDouble(0.0));

        List<String> features = new ArrayList<>();
        JsonNode fs = root.get("features");
        if (fs != null && fs.isArray()) {
            for (JsonNode f : fs) features.add(f.asText());
        }

        AnomalyScoreSignal sig = new AnomalyScoreSignal(entity, score, features);
        if (signalTagPrefix != null && !signalTagPrefix.isEmpty()) sig.setName(signalTagPrefix);
        return List.of(sig);
    }

    @Override public String name() { return NAME; }
}
