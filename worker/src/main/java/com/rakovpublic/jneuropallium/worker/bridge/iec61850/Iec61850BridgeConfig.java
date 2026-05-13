/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.iec61850;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Top-level IEC 61850 bridge configuration (11-IEC61850.md §6).
 *
 * <p>The bridge ceiling is structurally <b>READ-ONLY — initially</b>
 * (11-IEC61850.md §0 safety ceiling, §3 "Why read-only", §4 diagram,
 * §7 "No aggregator class"). The structural defences are:
 *
 * <ol>
 *   <li>The bridge package contains no aggregator or output class
 *       (11-IEC61850.md §7), so there is no surface a higher layer could
 *       call to push data back to an IED.</li>
 *   <li>The {@link Iec61850MmsClient} seam exposes only {@code readDa()},
 *       {@code subscribeReport()} and connection lifecycle — no method
 *       writes a Data Attribute, issues a select-before-operate, or
 *       controls a breaker.</li>
 *   <li>{@code writes:} blocks are rejected at config-load with a clear
 *       message (11-IEC61850.md §6 "{@code writes:} block is rejected at
 *       config-load with a clear message").</li>
 * </ol>
 */
public record Iec61850BridgeConfig(
        List<IedConfig> ied,
        List<DaReadConfig> reads,
        List<ReportEventConfig> events,
        AuditConfig audit,
        Duration tickInterval
) {

    public Iec61850BridgeConfig {
        Objects.requireNonNull(audit, "audit");
        ied = ied == null ? List.of() : List.copyOf(ied);
        reads = reads == null ? List.of() : List.copyOf(reads);
        events = events == null ? List.of() : List.copyOf(events);
        tickInterval = tickInterval == null ? Duration.ofMillis(1000) : tickInterval;

        Set<String> seenIeds = new LinkedHashSet<>();
        for (IedConfig i : ied) {
            if (!seenIeds.add(i.id())) {
                throw new IllegalArgumentException(
                        "IEC 61850 bridge: duplicate IED id: " + i.id());
            }
        }
        Set<String> seenBindings = new LinkedHashSet<>();
        for (DaReadConfig r : reads) {
            if (!seenBindings.add(r.bindingId())) {
                throw new IllegalArgumentException(
                        "IEC 61850 bridge: duplicate bindingId: " + r.bindingId());
            }
            if (!seenIeds.contains(r.iedId())) {
                throw new IllegalArgumentException(
                        "IEC 61850 bridge: read binding '" + r.bindingId()
                                + "' references unknown iedId '" + r.iedId() + "'");
            }
        }
        for (ReportEventConfig e : events) {
            if (!seenBindings.add(e.bindingId())) {
                throw new IllegalArgumentException(
                        "IEC 61850 bridge: duplicate bindingId: " + e.bindingId());
            }
            if (!seenIeds.contains(e.iedId())) {
                throw new IllegalArgumentException(
                        "IEC 61850 bridge: event binding '" + e.bindingId()
                                + "' references unknown iedId '" + e.iedId() + "'");
            }
        }
    }

    @JsonCreator
    public static Iec61850BridgeConfig create(
            @JsonProperty("ied") List<IedConfig> ied,
            @JsonProperty("reads") List<DaReadConfig> reads,
            @JsonProperty("events") List<ReportEventConfig> events,
            @JsonProperty("audit") AuditConfig audit,
            @JsonProperty("tickInterval") Duration tickInterval,
            // 11-IEC61850.md §6 — reject a writes: block at load time. Declared so
            // Jackson surfaces it with FAIL_ON_UNKNOWN_PROPERTIES=true and rejects
            // with the clear message mandated by the spec.
            @JsonProperty("writes") Object writes) {
        if (writes != null) {
            throw new IllegalArgumentException(
                    "IEC 61850 bridge: 'writes:' block is not permitted — the bridge "
                            + "ceiling is READ-ONLY (11-IEC61850.md §0, §3, §6). "
                            + "Control surface is a separate bridge ('iec61850-control') "
                            + "that must be certified separately. Remove the writes section.");
        }
        return new Iec61850BridgeConfig(ied, reads, events, audit, tickInterval);
    }

    /** Resolve an IED by id, throwing a clear error if absent. */
    public IedConfig requireIed(String id) {
        for (IedConfig i : ied) {
            if (i.id().equals(id)) return i;
        }
        throw new IllegalArgumentException(
                "IEC 61850 bridge: no IED configured with id '" + id + "'");
    }

    /** Target Jneopallium signal kind for a read binding (§5 mapping table). */
    public enum TargetSignal {
        /** MMXU-style measurement → {@code MeasurementSignal}. */
        MEASUREMENT,
        /** XCBR/XSWI status → {@code InterlockSignal} (state-only, never tripped). */
        STATUS,
        /** Protection / supervisory event → {@code AlarmSignal}. */
        ALARM
    }

    /** §6 {@code ied:} entry. */
    public record IedConfig(
            String id,
            String host,
            int port,
            String sclFile,
            String reportControlBlock
    ) {
        public IedConfig {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(host, "host");
            Objects.requireNonNull(sclFile, "sclFile");
            if (port <= 0 || port > 65535) {
                throw new IllegalArgumentException(
                        "IEC 61850 bridge: ied.port out of range: " + port);
            }
        }

        @JsonCreator
        public static IedConfig of(
                @JsonProperty("id") String id,
                @JsonProperty("host") String host,
                @JsonProperty("port") Integer port,
                @JsonProperty("sclFile") String sclFile,
                @JsonProperty("reportControlBlock") String reportControlBlock) {
            return new IedConfig(id, host, port == null ? 102 : port,
                    sclFile, reportControlBlock);
        }
    }

    /** §6 {@code reads:} entry — direct DA path read binding. */
    public record DaReadConfig(
            String bindingId,
            String iedId,
            String daPath,
            String signalTag,
            TargetSignal targetSignal
    ) {
        public DaReadConfig {
            Objects.requireNonNull(bindingId, "bindingId");
            Objects.requireNonNull(iedId, "iedId");
            Objects.requireNonNull(daPath, "daPath");
            Objects.requireNonNull(signalTag, "signalTag");
            if (targetSignal == null) targetSignal = inferTargetFromPath(daPath);
        }

        @JsonCreator
        public static DaReadConfig of(
                @JsonProperty("bindingId") String bindingId,
                @JsonProperty("iedId") String iedId,
                @JsonProperty("daPath") String daPath,
                @JsonProperty("signalTag") String signalTag,
                @JsonProperty("targetSignal") TargetSignal targetSignal) {
            return new DaReadConfig(bindingId, iedId, daPath, signalTag, targetSignal);
        }

        /**
         * Infer the target signal from the DA path Logical Node prefix (§5).
         * MMXU/MMTR/MSQI → measurement; XCBR/XSWI → status; others default to
         * measurement.
         */
        private static TargetSignal inferTargetFromPath(String daPath) {
            String upper = daPath.toUpperCase();
            if (upper.contains("XCBR") || upper.contains("XSWI")) return TargetSignal.STATUS;
            return TargetSignal.MEASUREMENT;
        }
    }

    /** §6 {@code events:} entry — Report Control Block subscription. */
    public record ReportEventConfig(
            String bindingId,
            String iedId,
            String reportControlBlock,
            String targetSignal,
            Map<String, String> severityMap
    ) {
        public ReportEventConfig {
            Objects.requireNonNull(bindingId, "bindingId");
            Objects.requireNonNull(iedId, "iedId");
            Objects.requireNonNull(reportControlBlock, "reportControlBlock");
            if (targetSignal == null) targetSignal = "ALARM";
            severityMap = severityMap == null ? Map.of() : Map.copyOf(severityMap);
        }

        @JsonCreator
        public static ReportEventConfig of(
                @JsonProperty("bindingId") String bindingId,
                @JsonProperty("iedId") String iedId,
                @JsonProperty("reportControlBlock") String reportControlBlock,
                @JsonProperty("targetSignal") String targetSignal,
                @JsonProperty("severityMap") Map<String, String> severityMap) {
            // Preserve YAML ordering for deterministic auditing.
            Map<String, String> ordered = severityMap == null
                    ? Map.of() : new LinkedHashMap<>(severityMap);
            return new ReportEventConfig(bindingId, iedId, reportControlBlock,
                    targetSignal, ordered);
        }
    }

    /** §6 {@code audit:} block. */
    public record AuditConfig(String localAuditFile) {
        public AuditConfig {
            Objects.requireNonNull(localAuditFile, "localAuditFile");
        }

        @JsonCreator
        public static AuditConfig of(@JsonProperty("localAuditFile") String localAuditFile) {
            return new AuditConfig(localAuditFile);
        }
    }
}
