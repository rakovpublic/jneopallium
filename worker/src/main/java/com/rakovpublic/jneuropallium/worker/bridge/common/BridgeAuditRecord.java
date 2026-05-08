/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;
import java.util.Objects;

/**
 * Universal bridge audit record (00-FRAMEWORK §4). One JSON object per
 * line in the local audit JSONL file; same shape regardless of bridge so
 * SIEMs and historians can ingest a single schema.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "ts", "run", "bridge", "verdict", "loopId", "tag",
        "proposed", "effective", "reason", "safetyMode", "evidenceNeurons"
})
public record BridgeAuditRecord(
        long ts,
        long run,
        String bridge,
        Verdict verdict,
        String loopId,
        String tag,
        Double proposed,
        Double effective,
        String reason,
        BridgeSafetyMode safetyMode,
        List<String> evidenceNeurons
) {

    public BridgeAuditRecord {
        Objects.requireNonNull(bridge, "bridge");
        Objects.requireNonNull(verdict, "verdict");
        evidenceNeurons = evidenceNeurons == null ? List.of() : List.copyOf(evidenceNeurons);
    }

    /** §4 verdict vocabulary. */
    public enum Verdict {
        /** Write succeeded at the protocol layer. */
        APPLIED,
        /** Aggregator refused the write — see {@link BridgeAuditRecord#reason()}. */
        REJECTED,
        /** Fail-safe written because of a tripped interlock. */
        INTERLOCK_TRIP,
        /** Operator override active — nothing written. */
        OVERRIDE_HOLD,
        /** Protocol-level write returned non-good status or threw. */
        FAILED
    }

    /** §4 vocabulary for {@link BridgeAuditRecord#reason()} on REJECTED verdicts. */
    public static final class RejectReason {
        private RejectReason() {}
        public static final String UNKNOWN_TAG    = "UNKNOWN_TAG";
        public static final String INTERLOCK_HOLD = "INTERLOCK_HOLD";
        public static final String OVERRIDE_HOLD  = "OVERRIDE_HOLD";
        public static final String SHADOW_MODE    = "SHADOW_MODE";
        public static final String ADVISORY_HOLD  = "ADVISORY_HOLD";
        public static final String EXCEPTION      = "EXCEPTION";
    }

    /** §4 vocabulary for {@link BridgeAuditRecord#reason()} on APPLIED verdicts. */
    public static final class ModifyReason {
        private ModifyReason() {}
        public static final String RATE_LIMITED    = "RATE_LIMITED";
        public static final String CLAMPED_HIGH    = "CLAMPED_HIGH";
        public static final String CLAMPED_LOW     = "CLAMPED_LOW";
        public static final String DIFF_SUPPRESSED = "DIFF_SUPPRESSED";
    }
}
