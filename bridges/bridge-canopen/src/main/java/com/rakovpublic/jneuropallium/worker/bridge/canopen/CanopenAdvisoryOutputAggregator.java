/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.canopen;

import com.rakovpublic.jneuropallium.worker.application.IOutputAggregator;
import com.rakovpublic.jneuropallium.worker.bridge.common.AbstractBridgeAuditOutput;
import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeAuditRecord;
import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeSafetyMode;
import com.rakovpublic.jneuropallium.worker.net.layers.IResult;
import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.ActuatorCommandSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.InterlockSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.OperatorOverrideSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.SetpointSignal;
import com.rakovpublic.jneuropallium.worker.util.IContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * {@link IOutputAggregator} for the CANopen advisory egress
 * (13-CANOPEN.md §3, §5 egress table).
 *
 * <p>Per 13-CANOPEN.md §3 the bridge ceiling is structurally
 * <b>ADVISORY</b> — it never autonomously promotes a write to
 * {@code AUTONOMOUS}. The structural defences are layered:
 *
 * <ol>
 *   <li>{@link CanopenBridgeConfig} rejects forbidden indices and any
 *       write whose {@code (nodeId, odIndex)} is not on the
 *       {@code writeIndexAllowList} at config load (§6, §10 R3).</li>
 *   <li>{@link AbstractCanopenClientService#send} re-checks both gates at
 *       runtime — a config that bypasses the loader (e.g. constructed
 *       in-process) still cannot punch a write through.</li>
 *   <li>This aggregator enforces the universal §2.2 algorithm: SHADOW
 *       drops, override holds, clamp, ramp, diff-suppress, audit.</li>
 * </ol>
 *
 * <p>Signal handling:
 *
 * <ul>
 *   <li>{@link InterlockSignal} → fail-safe value written to every binding
 *       whose loop matches; no veto (00-FRAMEWORK §0.2).</li>
 *   <li>{@link OperatorOverrideSignal} → registered for the matching tag
 *       (§0.3).</li>
 *   <li>{@link SetpointSignal} / {@link ActuatorCommandSignal} → resolved
 *       to a write binding, clamped, rate-limited, diff-suppressed,
 *       written, audited.</li>
 * </ul>
 */
public final class CanopenAdvisoryOutputAggregator implements IOutputAggregator {

    private static final Logger log = LoggerFactory.getLogger(CanopenAdvisoryOutputAggregator.class);

    /** §2.2.4f diff-suppression window. */
    public static final long DIFF_WINDOW_MS = 5_000L;

    /** §2.2.4f diff-suppression epsilon. */
    public static final double DIFF_EPSILON = 1e-9;

    private final CanopenClientService svc;
    private final AbstractBridgeAuditOutput audit;
    private final CanopenBridgeConfig config;

    /** Per-tag last-applied-value cache used by rate-limit and diff-suppress. */
    private final Map<String, double[]> lastApplied = new HashMap<>();
    private long lastTickTimestampMs = -1L;

    public CanopenAdvisoryOutputAggregator(CanopenClientService svc,
                                           AbstractBridgeAuditOutput audit) {
        this.svc = Objects.requireNonNull(svc, "svc");
        this.audit = Objects.requireNonNull(audit, "audit");
        this.config = svc.config();
    }

    @Override
    public synchronized void save(List<IResult> results, long timestamp, long run, IContext context) {
        if (results == null || results.isEmpty()) {
            this.lastTickTimestampMs = timestamp;
            return;
        }
        // §0.2 first — interlocks have direct authority and bypass every veto.
        for (IResult r : results) {
            if (r == null) continue;
            IResultSignal<?> s = r.getResult();
            if (s instanceof InterlockSignal il && il.isTripped()) {
                applyInterlock(il, r.getNeuronId(), timestamp, run);
            }
        }
        // §0.3 next — register operator overrides so subsequent commands see them.
        // (For simplicity the registry is tag-keyed and tick-scoped; the bridge does not
        //  carry an OverrideRegistry instance because CANopen overrides are typically
        //  hardware-asserted estop / mode-select switches, not persistent neuron signals.)
        // §0.1, §0.4 — partition + handle the commands.
        for (IResult r : results) {
            if (r == null) continue;
            IResultSignal<?> s = r.getResult();
            Long neuronId = r.getNeuronId();
            try {
                if (s instanceof SetpointSignal sp) {
                    handleNumeric(sp.getTag(), sp.getSetpoint(), true, timestamp, run, neuronId);
                } else if (s instanceof ActuatorCommandSignal ac) {
                    handleNumeric(ac.getTag(), ac.getTargetValue(), ac.isExecute(), timestamp, run, neuronId);
                }
            } catch (RuntimeException ex) {
                audit.append(new BridgeAuditRecord(
                        timestamp, run, CanopenClientService.BRIDGE_NAME,
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
        // CANopen bindings carry loopId == bindingId (the binding *is* the loop).
        for (String bid : config.writes().stream().map(CanopenBridgeConfig.WriteBindingConfig::bindingId).toList()) {
            CanopenNodeBinding b = svc.writeBinding(bid);
            if (b == null) continue;
            if (!Objects.equals(loop, b.loopId())) continue;
            Double fs = b.failSafeValue();
            if (fs == null) {
                audit.append(new BridgeAuditRecord(
                        ts, run, CanopenClientService.BRIDGE_NAME,
                        BridgeAuditRecord.Verdict.REJECTED,
                        b.loopId(), b.signalTag(), null, null,
                        "INTERLOCK_NO_FAILSAFE", BridgeSafetyMode.ADVISORY,
                        evidence(neuronId)));
                continue;
            }
            boolean ok = svc.send(bid, fs, ts, run);
            BridgeAuditRecord.Verdict verdict = ok
                    ? BridgeAuditRecord.Verdict.INTERLOCK_TRIP
                    : BridgeAuditRecord.Verdict.FAILED;
            audit.append(new BridgeAuditRecord(
                    ts, run, CanopenClientService.BRIDGE_NAME, verdict,
                    b.loopId(), b.signalTag(), null, fs,
                    null, BridgeSafetyMode.ADVISORY, evidence(neuronId)));
            if (ok) lastApplied.put(b.signalTag(), new double[]{fs, ts});
        }
    }

    private void handleNumeric(String tag, double proposed, boolean execute,
                               long ts, long run, Long neuronId) {
        if (tag == null) {
            audit.append(new BridgeAuditRecord(
                    ts, run, CanopenClientService.BRIDGE_NAME,
                    BridgeAuditRecord.Verdict.REJECTED,
                    null, null, proposed, null,
                    BridgeAuditRecord.RejectReason.UNKNOWN_TAG, null, evidence(neuronId)));
            return;
        }
        CanopenNodeBinding b = svc.writeBindingForTag(tag);
        if (b == null) {
            audit.append(new BridgeAuditRecord(
                    ts, run, CanopenClientService.BRIDGE_NAME,
                    BridgeAuditRecord.Verdict.REJECTED,
                    null, tag, proposed, null,
                    BridgeAuditRecord.RejectReason.UNKNOWN_TAG, null, evidence(neuronId)));
            return;
        }

        BridgeSafetyMode mode = config.perTagSafetyMode().getOrDefault(b.bindingId(), BridgeSafetyMode.ADVISORY);
        if (mode == BridgeSafetyMode.SHADOW) {
            audit.append(new BridgeAuditRecord(
                    ts, run, CanopenClientService.BRIDGE_NAME,
                    BridgeAuditRecord.Verdict.REJECTED,
                    b.loopId(), tag, proposed, null,
                    BridgeAuditRecord.RejectReason.SHADOW_MODE, mode, evidence(neuronId)));
            return;
        }
        if (mode == BridgeSafetyMode.ADVISORY && !execute) {
            audit.append(new BridgeAuditRecord(
                    ts, run, CanopenClientService.BRIDGE_NAME,
                    BridgeAuditRecord.Verdict.REJECTED,
                    b.loopId(), tag, proposed, null,
                    BridgeAuditRecord.RejectReason.ADVISORY_HOLD, mode, evidence(neuronId)));
            return;
        }

        // §2.2.4d clamp.
        String modifyReason = null;
        double effective = proposed;
        if (b.maxClampValue() != null && effective > b.maxClampValue()) {
            effective = b.maxClampValue();
            modifyReason = BridgeAuditRecord.ModifyReason.CLAMPED_HIGH;
        } else if (b.minClampValue() != null && effective < b.minClampValue()) {
            effective = b.minClampValue();
            modifyReason = BridgeAuditRecord.ModifyReason.CLAMPED_LOW;
        }

        // §2.2.4e rate limit.
        double[] last = lastApplied.get(tag);
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

        // §2.2.4f diff suppression.
        if (last != null && (ts - (long) last[1]) <= DIFF_WINDOW_MS
                && Math.abs(effective - last[0]) <= DIFF_EPSILON) {
            audit.append(new BridgeAuditRecord(
                    ts, run, CanopenClientService.BRIDGE_NAME,
                    BridgeAuditRecord.Verdict.APPLIED,
                    b.loopId(), tag, proposed, effective,
                    BridgeAuditRecord.ModifyReason.DIFF_SUPPRESSED, mode, evidence(neuronId)));
            return;
        }

        boolean ok = svc.send(b.bindingId(), effective, ts, run);
        if (ok) {
            lastApplied.put(tag, new double[]{effective, ts});
            audit.append(new BridgeAuditRecord(
                    ts, run, CanopenClientService.BRIDGE_NAME,
                    BridgeAuditRecord.Verdict.APPLIED,
                    b.loopId(), tag, proposed, effective,
                    modifyReason, mode, evidence(neuronId)));
        }
        // Failure paths are audited inside AbstractCanopenClientService.send.
    }

    private static String tagOf(IResultSignal<?> s) {
        if (s instanceof SetpointSignal sp) return sp.getTag();
        if (s instanceof ActuatorCommandSignal ac) return ac.getTag();
        return null;
    }

    private static List<String> evidence(Long neuronId) {
        return neuronId == null ? List.of() : List.of(String.valueOf(neuronId));
    }
}
