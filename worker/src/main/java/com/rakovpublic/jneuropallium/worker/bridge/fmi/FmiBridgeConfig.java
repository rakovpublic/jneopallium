/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.fmi;

import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeSafetyMode;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Top-level configuration for the FMI/FMU bridge (03-FMI-FMU.md §6).
 *
 * <p>Loaded from YAML at startup via {@link FmiBridgeConfigLoader}. Immutable.
 */
public record FmiBridgeConfig(
        FmuConfig fmu,
        ClockConfig clock,
        List<ReadBindingConfig> reads,
        List<WriteBindingConfig> writes,
        List<EventBindingConfig> events,
        AuditConfig audit,
        Map<String, BridgeSafetyMode> perTagSafetyMode
) {
    public FmiBridgeConfig {
        Objects.requireNonNull(fmu, "fmu");
        Objects.requireNonNull(clock, "clock");
        reads = reads == null ? List.of() : List.copyOf(reads);
        writes = writes == null ? List.of() : List.copyOf(writes);
        events = events == null ? List.of() : List.copyOf(events);
        perTagSafetyMode = perTagSafetyMode == null ? Map.of() : Map.copyOf(perTagSafetyMode);
    }

    public record FmuConfig(
            String path,
            boolean loggingOn,
            boolean toleranceDefined,
            double tolerance
    ) {
        public FmuConfig {
            Objects.requireNonNull(path, "path");
            if (tolerance < 0) throw new IllegalArgumentException("tolerance must be >= 0");
        }
    }

    public record ClockConfig(
            ClockMode mode,
            double startTime,
            double stepSize
    ) {
        public ClockConfig {
            mode = mode == null ? ClockMode.AS_FAST_AS_POSSIBLE : mode;
            if (stepSize <= 0) throw new IllegalArgumentException("stepSize must be > 0");
        }

        public enum ClockMode { REAL_TIME, AS_FAST_AS_POSSIBLE }
    }

    /** Mapping of one FMU Real output to a MeasurementSignal. */
    public record ReadBindingConfig(
            String bindingId,
            String fmuVariable,
            String signalTag
    ) {
        public ReadBindingConfig {
            Objects.requireNonNull(bindingId, "bindingId");
            Objects.requireNonNull(fmuVariable, "fmuVariable");
            Objects.requireNonNull(signalTag, "signalTag");
        }
    }

    /** Mapping of one FMU Real input driven by an ActuatorCommandSignal. */
    public record WriteBindingConfig(
            String bindingId,
            String fmuVariable,
            String signalTag,
            Double failSafeValue,
            Double minClampValue,
            Double maxClampValue,
            Double rampRateMaxPerSec
    ) {
        public WriteBindingConfig {
            Objects.requireNonNull(bindingId, "bindingId");
            Objects.requireNonNull(fmuVariable, "fmuVariable");
            Objects.requireNonNull(signalTag, "signalTag");
        }
    }

    /** Mapping of one FMU Boolean output to an AlarmSignal. */
    public record EventBindingConfig(
            String bindingId,
            String fmuVariable,
            String signalTag,
            String severity
    ) {
        public EventBindingConfig {
            Objects.requireNonNull(bindingId, "bindingId");
            Objects.requireNonNull(fmuVariable, "fmuVariable");
            Objects.requireNonNull(signalTag, "signalTag");
            severity = severity == null ? "HIGH" : severity;
        }
    }

    public record AuditConfig(
            String localAuditFile,
            boolean writeRejectedToAudit
    ) {
        public AuditConfig {
            Objects.requireNonNull(localAuditFile, "localAuditFile");
        }
    }
}
