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
 * Nengo → Jneopallium frame (15-NENGO.md §4).
 *
 * <p>One-to-one JSON mapping. Required fields are not-null; rejection of
 * a frame with any required field missing or any non-finite number is
 * the receiver's responsibility (15-NENGO.md §4 last paragraph, S10).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "schema_version", "source", "frame_id", "sequence_no",
        "timestamp_ms", "valid_until_ms", "safety_status", "values",
        "transparency_log_id"
})
public record NengoInputFrame(
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
    public static final String SOURCE = "NENGO_INPUT";

    public static final String STATUS_OK       = "OK";
    public static final String STATUS_DEGRADED = "DEGRADED";
    public static final String STATUS_STOP     = "STOP";

    @JsonCreator
    public static NengoInputFrame of(
            @JsonProperty("schema_version")      String schemaVersion,
            @JsonProperty("source")              String source,
            @JsonProperty("frame_id")            String frameId,
            @JsonProperty("sequence_no")         Long sequenceNo,
            @JsonProperty("timestamp_ms")        Long timestampMs,
            @JsonProperty("valid_until_ms")      Long validUntilMs,
            @JsonProperty("safety_status")       String safetyStatus,
            @JsonProperty("values")              Map<String, Double> values,
            @JsonProperty("transparency_log_id") String transparencyLogId) {
        return new NengoInputFrame(
                schemaVersion, source, frameId, sequenceNo,
                timestampMs, validUntilMs, safetyStatus, values, transparencyLogId);
    }

    /**
     * Validate that all required fields are present and all numeric values
     * are finite. Returns {@code null} if the frame is valid, otherwise a
     * machine-friendly reason string suitable for an audit record.
     */
    public String validate() {
        if (schemaVersion == null) return "MISSING_FIELD:schema_version";
        if (!SCHEMA_VERSION.equals(schemaVersion))
            return "SCHEMA_VERSION_MISMATCH:" + schemaVersion;
        if (source == null) return "MISSING_FIELD:source";
        if (!SOURCE.equals(source)) return "BAD_SOURCE:" + source;
        if (frameId == null) return "MISSING_FIELD:frame_id";
        if (sequenceNo == null || sequenceNo < 0) return "BAD_FIELD:sequence_no";
        if (timestampMs == null || timestampMs < 0) return "BAD_FIELD:timestamp_ms";
        if (validUntilMs == null || validUntilMs < 0) return "BAD_FIELD:valid_until_ms";
        if (safetyStatus == null) return "MISSING_FIELD:safety_status";
        if (!STATUS_OK.equals(safetyStatus)
                && !STATUS_DEGRADED.equals(safetyStatus)
                && !STATUS_STOP.equals(safetyStatus)) {
            return "BAD_SAFETY_STATUS:" + safetyStatus;
        }
        if (values == null) return "MISSING_FIELD:values";
        for (Map.Entry<String, Double> e : values.entrySet()) {
            Double v = e.getValue();
            if (v == null || !Double.isFinite(v)) {
                return "NON_FINITE_VALUE:" + e.getKey();
            }
        }
        return null;
    }
}
