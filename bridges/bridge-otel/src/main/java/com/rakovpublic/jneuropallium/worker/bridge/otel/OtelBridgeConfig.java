/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.otel;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Top-level configuration for the OpenTelemetry bridge (09-OPENTELEMETRY.md §6).
 *
 * <p>Loaded from YAML at startup via {@link OtelBridgeConfigLoader}. Immutable.
 *
 * <p>The OTel bridge is <strong>export-only</strong> (§3) — there is no input
 * surface, no aggregator, no read API. This config therefore only describes
 * how observability data leaves the worker.
 */
public record OtelBridgeConfig(
        String serviceName,
        String serviceVersion,
        String serviceInstanceId,
        ExporterConfig exporter,
        Map<String, String> resourceAttributes,
        MetricsConfig metrics,
        TracesConfig traces,
        LogsConfig logs,
        RedactionConfig redaction
) {
    public OtelBridgeConfig {
        Objects.requireNonNull(serviceName, "serviceName");
        Objects.requireNonNull(exporter, "exporter");
        serviceVersion = serviceVersion == null ? "unknown" : serviceVersion;
        serviceInstanceId = serviceInstanceId == null ? "unknown" : serviceInstanceId;
        resourceAttributes = resourceAttributes == null ? Map.of() : Map.copyOf(resourceAttributes);
        metrics = metrics == null ? MetricsConfig.defaults() : metrics;
        traces = traces == null ? TracesConfig.defaults() : traces;
        logs = logs == null ? LogsConfig.defaults() : logs;
        redaction = redaction == null ? RedactionConfig.defaults() : redaction;
    }

    /** §6 — exporter wiring. */
    public record ExporterConfig(
            ExporterType type,
            String endpoint,
            long timeoutMs,
            Map<String, String> headers
    ) {
        public ExporterConfig {
            Objects.requireNonNull(type, "type");
            if (timeoutMs <= 0) timeoutMs = 10_000L;
            headers = headers == null ? Map.of() : Map.copyOf(headers);
        }

        /**
         * Exporter transport (§6). {@code NONE} disables export entirely
         * (the SDK is still wired so {@link OtelInstrumentation} is active,
         * but spans/metrics/logs are dropped before leaving the process —
         * useful for benchmarks and tests).
         */
        public enum ExporterType {
            OTLP_GRPC,
            OTLP_HTTP,
            PROMETHEUS_PUSH,
            NONE
        }
    }

    /** §6 — metrics knobs. */
    public record MetricsConfig(
            boolean enabled,
            long intervalMs
    ) {
        public MetricsConfig {
            if (intervalMs <= 0) intervalMs = 10_000L;
        }
        public static MetricsConfig defaults() { return new MetricsConfig(true, 10_000L); }
    }

    /** §6 — tracing knobs. */
    public record TracesConfig(
            boolean enabled,
            double samplerRatio
    ) {
        public TracesConfig {
            if (samplerRatio < 0.0 || samplerRatio > 1.0) {
                throw new IllegalArgumentException("samplerRatio must be in [0,1]: " + samplerRatio);
            }
        }
        public static TracesConfig defaults() { return new TracesConfig(true, 0.1); }
    }

    /** §6 — log export knobs. */
    public record LogsConfig(
            boolean enabled
    ) {
        public static LogsConfig defaults() { return new LogsConfig(true); }
    }

    /**
     * §6 / §10 R2 — sensitive-data scrubbing. Applied to attribute values and
     * log bodies before they leave the SDK.
     */
    public record RedactionConfig(
            @JsonProperty("redactSignalTags") boolean redactSignalTags,
            @JsonProperty("redactPatterns") List<String> redactPatterns
    ) {
        public RedactionConfig {
            redactPatterns = redactPatterns == null ? List.of() : List.copyOf(redactPatterns);
        }
        public static RedactionConfig defaults() { return new RedactionConfig(false, List.of()); }
    }
}
