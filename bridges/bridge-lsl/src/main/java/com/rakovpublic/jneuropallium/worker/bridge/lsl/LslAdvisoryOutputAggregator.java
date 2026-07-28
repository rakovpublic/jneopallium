/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.lsl;

import com.rakovpublic.jneuropallium.worker.application.IOutputAggregator;
import com.rakovpublic.jneuropallium.worker.bridge.common.AbstractBridgeAuditOutput;
import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeAuditRecord;
import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeSafetyMode;
import com.rakovpublic.jneuropallium.worker.net.layers.IResult;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci.IStimulationSafetyGateNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.bci.IntentSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.bci.SeizureRiskSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.bci.StimulationCommandSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.InterlockSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.OperatorOverrideSignal;
import com.rakovpublic.jneuropallium.worker.util.IContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Advisory output aggregator for the LSL bridge (05-LSL.md §3, §4 — the
 * aggregator publishes onto the {@code Jneopallium-Stim-Advisory},
 * {@code Jneopallium-Intent}, and {@code Jneopallium-Risk} outlets that
 * stimulator-driver software subscribes to).
 *
 * <p>Implements 00-FRAMEWORK §2.2 in this exact order:
 *
 * <ol>
 *   <li>{@link InterlockSignal} → write {@code failSafeValue} on every
 *       binding bound to the same loop. No other veto applies.</li>
 *   <li>{@link OperatorOverrideSignal} → registered for the matching tag
 *       (subsequent commands for that tag see {@code OVERRIDE_HOLD}).</li>
 *   <li>For each command:
 *     <ol>
 *       <li>Resolve binding by tag — unknown tags → {@code REJECTED:UNKNOWN_TAG}.</li>
 *       <li>Override-hold check.</li>
 *       <li>{@link BridgeSafetyMode} check — {@code SHADOW} → reject;
 *           {@code ADVISORY} requires the stimulation gate to allow the
 *           command (for {@link StimulationCommandSignal}) or
 *           {@link IntentSignal#getConfidence()} above the threshold (for
 *           Intent).</li>
 *       <li>Clamp / ramp / diff-suppress (numeric outlets only).</li>
 *       <li>Push onto the LSL outlet via {@link LslClientService}.</li>
 *       <li>Audit verdict.</li>
 *     </ol>
 *   </li>
 * </ol>
 *
 * <p>05-LSL.md §3 — Stimulation outputs are gated by a
 * {@link IStimulationSafetyGateNeuron}. This aggregator wires into it; if
 * the gate is unset, every {@link StimulationCommandSignal} is rejected
 * with reason {@code GATE_UNCONFIGURED}. The repository's
 * {@code StimulationSafetyGateNeuron} (Shannon criterion + lockouts) is
 * the supplied implementation; tests inject a stub.
 */
public final class LslAdvisoryOutputAggregator implements IOutputAggregator {

    private static final Logger log = LoggerFactory.getLogger(LslAdvisoryOutputAggregator.class);

    /** §2.2.4f diff-suppression window (5 s — same as every other bridge). */
    public static final long DIFF_WINDOW_MS = 5_000L;
    /** §2.2.4f diff-suppression epsilon. */
    public static final double DIFF_EPSILON = 1e-9;

    /** LSL-specific reasons in addition to 00-FRAMEWORK §4 vocabulary. */
    public static final class Reason {
        private Reason() {}
        public static final String GATE_UNCONFIGURED = "GATE_UNCONFIGURED";
        public static final String GATE_VETO         = "GATE_VETO";
        public static final String UNKNOWN_TAG       = BridgeAuditRecord.RejectReason.UNKNOWN_TAG;
    }

    private final LslClientService svc;
    private final AbstractBridgeAuditOutput audit;
    private final StimulationGate gate;
    private final LslBridgeConfig config;

    /**
     * Narrowed view of the stimulation safety contract so the aggregator
     * can be constructed with a real {@link IStimulationSafetyGateNeuron}
     * (production wiring) or a small test double (acceptance scenarios)
     * without dragging in the full {@code INeuron} surface.
     */
    @FunctionalInterface
    public interface StimulationGate {
        /** {@code null} = allow; non-null = veto reason. */
        String veto(StimulationCommandSignal cmd, long currentTick);
    }

    /** Adapter so production code can pass a real gate neuron directly. */
    public static StimulationGate fromNeuron(IStimulationSafetyGateNeuron neuron) {
        return neuron == null ? null : neuron::veto;
    }

    /** Per-tag last-applied {value, ts} for diff-suppression and rate-limit. */
    private final Map<String, double[]> lastApplied = new HashMap<>();
    /** Per-tag override TTL (ms since epoch) — lookup key is the binding's signal tag. */
    private final Map<String, Long> overrideUntilMs = new HashMap<>();
    private long lastTickTimestampMs = -1L;

    public LslAdvisoryOutputAggregator(LslClientService svc,
                                       AbstractBridgeAuditOutput audit,
                                       StimulationGate gate) {
        this.svc = Objects.requireNonNull(svc, "svc");
        this.audit = Objects.requireNonNull(audit, "audit");
        this.gate = gate; // may be null — every Stim command is then refused.
        this.config = svc.config();
    }

    /** Convenience constructor — wires a real gate neuron directly. */
    public LslAdvisoryOutputAggregator(LslClientService svc,
                                       AbstractBridgeAuditOutput audit,
                                       IStimulationSafetyGateNeuron gate) {
        this(svc, audit, fromNeuron(gate));
    }

    @Override
    public synchronized void save(List<IResult> results, long timestamp, long run, IContext context) {
        if (results == null || results.isEmpty()) {
            this.lastTickTimestampMs = timestamp;
            return;
        }
        // §0.2 — interlocks first, no veto.
        for (IResult r : results) {
            if (r == null) continue;
            IResultSignal<?> s = r.getResult();
            if (s instanceof InterlockSignal il && il.isTripped()) {
                applyInterlock(il, r.getNeuronId(), timestamp, run);
            }
        }
        // §0.3 — register operator overrides next.
        for (IResult r : results) {
            if (r == null) continue;
            if (r.getResult() instanceof OperatorOverrideSignal ov) {
                String tag = ov.getTag();
                if (tag == null) continue;
                long tickMs = Math.max(1L, config.tickInterval().toMillis());
                long ttlMs = Math.max(tickMs, (long) ov.getTimeAlive() * tickMs);
                overrideUntilMs.put(tag, timestamp + ttlMs);
            }
        }
        // §0.1, §0.4 — process commands.
        for (IResult r : results) {
            if (r == null) continue;
            IResultSignal<?> s = r.getResult();
            Long neuronId = r.getNeuronId();
            try {
                if (s instanceof StimulationCommandSignal stim) {
                    handleStimulation(stim, timestamp, run, neuronId);
                } else if (s instanceof IntentSignal intent) {
                    handleIntent(intent, timestamp, run, neuronId);
                } else if (s instanceof SeizureRiskSignal risk) {
                    handleRisk(risk, timestamp, run, neuronId);
                }
            } catch (RuntimeException ex) {
                audit.append(new BridgeAuditRecord(
                        timestamp, run, LslClientService.BRIDGE_NAME,
                        BridgeAuditRecord.Verdict.FAILED,
                        null, tagOf(s), null, null,
                        BridgeAuditRecord.RejectReason.EXCEPTION + ":" + ex.getClass().getSimpleName(),
                        null, evidence(neuronId)));
            }
        }
        this.lastTickTimestampMs = timestamp;
    }

    private void applyInterlock(InterlockSignal il, Long neuronId, long ts, long run) {
        String loop = il.getInterlockId();
        if (loop == null) return;
        for (LslBridgeConfig.WriteBindingConfig w : config.writes()) {
            LslStreamBinding b = svc.binding(w.bindingId());
            if (b == null) continue;
            if (!Objects.equals(loop, b.loopId()) && !Objects.equals(loop, b.signalTag())) continue;
            Double fs = b.failSafeValue();
            if (fs == null) {
                // Markers default fail-safe is the sentinel "INTERLOCK_TRIP".
                boolean ok = svc.pushMarker(b.bindingId(), "INTERLOCK_TRIP", ts, run);
                audit.append(new BridgeAuditRecord(
                        ts, run, LslClientService.BRIDGE_NAME,
                        ok ? BridgeAuditRecord.Verdict.INTERLOCK_TRIP : BridgeAuditRecord.Verdict.FAILED,
                        b.loopId(), b.signalTag(), null, null,
                        ok ? null : "PUBLISH_FAILED", BridgeSafetyMode.ADVISORY,
                        evidence(neuronId)));
                continue;
            }
            boolean ok = svc.pushNumeric(b.bindingId(), fs, ts, run);
            audit.append(new BridgeAuditRecord(
                    ts, run, LslClientService.BRIDGE_NAME,
                    ok ? BridgeAuditRecord.Verdict.INTERLOCK_TRIP : BridgeAuditRecord.Verdict.FAILED,
                    b.loopId(), b.signalTag(), null, fs,
                    null, BridgeSafetyMode.ADVISORY, evidence(neuronId)));
            if (ok) lastApplied.put(b.signalTag(), new double[]{fs, ts});
        }
    }

    /**
     * Stimulation commands MUST go through the safety gate before reaching
     * the advisory outlet (05-LSL.md §3). The aggregator never bypasses it.
     */
    private void handleStimulation(StimulationCommandSignal stim, long ts, long run, Long neuronId) {
        LslStreamBinding b = findStimulationBinding();
        if (b == null) {
            audit.append(new BridgeAuditRecord(
                    ts, run, LslClientService.BRIDGE_NAME,
                    BridgeAuditRecord.Verdict.REJECTED,
                    null, null, null, null,
                    Reason.UNKNOWN_TAG, null, evidence(neuronId)));
            return;
        }
        BridgeSafetyMode mode = effectiveMode(b);
        if (overrideActive(b.signalTag(), ts)) {
            audit.append(new BridgeAuditRecord(
                    ts, run, LslClientService.BRIDGE_NAME,
                    BridgeAuditRecord.Verdict.OVERRIDE_HOLD,
                    b.loopId(), b.signalTag(), null, null,
                    BridgeAuditRecord.RejectReason.OVERRIDE_HOLD, mode, evidence(neuronId)));
            return;
        }
        if (mode == BridgeSafetyMode.SHADOW) {
            audit.append(new BridgeAuditRecord(
                    ts, run, LslClientService.BRIDGE_NAME,
                    BridgeAuditRecord.Verdict.REJECTED,
                    b.loopId(), b.signalTag(), null, null,
                    BridgeAuditRecord.RejectReason.SHADOW_MODE, mode, evidence(neuronId)));
            return;
        }
        // §3 — every stimulation command must pass the safety gate.
        if (gate == null) {
            audit.append(new BridgeAuditRecord(
                    ts, run, LslClientService.BRIDGE_NAME,
                    BridgeAuditRecord.Verdict.REJECTED,
                    b.loopId(), b.signalTag(), null, null,
                    Reason.GATE_UNCONFIGURED, mode, evidence(neuronId)));
            return;
        }
        String veto = gate.veto(stim, run);
        if (veto != null) {
            audit.append(new BridgeAuditRecord(
                    ts, run, LslClientService.BRIDGE_NAME,
                    BridgeAuditRecord.Verdict.REJECTED,
                    b.loopId(), b.signalTag(), null, null,
                    Reason.GATE_VETO + ":" + veto, mode, evidence(neuronId)));
            return;
        }
        // Marker payload encodes the gated parameters in a form the
        // stimulator-driver software can re-validate before emitting current.
        String payload = String.format(
                "STIM electrode=%d amp=%.3fuA pw=%.1fus f=%.1fHz n=%d pattern=%s",
                stim.getElectrodeId(), stim.getAmplitudeUA(), stim.getPulseWidthUS(),
                stim.getFrequencyHz(), stim.getNPulses(),
                stim.getPattern() == null ? "NULL" : stim.getPattern().name());
        boolean ok = svc.pushMarker(b.bindingId(), payload, ts, run);
        audit.append(new BridgeAuditRecord(
                ts, run, LslClientService.BRIDGE_NAME,
                ok ? BridgeAuditRecord.Verdict.APPLIED : BridgeAuditRecord.Verdict.FAILED,
                b.loopId(), b.signalTag(), null, null,
                ok ? null : "PUBLISH_FAILED", mode, evidence(neuronId)));
    }

    /**
     * Intent: published as a marker on the {@code Jneopallium-Intent}
     * outlet. Subject to override / SHADOW; not gated by the safety gate
     * (Intent is a hypothesis about user state, not an effecting command).
     */
    private void handleIntent(IntentSignal intent, long ts, long run, Long neuronId) {
        LslStreamBinding b = findBindingByOutletKindAndType(LslBridgeConfig.OutletKind.MARKERS, "Intent");
        if (b == null) return; // No Intent outlet configured — silently skipped.
        BridgeSafetyMode mode = effectiveMode(b);
        if (overrideActive(b.signalTag(), ts)) {
            audit.append(new BridgeAuditRecord(
                    ts, run, LslClientService.BRIDGE_NAME,
                    BridgeAuditRecord.Verdict.OVERRIDE_HOLD,
                    b.loopId(), b.signalTag(), null, null,
                    BridgeAuditRecord.RejectReason.OVERRIDE_HOLD, mode, evidence(neuronId)));
            return;
        }
        if (mode == BridgeSafetyMode.SHADOW) {
            audit.append(new BridgeAuditRecord(
                    ts, run, LslClientService.BRIDGE_NAME,
                    BridgeAuditRecord.Verdict.REJECTED,
                    b.loopId(), b.signalTag(), null, null,
                    BridgeAuditRecord.RejectReason.SHADOW_MODE, mode, evidence(neuronId)));
            return;
        }
        String payload = String.format("INTENT kind=%s confidence=%.3f",
                intent.getKind() == null ? "NONE" : intent.getKind().name(),
                intent.getConfidence());
        boolean ok = svc.pushMarker(b.bindingId(), payload, ts, run);
        audit.append(new BridgeAuditRecord(
                ts, run, LslClientService.BRIDGE_NAME,
                ok ? BridgeAuditRecord.Verdict.APPLIED : BridgeAuditRecord.Verdict.FAILED,
                b.loopId(), b.signalTag(), null, null,
                ok ? null : "PUBLISH_FAILED", mode, evidence(neuronId)));
    }

    /**
     * Risk: published numerically on the {@code Jneopallium-Risk} outlet
     * (10 Hz nominal). Clamped 0..1; rate-limited and diff-suppressed.
     */
    private void handleRisk(SeizureRiskSignal risk, long ts, long run, Long neuronId) {
        LslStreamBinding b = findBindingByOutletKindAndType(LslBridgeConfig.OutletKind.NUMERIC, "Risk");
        if (b == null) return;
        BridgeSafetyMode mode = effectiveMode(b);
        if (overrideActive(b.signalTag(), ts)) {
            audit.append(new BridgeAuditRecord(
                    ts, run, LslClientService.BRIDGE_NAME,
                    BridgeAuditRecord.Verdict.OVERRIDE_HOLD,
                    b.loopId(), b.signalTag(), null, null,
                    BridgeAuditRecord.RejectReason.OVERRIDE_HOLD, mode, evidence(neuronId)));
            return;
        }
        if (mode == BridgeSafetyMode.SHADOW) {
            audit.append(new BridgeAuditRecord(
                    ts, run, LslClientService.BRIDGE_NAME,
                    BridgeAuditRecord.Verdict.REJECTED,
                    b.loopId(), b.signalTag(), risk.getRisk(), null,
                    BridgeAuditRecord.RejectReason.SHADOW_MODE, mode, evidence(neuronId)));
            return;
        }
        double proposed = risk.getRisk();
        String modifyReason = null;
        double effective = proposed;
        if (b.maxClampValue() != null && effective > b.maxClampValue()) {
            effective = b.maxClampValue();
            modifyReason = BridgeAuditRecord.ModifyReason.CLAMPED_HIGH;
        } else if (b.minClampValue() != null && effective < b.minClampValue()) {
            effective = b.minClampValue();
            modifyReason = BridgeAuditRecord.ModifyReason.CLAMPED_LOW;
        }
        // Rate-limit.
        double[] last = lastApplied.get(b.signalTag());
        double dtSec = (lastTickTimestampMs > 0 && ts > lastTickTimestampMs)
                ? (ts - lastTickTimestampMs) / 1000.0 : 0.0;
        if (b.rampRateMaxPerSec() != null && last != null && dtSec > 0) {
            double maxStep = b.rampRateMaxPerSec() * dtSec;
            double delta = effective - last[0];
            if (Math.abs(delta) > maxStep) {
                effective = last[0] + Math.signum(delta) * maxStep;
                modifyReason = BridgeAuditRecord.ModifyReason.RATE_LIMITED;
            }
        }
        // Diff-suppression.
        if (last != null && (ts - (long) last[1]) <= DIFF_WINDOW_MS
                && Math.abs(effective - last[0]) <= DIFF_EPSILON) {
            audit.append(new BridgeAuditRecord(
                    ts, run, LslClientService.BRIDGE_NAME,
                    BridgeAuditRecord.Verdict.APPLIED,
                    b.loopId(), b.signalTag(), proposed, effective,
                    BridgeAuditRecord.ModifyReason.DIFF_SUPPRESSED, mode, evidence(neuronId)));
            return;
        }
        boolean ok = svc.pushNumeric(b.bindingId(), effective, ts, run);
        if (ok) {
            lastApplied.put(b.signalTag(), new double[]{effective, ts});
            audit.append(new BridgeAuditRecord(
                    ts, run, LslClientService.BRIDGE_NAME,
                    BridgeAuditRecord.Verdict.APPLIED,
                    b.loopId(), b.signalTag(), proposed, effective,
                    modifyReason, mode, evidence(neuronId)));
        }
    }

    /* ===== helpers ======================================================== */

    private boolean overrideActive(String tag, long ts) {
        if (tag == null) return false;
        Long until = overrideUntilMs.get(tag);
        return until != null && until > ts;
    }

    private BridgeSafetyMode effectiveMode(LslStreamBinding b) {
        return config.perTagSafetyMode().getOrDefault(b.bindingId(), BridgeSafetyMode.ADVISORY);
    }

    private LslStreamBinding findStimulationBinding() {
        for (LslBridgeConfig.WriteBindingConfig w : config.writes()) {
            if (w.stimulationGated()) return svc.binding(w.bindingId());
        }
        return null;
    }

    /** Find a write binding whose {@code outletName} contains the given suffix. */
    private LslStreamBinding findBindingByOutletKindAndType(LslBridgeConfig.OutletKind kind, String suffix) {
        for (LslBridgeConfig.WriteBindingConfig w : config.writes()) {
            if (w.type() == kind && w.outletName() != null && w.outletName().contains(suffix)) {
                return svc.binding(w.bindingId());
            }
        }
        return null;
    }

    private static String tagOf(IResultSignal<?> s) {
        if (s instanceof InterlockSignal il) return il.getInterlockId();
        if (s instanceof OperatorOverrideSignal ov) return ov.getTag();
        return null;
    }

    private static List<String> evidence(Long neuronId) {
        return neuronId == null ? List.of() : List.of(String.valueOf(neuronId));
    }
}
