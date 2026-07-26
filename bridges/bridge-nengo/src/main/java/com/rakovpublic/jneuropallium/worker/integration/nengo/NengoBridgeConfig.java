/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.integration.nengo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeSafetyMode;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Top-level Nengo integration configuration (15-NENGO.md §7).
 *
 * <p>Structural defences enforced at load time:
 *
 * <ol>
 *   <li>{@code AUTONOMOUS} per-tag safety mode is only permitted when
 *       {@code simulatorOnly: true} (15-NENGO.md §0, §11 S15).</li>
 *   <li>Binding IDs must be unique across {@code inputMappings} and across
 *       {@code outputMappings}.</li>
 *   <li>Unknown YAML properties fail the load (00-FRAMEWORK §3).</li>
 *   <li>Watchdog and frame-size values must be strictly positive.</li>
 * </ol>
 */
public record NengoBridgeConfig(
        TransportSection transport,
        boolean simulatorOnly,
        List<InputMapping> inputMappings,
        List<OutputMapping> outputMappings,
        WatchdogSection watchdog,
        AuditSection audit,
        Map<String, BridgeSafetyMode> perTagSafetyMode
) {

    public NengoBridgeConfig {
        Objects.requireNonNull(transport, "transport");
        Objects.requireNonNull(audit, "audit");
        inputMappings = inputMappings == null ? List.of() : List.copyOf(inputMappings);
        outputMappings = outputMappings == null ? List.of() : List.copyOf(outputMappings);
        watchdog = watchdog == null ? WatchdogSection.defaults() : watchdog;
        perTagSafetyMode = perTagSafetyMode == null ? Map.of() : Map.copyOf(perTagSafetyMode);

        Set<String> seenIn = new LinkedHashSet<>();
        for (InputMapping m : inputMappings) {
            if (!seenIn.add(m.frameLabel())) {
                throw new IllegalArgumentException(
                        "Nengo bridge: duplicate input frameLabel: " + m.frameLabel());
            }
        }
        Set<String> seenOut = new LinkedHashSet<>();
        for (OutputMapping m : outputMappings) {
            if (!seenOut.add(m.approvedSignalType())) {
                throw new IllegalArgumentException(
                        "Nengo bridge: duplicate output approvedSignalType: "
                                + m.approvedSignalType());
            }
        }

        for (Map.Entry<String, BridgeSafetyMode> e : perTagSafetyMode.entrySet()) {
            if (e.getValue() == BridgeSafetyMode.AUTONOMOUS && !simulatorOnly) {
                throw new IllegalArgumentException(
                        "Nengo bridge: perTagSafetyMode '" + e.getKey()
                                + "' declares AUTONOMOUS but simulatorOnly=false "
                                + "(15-NENGO.md §0 safety ceiling, §11 S15).");
            }
        }
    }

    @JsonCreator
    public static NengoBridgeConfig create(
            @JsonProperty("transport") TransportSection transport,
            @JsonProperty("simulatorOnly") Boolean simulatorOnly,
            @JsonProperty("inputMappings") List<InputMapping> inputMappings,
            @JsonProperty("outputMappings") List<OutputMapping> outputMappings,
            @JsonProperty("watchdog") WatchdogSection watchdog,
            @JsonProperty("audit") AuditSection audit,
            @JsonProperty("perTagSafetyMode") Map<String, BridgeSafetyMode> perTagSafetyMode) {
        return new NengoBridgeConfig(
                transport,
                simulatorOnly != null && simulatorOnly,
                inputMappings,
                outputMappings,
                watchdog,
                audit,
                perTagSafetyMode);
    }

    /** Channel transport — UDS for live demos, FILE for replay/CI. */
    public enum TransportMode { UDS, FILE }

    /** Target Jneopallium signal class for a labeled frame value. */
    public enum SignalKind {
        SENSORY, HARM_ASSESSMENT, EFFICIENCY, MEASUREMENT
    }

    /** §7 {@code transport:} block. */
    public record TransportSection(
            String channelInPath,
            String channelOutPath,
            TransportMode mode,
            long reconnectBackoffMs,
            long reconnectMaxMs,
            int frameMaxBytes
    ) {
        public static final long DEFAULT_BACKOFF_MS = 250L;
        public static final long DEFAULT_MAX_BACKOFF_MS = 5_000L;
        public static final int DEFAULT_FRAME_MAX_BYTES = 65_536;

        public TransportSection {
            Objects.requireNonNull(channelInPath, "channelInPath");
            Objects.requireNonNull(channelOutPath, "channelOutPath");
            mode = mode == null ? TransportMode.UDS : mode;
            if (reconnectBackoffMs <= 0) reconnectBackoffMs = DEFAULT_BACKOFF_MS;
            if (reconnectMaxMs <= 0) reconnectMaxMs = DEFAULT_MAX_BACKOFF_MS;
            if (frameMaxBytes <= 0) frameMaxBytes = DEFAULT_FRAME_MAX_BYTES;
            if (reconnectMaxMs < reconnectBackoffMs) {
                throw new IllegalArgumentException(
                        "Nengo bridge: transport.reconnectMaxMs (" + reconnectMaxMs
                                + ") must be >= reconnectBackoffMs (" + reconnectBackoffMs + ")");
            }
        }

        @JsonCreator
        public static TransportSection of(
                @JsonProperty("channelInPath") String channelInPath,
                @JsonProperty("channelOutPath") String channelOutPath,
                @JsonProperty("mode") TransportMode mode,
                @JsonProperty("reconnectBackoffMs") Long reconnectBackoffMs,
                @JsonProperty("reconnectMaxMs") Long reconnectMaxMs,
                @JsonProperty("frameMaxBytes") Integer frameMaxBytes) {
            return new TransportSection(
                    channelInPath, channelOutPath,
                    mode == null ? TransportMode.UDS : mode,
                    reconnectBackoffMs == null ? DEFAULT_BACKOFF_MS : reconnectBackoffMs,
                    reconnectMaxMs == null ? DEFAULT_MAX_BACKOFF_MS : reconnectMaxMs,
                    frameMaxBytes == null ? DEFAULT_FRAME_MAX_BYTES : frameMaxBytes);
        }
    }

    /** §7 {@code inputMappings:} entry — one frame label → one signal. */
    public record InputMapping(
            String frameLabel,
            SignalKind signal,
            String modality,
            String signalTag
    ) {
        public InputMapping {
            Objects.requireNonNull(frameLabel, "frameLabel");
            Objects.requireNonNull(signal, "signal");
            Objects.requireNonNull(signalTag, "signalTag");
        }

        @JsonCreator
        public static InputMapping of(
                @JsonProperty("frameLabel") String frameLabel,
                @JsonProperty("signal") SignalKind signal,
                @JsonProperty("modality") String modality,
                @JsonProperty("signalTag") String signalTag) {
            return new InputMapping(frameLabel, signal, modality, signalTag);
        }
    }

    /** §7 {@code outputMappings:} entry. */
    public record OutputMapping(
            String approvedSignalType,
            List<String> frameLabels,
            long validForMs,
            FailSafeFrame failSafeFrame
    ) {
        public OutputMapping {
            Objects.requireNonNull(approvedSignalType, "approvedSignalType");
            frameLabels = frameLabels == null ? List.of() : List.copyOf(frameLabels);
            if (validForMs <= 0) validForMs = 250L;
        }

        @JsonCreator
        public static OutputMapping of(
                @JsonProperty("approvedSignalType") String approvedSignalType,
                @JsonProperty("frameLabels") List<String> frameLabels,
                @JsonProperty("validForMs") Long validForMs,
                @JsonProperty("failSafeFrame") FailSafeFrame failSafeFrame) {
            return new OutputMapping(
                    approvedSignalType, frameLabels,
                    validForMs == null ? 250L : validForMs, failSafeFrame);
        }
    }

    /** §7 — fail-safe frame written on STOP / interlock / watchdog. */
    public record FailSafeFrame(
            String safety_status,
            Map<String, Double> values
    ) {
        public FailSafeFrame {
            safety_status = safety_status == null ? "STOP" : safety_status;
            values = values == null ? Map.of() : Map.copyOf(values);
        }

        @JsonCreator
        public static FailSafeFrame of(
                @JsonProperty("safety_status") String safety_status,
                @JsonProperty("values") Map<String, Double> values) {
            return new FailSafeFrame(safety_status, values);
        }
    }

    /** §7 {@code watchdog:} block. */
    public record WatchdogSection(
            long staleFrameMs,
            long outputDecayMs
    ) {
        public static WatchdogSection defaults() {
            return new WatchdogSection(250L, 250L);
        }

        public WatchdogSection {
            if (staleFrameMs <= 0) staleFrameMs = 250L;
            if (outputDecayMs <= 0) outputDecayMs = 250L;
        }

        @JsonCreator
        public static WatchdogSection of(
                @JsonProperty("staleFrameMs") Long staleFrameMs,
                @JsonProperty("outputDecayMs") Long outputDecayMs) {
            return new WatchdogSection(
                    staleFrameMs == null ? 250L : staleFrameMs,
                    outputDecayMs == null ? 250L : outputDecayMs);
        }
    }

    /** §7 {@code audit:} block. */
    public record AuditSection(
            String localAuditFile
    ) {
        public AuditSection {
            Objects.requireNonNull(localAuditFile, "localAuditFile");
        }

        @JsonCreator
        public static AuditSection of(
                @JsonProperty("localAuditFile") String localAuditFile) {
            return new AuditSection(localAuditFile);
        }
    }
}
