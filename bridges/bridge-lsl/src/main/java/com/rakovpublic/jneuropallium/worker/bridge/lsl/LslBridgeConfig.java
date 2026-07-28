/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.lsl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeSafetyMode;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Top-level Lab Streaming Layer bridge configuration (05-LSL.md §6).
 *
 * <p>Loaded from YAML through {@link LslBridgeConfigLoader} with
 * {@code FAIL_ON_UNKNOWN_PROPERTIES = true} per 00-FRAMEWORK §3.
 *
 * <p>The bridge ceiling is structurally <b>ADVISORY</b> (05-LSL.md §3, §11
 * row 05). Two structural defences enforce it:
 * <ol>
 *   <li>Every write binding's loop must declare a {@link BridgeSafetyMode}
 *       in {@link #perTagSafetyMode()}; the constructor rejects
 *       {@link BridgeSafetyMode#AUTONOMOUS} for any LSL write — the
 *       stimulator-driver software downstream of the {@code Jneopallium-Stim-Advisory}
 *       outlet is a separately certified component (§3, §11 R3 in the spec
 *       and 00-FRAMEWORK §11 ceiling row).</li>
 *   <li>{@link LslAdvisoryOutputAggregator} routes every
 *       {@code StimulationCommandSignal} through the existing
 *       {@code StimulationSafetyGateNeuron} contract and the universal
 *       §2.2 algorithm (clamp, ramp-limit, diff-suppress, audit).</li>
 * </ol>
 */
public record LslBridgeConfig(
        DiscoveryConfig discovery,
        List<ReadBindingConfig> reads,
        List<WriteBindingConfig> writes,
        AuditConfig audit,
        Map<String, BridgeSafetyMode> perTagSafetyMode,
        Duration tickInterval
) {

    public LslBridgeConfig {
        Objects.requireNonNull(discovery, "discovery");
        reads = reads == null ? List.of() : List.copyOf(reads);
        writes = writes == null ? List.of() : List.copyOf(writes);
        tickInterval = tickInterval == null ? Duration.ofMillis(250) : tickInterval;
        perTagSafetyMode = perTagSafetyMode == null ? Map.of() : Map.copyOf(perTagSafetyMode);

        Set<String> seen = new LinkedHashSet<>();
        for (ReadBindingConfig r : reads) {
            if (!seen.add(r.bindingId())) {
                throw new IllegalArgumentException("LSL bridge: duplicate bindingId: " + r.bindingId());
            }
        }
        for (WriteBindingConfig w : writes) {
            if (!seen.add(w.bindingId())) {
                throw new IllegalArgumentException("LSL bridge: duplicate bindingId: " + w.bindingId());
            }
            // §3, §11 — AUTONOMOUS is structurally rejected for LSL writes.
            BridgeSafetyMode mode = perTagSafetyMode.get(w.bindingId());
            if (mode == BridgeSafetyMode.AUTONOMOUS) {
                throw new IllegalArgumentException(
                        "LSL bridge: write binding '" + w.bindingId()
                                + "' declares perTagSafetyMode=AUTONOMOUS, "
                                + "which is rejected by the LSL ceiling (05-LSL.md §3).");
            }
        }
    }

    @JsonCreator
    public static LslBridgeConfig create(
            @JsonProperty("discovery") DiscoveryConfig discovery,
            @JsonProperty("reads") List<ReadBindingConfig> reads,
            @JsonProperty("writes") List<WriteBindingConfig> writes,
            @JsonProperty("audit") AuditConfig audit,
            @JsonProperty("perTagSafetyMode") Map<String, BridgeSafetyMode> perTagSafetyMode,
            @JsonProperty("tickInterval") Duration tickInterval) {
        return new LslBridgeConfig(discovery, reads, writes, audit, perTagSafetyMode, tickInterval);
    }

    /** Targets the bridge can map an inbound LSL chunk onto (05-LSL.md §5). */
    public enum ReadSignalKind {
        /** {@code EEG} stream → {@link com.rakovpublic.jneuropallium.worker.net.signals.impl.bci.LFPSignal}. */
        LFP,
        /** {@code Spikes} stream → {@link com.rakovpublic.jneuropallium.worker.net.signals.impl.bci.NeuralSpikeSignal}. */
        NEURAL_SPIKE,
        /** {@code ECoG} stream → {@link com.rakovpublic.jneuropallium.worker.net.signals.impl.bci.ECoGSignal}. */
        ECOG,
        /** {@code EMG} stream → {@link com.rakovpublic.jneuropallium.worker.net.signals.impl.embodiment.ProprioceptiveSignal}. */
        EMG_PROPRIOCEPTIVE,
        /** {@code HRV} or {@code GSR} stream → {@link com.rakovpublic.jneuropallium.worker.net.signals.impl.affect.InteroceptiveSignal}. */
        INTEROCEPTIVE,
        /** {@code Eye} stream → {@link com.rakovpublic.jneuropallium.worker.net.signals.impl.affect.AppraisalSignal}. */
        APPRAISAL,
        /** {@code Temperature} stream → {@link com.rakovpublic.jneuropallium.worker.net.signals.impl.bci.ThermalSignal}. */
        THERMAL,
        /** {@code Markers} stream → {@link com.rakovpublic.jneuropallium.worker.net.signals.impl.bci.CalibrationSignal} when matched. */
        CALIBRATION_MARKER
    }

    /** Outlet payload shape (05-LSL.md §5 egress table). */
    public enum OutletKind {
        /** Irregular marker stream — string payload. */
        MARKERS,
        /** Numeric stream at a fixed nominal sample rate. */
        NUMERIC
    }

    /** §6 {@code discovery:} block. */
    public record DiscoveryConfig(
            long resolveTimeoutMs,
            List<String> expectedStreams
    ) {
        public DiscoveryConfig {
            if (resolveTimeoutMs <= 0) resolveTimeoutMs = 2_000L;
            expectedStreams = expectedStreams == null ? List.of() : List.copyOf(expectedStreams);
        }

        @JsonCreator
        public static DiscoveryConfig of(
                @JsonProperty("resolveTimeoutMs") Long resolveTimeoutMs,
                @JsonProperty("expectedStreams") List<String> expectedStreams) {
            return new DiscoveryConfig(
                    resolveTimeoutMs == null ? 2_000L : resolveTimeoutMs,
                    expectedStreams);
        }
    }

    /** §6 {@code reads:} entry — one inlet binding. */
    public record ReadBindingConfig(
            String bindingId,
            String streamName,
            String streamType,
            List<String> channels,
            int chunkLengthSamples,
            ReadSignalKind targetSignal,
            String signalTag,
            String signalTagPrefix,
            int decimateBy,
            String calibrationCueRegex,
            int ringBufferMaxSamples
    ) {
        public ReadBindingConfig {
            Objects.requireNonNull(bindingId, "bindingId");
            Objects.requireNonNull(streamName, "streamName");
            channels = channels == null ? List.of() : List.copyOf(channels);
            if (chunkLengthSamples <= 0) chunkLengthSamples = 32;
            if (decimateBy <= 0) decimateBy = 1;
            if (ringBufferMaxSamples <= 0) ringBufferMaxSamples = 4096;
            if (targetSignal == null) targetSignal = ReadSignalKind.LFP;
        }

        @JsonCreator
        public static ReadBindingConfig of(
                @JsonProperty("bindingId") String bindingId,
                @JsonProperty("streamName") String streamName,
                @JsonProperty("streamType") String streamType,
                @JsonProperty("channels") List<String> channels,
                @JsonProperty("chunkLengthSamples") Integer chunkLengthSamples,
                @JsonProperty("targetSignal") ReadSignalKind targetSignal,
                @JsonProperty("signalTag") String signalTag,
                @JsonProperty("signalTagPrefix") String signalTagPrefix,
                @JsonProperty("decimateBy") Integer decimateBy,
                @JsonProperty("calibrationCueRegex") String calibrationCueRegex,
                @JsonProperty("ringBufferMaxSamples") Integer ringBufferMaxSamples) {
            return new ReadBindingConfig(
                    bindingId, streamName, streamType, channels,
                    chunkLengthSamples == null ? 32 : chunkLengthSamples,
                    targetSignal, signalTag, signalTagPrefix,
                    decimateBy == null ? 1 : decimateBy,
                    calibrationCueRegex,
                    ringBufferMaxSamples == null ? 4096 : ringBufferMaxSamples);
        }
    }

    /** §6 {@code writes:} entry — one outlet binding (05-LSL.md §5 egress). */
    public record WriteBindingConfig(
            String bindingId,
            String outletName,
            OutletKind type,
            double nominalSrate,
            String signalTag,
            Double minClampValue,
            Double maxClampValue,
            Double rampRateMaxPerSec,
            Double failSafeValue,
            boolean stimulationGated
    ) {
        public WriteBindingConfig {
            Objects.requireNonNull(bindingId, "bindingId");
            Objects.requireNonNull(outletName, "outletName");
            Objects.requireNonNull(signalTag, "signalTag");
            if (type == null) type = OutletKind.MARKERS;
        }

        @JsonCreator
        public static WriteBindingConfig of(
                @JsonProperty("bindingId") String bindingId,
                @JsonProperty("outletName") String outletName,
                @JsonProperty("type") OutletKind type,
                @JsonProperty("nominalSrate") Double nominalSrate,
                @JsonProperty("signalTag") String signalTag,
                @JsonProperty("minClampValue") Double minClampValue,
                @JsonProperty("maxClampValue") Double maxClampValue,
                @JsonProperty("rampRateMaxPerSec") Double rampRateMaxPerSec,
                @JsonProperty("failSafeValue") Double failSafeValue,
                @JsonProperty("stimulationGated") Boolean stimulationGated) {
            return new WriteBindingConfig(
                    bindingId, outletName, type,
                    nominalSrate == null ? 0.0 : nominalSrate,
                    signalTag, minClampValue, maxClampValue, rampRateMaxPerSec, failSafeValue,
                    stimulationGated != null && stimulationGated);
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
