/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.integration.nengo;

import com.rakovpublic.jneuropallium.ai.signals.fast.HarmVetoSignal;
import com.rakovpublic.jneuropallium.ai.signals.fast.MotorCommandSignal;
import com.rakovpublic.jneuropallium.ai.signals.fast.TransparencyLogSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Maps approved Jneopallium result signals to {@link NengoOutputFrame}s
 * (15-NENGO.md §6.3).
 *
 * <p>Three approved-signal classes are recognised:
 *
 * <ul>
 *   <li>{@link MotorCommandSignal} — turns into an {@code OK} frame whose
 *       {@code values} are labelled by the binding's {@code frameLabels}.
 *       Refused (returns {@code null}) unless {@code execute=true} —
 *       only the safety pipeline (HarmGateNeuron) sets that flag, so this
 *       structurally prevents shadow-mode commands from reaching Nengo.</li>
 *   <li>{@link HarmVetoSignal} — always produces a {@code STOP} frame with
 *       the configured fail-safe values (typically zero).</li>
 *   <li>{@link TransparencyLogSignal} — observed only to capture the
 *       associated transparency-log id for the next emitted frame, so the
 *       Python side can correlate frames to the worker's audit JSONL
 *       (S13).</li>
 * </ul>
 *
 * <p>Frame ids are monotonically issued from a single {@link AtomicLong};
 * the {@code valid_until_ms} is {@code timestampMs + binding.validForMs}.
 */
public final class JneopalliumToNengoMapper {

    private final NengoBridgeConfig config;
    private final Map<String, NengoBridgeConfig.OutputMapping> bySignalType;
    private final AtomicLong sequence = new AtomicLong();
    private String pendingTxId;

    public JneopalliumToNengoMapper(NengoBridgeConfig config) {
        this.config = Objects.requireNonNull(config, "config");
        Map<String, NengoBridgeConfig.OutputMapping> idx = new LinkedHashMap<>();
        for (NengoBridgeConfig.OutputMapping m : config.outputMappings()) {
            idx.put(m.approvedSignalType(), m);
        }
        this.bySignalType = Map.copyOf(idx);
    }

    /** Note a transparency-log id observed in the same tick — attached to the next emitted frame. */
    public void noteTransparency(TransparencyLogSignal tx) {
        if (tx == null) return;
        this.pendingTxId = tx.getActionPlanId();
    }

    /**
     * Build an output frame from an approved result signal. Returns
     * {@code null} when no frame should be emitted (e.g. unbound signal
     * type, or {@code execute=false}).
     */
    public NengoOutputFrame buildFrame(IResultSignal<?> approved, long nowMs) {
        if (approved instanceof MotorCommandSignal mc) {
            return buildMotorFrame(mc, nowMs);
        }
        if (approved instanceof HarmVetoSignal hv) {
            return buildStopFrame(hv, nowMs);
        }
        if (approved instanceof TransparencyLogSignal tx) {
            noteTransparency(tx);
            return null;
        }
        return null;
    }

    private NengoOutputFrame buildMotorFrame(MotorCommandSignal mc, long nowMs) {
        if (!mc.isExecute()) return null;
        NengoBridgeConfig.OutputMapping binding = bySignalType.get("MotorCommandSignal");
        if (binding == null) return null;
        Map<String, Double> values = new LinkedHashMap<>();
        double[] params = mc.getParams();
        List<String> labels = binding.frameLabels();
        for (int i = 0; i < labels.size(); i++) {
            double v = (params != null && i < params.length) ? params[i] : 0.0;
            values.put(labels.get(i), v);
        }
        long seq = sequence.incrementAndGet();
        String txId = takeTxId();
        return new NengoOutputFrame(
                NengoOutputFrame.SCHEMA_VERSION,
                NengoOutputFrame.SOURCE,
                String.format("f-%06d", seq),
                seq,
                nowMs,
                nowMs + binding.validForMs(),
                NengoOutputFrame.STATUS_OK,
                values,
                txId);
    }

    private NengoOutputFrame buildStopFrame(HarmVetoSignal hv, long nowMs) {
        NengoBridgeConfig.OutputMapping binding = bySignalType.get("MotorCommandSignal");
        long validFor = binding == null ? 250L : binding.validForMs();
        Map<String, Double> values = binding != null && binding.failSafeFrame() != null
                ? binding.failSafeFrame().values()
                : Map.of();
        long seq = sequence.incrementAndGet();
        String txId = takeTxId();
        if (txId == null && hv.getActionPlanId() != null) txId = hv.getActionPlanId();
        return new NengoOutputFrame(
                NengoOutputFrame.SCHEMA_VERSION,
                NengoOutputFrame.SOURCE,
                String.format("f-%06d", seq),
                seq,
                nowMs,
                nowMs + validFor,
                NengoOutputFrame.STATUS_STOP,
                values,
                txId);
    }

    /** Build a watchdog STOP frame on outputDecayMs expiry (S11). */
    public NengoOutputFrame buildWatchdogStopFrame(long nowMs) {
        NengoBridgeConfig.OutputMapping binding = bySignalType.get("MotorCommandSignal");
        long validFor = binding == null ? 250L : binding.validForMs();
        Map<String, Double> values = binding != null && binding.failSafeFrame() != null
                ? binding.failSafeFrame().values()
                : Map.of();
        long seq = sequence.incrementAndGet();
        return new NengoOutputFrame(
                NengoOutputFrame.SCHEMA_VERSION,
                NengoOutputFrame.SOURCE,
                String.format("f-%06d", seq),
                seq,
                nowMs,
                nowMs + validFor,
                NengoOutputFrame.STATUS_STOP,
                values,
                "WATCHDOG_DECAY");
    }

    private String takeTxId() {
        String tx = pendingTxId;
        pendingTxId = null;
        return tx;
    }

    NengoBridgeConfig config() { return config; }
}
