/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.integration.nengo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.Map;

/**
 * Jneopallium → Nengo frame (15-NENGO.md §4).
 *
 * <p>Same envelope as {@link NengoInputFrame} but with {@code source} fixed
 * to {@code JNEOPALLIUM_OUTPUT}. The output bridge is responsible for
 * setting {@code valid_until_ms = timestamp_ms + validForMs} so the Nengo
 * smoothing decoder can decay to zero on staleness (§9.3).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "schema_version", "source", "frame_id", "sequence_no",
        "timestamp_ms", "valid_until_ms", "safety_status", "values",
        "transparency_log_id"
})
public record NengoOutputFrame(
        @JsonProperty("schema_version")      String schemaVersion,
        @JsonProperty("source")              String source,
        @JsonProperty("frame_id")            String frameId,
        @JsonProperty("sequence_no")         Long sequenceNo,
        @JsonProperty("timestamp_ms")        Long timestampMs,
        @JsonProperty("valid_until_ms")      Long validUntilMs,
        @JsonProperty("safety_status")       String safetyStatus,
        @JsonProperty("values")              Map<String, Double> values,
        @JsonProperty("transparency_log_id") String transparencyLogId
) {

    public static final String SCHEMA_VERSION = "1";
    public static final String SOURCE = "JNEOPALLIUM_OUTPUT";

    public static final String STATUS_OK       = "OK";
    public static final String STATUS_DEGRADED = "DEGRADED";
    public static final String STATUS_STOP     = "STOP";

    @JsonCreator
    public static NengoOutputFrame of(
            @JsonProperty("schema_version")      String schemaVersion,
            @JsonProperty("source")              String source,
            @JsonProperty("frame_id")            String frameId,
            @JsonProperty("sequence_no")         Long sequenceNo,
            @JsonProperty("timestamp_ms")        Long timestampMs,
            @JsonProperty("valid_until_ms")      Long validUntilMs,
            @JsonProperty("safety_status")       String safetyStatus,
            @JsonProperty("values")              Map<String, Double> values,
            @JsonProperty("transparency_log_id") String transparencyLogId) {
        return new NengoOutputFrame(
                schemaVersion, source, frameId, sequenceNo,
                timestampMs, validUntilMs, safetyStatus, values, transparencyLogId);
    }
}
