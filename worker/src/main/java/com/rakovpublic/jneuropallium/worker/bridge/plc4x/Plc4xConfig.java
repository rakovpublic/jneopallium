/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.plc4x;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeSafetyMode;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Top-level configuration for the PLC4X bridge (01-PLC4X.md §5).
 *
 * <p>Loaded from YAML at startup via {@link Plc4xConfigLoader}. Immutable.
 */
public record Plc4xConfig(
        List<ConnectionConfig> connections,
        List<ReadBindingConfig> reads,
        List<WriteBindingConfig> writes,
        List<EventBindingConfig> events,
        AuditConfig audit,
        Map<String, BridgeSafetyMode> perTagSafetyMode,
        Duration tickInterval
) {
    public Plc4xConfig {
        connections = connections == null ? List.of() : List.copyOf(connections);
        reads = reads == null ? List.of() : List.copyOf(reads);
        writes = writes == null ? List.of() : List.copyOf(writes);
        events = events == null ? List.of() : List.copyOf(events);
        perTagSafetyMode = perTagSafetyMode == null ? Map.of() : Map.copyOf(perTagSafetyMode);
        tickInterval = tickInterval == null ? Duration.ofMillis(250) : tickInterval;
    }

    /** One PLC connection — a {@code PlcConnection} in PLC4X parlance. */
    public record ConnectionConfig(
            String id,
            String connectionString,
            Duration requestTimeout,
            Duration keepAliveInterval
    ) {
        public ConnectionConfig {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(connectionString, "connectionString");
            requestTimeout = requestTimeout == null ? Duration.ofSeconds(5) : requestTimeout;
        }
    }

    /** One numeric/discrete tag polled into a {@code MeasurementSignal}. */
    public record ReadBindingConfig(
            String bindingId,
            String connectionId,
            String fieldAddress,
            String signalTag,
            long pollIntervalMs
    ) {
        public ReadBindingConfig {
            Objects.requireNonNull(bindingId, "bindingId");
            Objects.requireNonNull(connectionId, "connectionId");
            Objects.requireNonNull(fieldAddress, "fieldAddress");
            Objects.requireNonNull(signalTag, "signalTag");
            if (pollIntervalMs <= 0) {
                throw new IllegalArgumentException("pollIntervalMs must be > 0 for binding " + bindingId);
            }
        }
    }

    /** One actuating tag driven by an {@code ActuatorCommandSignal}. */
    public record WriteBindingConfig(
            String bindingId,
            String connectionId,
            String fieldAddress,
            String signalTag,
            Double failSafeValue,
            Double rampRateMaxPerSec,
            Double minClampValue,
            Double maxClampValue
    ) {
        public WriteBindingConfig {
            Objects.requireNonNull(bindingId, "bindingId");
            Objects.requireNonNull(connectionId, "connectionId");
            Objects.requireNonNull(fieldAddress, "fieldAddress");
            Objects.requireNonNull(signalTag, "signalTag");
        }
    }

    /**
     * One alarm/diagnostic word polled into one or more {@code AlarmSignal}s
     * (01-PLC4X.md §5: bit-decoded WORD with a per-bit severity map).
     *
     * <p>{@code severityMap} keys are hex-encoded bit masks (e.g. {@code 0x0001}).
     * The {@link Plc4xSignalMapper} matches each set bit in the polled value
     * against this map to produce one alarm signal per matching bit.
     */
    public record EventBindingConfig(
            String bindingId,
            String connectionId,
            String fieldAddress,
            String signalTag,
            long pollIntervalMs,
            Map<String, String> severityMap
    ) {
        public EventBindingConfig {
            Objects.requireNonNull(bindingId, "bindingId");
            Objects.requireNonNull(connectionId, "connectionId");
            Objects.requireNonNull(fieldAddress, "fieldAddress");
            Objects.requireNonNull(signalTag, "signalTag");
            if (pollIntervalMs <= 0) {
                throw new IllegalArgumentException("pollIntervalMs must be > 0 for event " + bindingId);
            }
            // Preserve insertion order so severity decoding is deterministic.
            severityMap = severityMap == null
                    ? Map.of()
                    : java.util.Collections.unmodifiableMap(new LinkedHashMap<>(severityMap));
        }
    }

    public record AuditConfig(
            String localAuditFile,
            boolean writeRejectedToAudit
    ) {
        public AuditConfig {
            Objects.requireNonNull(localAuditFile, "localAuditFile");
        }

        @JsonCreator
        public static AuditConfig of(
                @JsonProperty("localAuditFile") String localAuditFile,
                @JsonProperty("writeRejectedToAudit") Boolean writeRejectedToAudit) {
            return new AuditConfig(localAuditFile,
                    writeRejectedToAudit != null && writeRejectedToAudit);
        }
    }
}
