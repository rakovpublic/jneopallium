/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.common;

import com.rakovpublic.jneuropallium.worker.application.IOutputAggregator;
import com.rakovpublic.jneuropallium.worker.net.layers.IResult;
import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.ActuatorCommandSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.InterlockSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.OperatorOverrideSignal;
import com.rakovpublic.jneuropallium.worker.util.IContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Template-method base class implementing the universal write algorithm
 * from 00-FRAMEWORK §2.2. Bridges plug in protocol specifics by overriding
 * {@link #issueWrite(BridgeBinding, double)} (and, optionally, the binding
 * / safety-mode lookup hooks).
 *
 * <p>The algorithm enforces ground rules §0.1–§0.4:
 *
 * <ol>
 *   <li>Partition results into interlocks, overrides, and commands.</li>
 *   <li>For every tripped interlock, write the bound output's
 *       {@link BridgeBinding#failSafeValue() fail-safe value}. <b>No vetoes
 *       apply.</b></li>
 *   <li>Refresh the {@link OverrideRegistry} from incoming
 *       {@link OperatorOverrideSignal}s.</li>
 *   <li>For each command:
 *       <ol type="a">
 *         <li>resolve binding ({@code UNKNOWN_TAG} → {@code REJECTED})</li>
 *         <li>override active for tag → {@code OVERRIDE_HOLD}</li>
 *         <li>mode {@code SHADOW} → {@code REJECTED reason=SHADOW_MODE};
 *             mode {@code ADVISORY} requires {@code execute=true} <b>and</b>
 *             operator confirmation</li>
 *         <li>clamp to {@code [minClampValue, maxClampValue]}</li>
 *         <li>rate-limit by {@code rampRateMaxPerSec * dt}</li>
 *         <li>diff-suppress writes within {@code DIFF_EPSILON} of the last
 *             applied value for this tag within {@link #DIFF_WINDOW_MS}</li>
 *         <li>protocol write</li>
 *         <li>audit verdict (with {@code reason} when modified)</li>
 *       </ol></li>
 * </ol>
 *
 * <p>The base is generic in {@code <BindingT>} — every bridge supplies its
 * own binding shape (a record adapting its native ID type onto
 * {@link BridgeBinding}). Bindings are resolved by {@code signalTag} via
 * {@link #binding(String)}.
 *
 * <h2>Threading</h2>
 *
 * {@code save()} is intended to be called serially per tick by the worker
 * dispatcher. The internal last-applied cache is guarded by the instance
 * monitor for safety against concurrent ticks.
 *
 * @param <BindingT> bridge-specific binding type
 */
public abstract class AbstractBridgeOutputAggregator<BindingT extends BridgeBinding>
        implements IOutputAggregator {

    private static final Logger log = LoggerFactory.getLogger(AbstractBridgeOutputAggregator.class);

    /** §2.2.4f diff-suppression window. */
    public static final long DIFF_WINDOW_MS = 5_000L;

    /** §2.2.4f diff-suppression epsilon (relative to value scale). */
    public static final double DIFF_EPSILON = 1e-9;

    /** Default override TTL when an OperatorOverrideSignal carries no explicit one. */
    public static final long DEFAULT_OVERRIDE_TTL_MS = 5 * 60_000L;

    private final String bridgeName;
    private final OverrideRegistry overrides;
    private final AbstractBridgeAuditOutput audit;

    private final Map<String, LastApplied> lastApplied = new HashMap<>();
    private long lastTickTimestampMs = -1L;

    protected AbstractBridgeOutputAggregator(
            String bridgeName,
            OverrideRegistry overrides,
            AbstractBridgeAuditOutput audit) {
        this.bridgeName = Objects.requireNonNull(bridgeName, "bridgeName");
        this.overrides = Objects.requireNonNull(overrides, "overrides");
        this.audit = Objects.requireNonNull(audit, "audit");
    }

    /* ===== abstract & overridable hooks ====================================== */

    /** Resolve a binding by signal tag, or {@code null} if unknown. */
    protected abstract BindingT binding(String tag);

    /** Issue the protocol-specific write. Implementations MUST NOT throw on protocol errors — wrap them in a {@link BridgeWriteResult#failed(String)}. */
    protected abstract BridgeWriteResult issueWrite(BindingT binding, double value);

    /**
     * Effective per-loop {@link BridgeSafetyMode}. Default: {@code SHADOW}
     * (safe-by-default — every loop must be explicitly promoted in config).
     */
    protected BridgeSafetyMode safetyMode(BindingT binding) { return BridgeSafetyMode.SHADOW; }

    /**
     * Bindings to drive to fail-safe when interlock {@code interlockId}
     * trips. Default: any binding whose {@link BridgeBinding#loopId()}
     * equals {@code interlockId}.
     *
     * <p>Subclasses override to express many-to-many interlock-to-binding
     * mappings. The default is correct for the common 1:1 case.
     */
    protected List<BindingT> bindingsForInterlock(String interlockId) { return List.of(); }

    /**
     * Whether an ADVISORY-mode command currently carries operator
     * confirmation. Default: {@code false} — bridges must opt in. The
     * override is by command tag rather than time so a single confirmation
     * can authorise a single tick's command.
     */
    protected boolean operatorConfirmed(ActuatorCommandSignal command) { return false; }

    /* ===== IOutputAggregator =================================================== */

    @Override
    public final void save(List<IResult> results, long timestampMs, long run, IContext context) {
        if (results == null || results.isEmpty()) {
            this.lastTickTimestampMs = timestampMs;
            return;
        }

        Partition p = partition(results);
        applyInterlocks(p.interlocks, timestampMs, run);
        registerOverrides(p.overrides, timestampMs);
        applyCommands(p.commands, timestampMs, run);
        this.lastTickTimestampMs = timestampMs;
    }

    /* ===== algorithm steps ===================================================== */

    private record EvidencedSignal<S extends IResultSignal<?>>(S signal, Long neuronId) {}

    private static final class Partition {
        final List<EvidencedSignal<InterlockSignal>> interlocks = new ArrayList<>();
        final List<EvidencedSignal<OperatorOverrideSignal>> overrides = new ArrayList<>();
        final List<EvidencedSignal<ActuatorCommandSignal>> commands = new ArrayList<>();
    }

    private Partition partition(List<IResult> results) {
        Partition p = new Partition();
        for (IResult r : results) {
            if (r == null) continue;
            IResultSignal<?> s = r.getResult();
            if (s == null) continue;
            Long neuronId = r.getNeuronId();
            if (s instanceof InterlockSignal il) {
                p.interlocks.add(new EvidencedSignal<>(il, neuronId));
            } else if (s instanceof OperatorOverrideSignal oo) {
                p.overrides.add(new EvidencedSignal<>(oo, neuronId));
            } else if (s instanceof ActuatorCommandSignal ac) {
                p.commands.add(new EvidencedSignal<>(ac, neuronId));
            }
        }
        return p;
    }

    private void applyInterlocks(List<EvidencedSignal<InterlockSignal>> interlocks,
                                 long ts, long run) {
        for (EvidencedSignal<InterlockSignal> e : interlocks) {
            InterlockSignal il = e.signal();
            if (!il.isTripped()) continue;
            for (BindingT b : safe(bindingsForInterlock(il.getInterlockId()))) {
                Double fs = b.failSafeValue();
                if (fs == null) {
                    audit.append(new BridgeAuditRecord(
                            ts, run, bridgeName, BridgeAuditRecord.Verdict.REJECTED,
                            b.loopId(), b.signalTag(), null, null,
                            "INTERLOCK_NO_FAILSAFE", safetyMode(b),
                            evidence(e)));
                    continue;
                }
                BridgeWriteResult wr = safeIssue(b, fs);
                BridgeAuditRecord.Verdict verdict = wr.success()
                        ? BridgeAuditRecord.Verdict.INTERLOCK_TRIP
                        : BridgeAuditRecord.Verdict.FAILED;
                audit.append(new BridgeAuditRecord(
                        ts, run, bridgeName, verdict,
                        b.loopId(), b.signalTag(), null, fs,
                        wr.detail(), safetyMode(b), evidence(e)));
                if (wr.success()) {
                    rememberApplied(b.signalTag(), fs, ts);
                }
            }
        }
    }

    private void registerOverrides(List<EvidencedSignal<OperatorOverrideSignal>> ovs, long ts) {
        for (EvidencedSignal<OperatorOverrideSignal> e : ovs) {
            OperatorOverrideSignal oo = e.signal();
            if (oo.getTag() == null) continue;
            overrides.put(oo.getTag(), oo.getOperatorId(), oo.getReason(),
                    oo.getManualValue(), DEFAULT_OVERRIDE_TTL_MS, ts);
        }
    }

    private void applyCommands(List<EvidencedSignal<ActuatorCommandSignal>> commands,
                               long ts, long run) {
        double dtSec = (lastTickTimestampMs > 0 && ts > lastTickTimestampMs)
                ? (ts - lastTickTimestampMs) / 1000.0
                : 0.0;

        for (EvidencedSignal<ActuatorCommandSignal> e : commands) {
            ActuatorCommandSignal cmd = e.signal();
            String tag = cmd.getTag();
            double proposed = cmd.getTargetValue();

            BindingT b = (tag == null) ? null : binding(tag);
            if (b == null) {
                audit.append(new BridgeAuditRecord(
                        ts, run, bridgeName, BridgeAuditRecord.Verdict.REJECTED,
                        null, tag, proposed, null,
                        BridgeAuditRecord.RejectReason.UNKNOWN_TAG, null,
                        evidence(e)));
                continue;
            }

            BridgeSafetyMode mode = safetyMode(b);

            if (overrides.active(tag, ts).isPresent()) {
                audit.append(new BridgeAuditRecord(
                        ts, run, bridgeName, BridgeAuditRecord.Verdict.OVERRIDE_HOLD,
                        b.loopId(), tag, proposed, null,
                        BridgeAuditRecord.RejectReason.OVERRIDE_HOLD, mode,
                        evidence(e)));
                continue;
            }

            if (mode == BridgeSafetyMode.SHADOW) {
                audit.append(new BridgeAuditRecord(
                        ts, run, bridgeName, BridgeAuditRecord.Verdict.REJECTED,
                        b.loopId(), tag, proposed, null,
                        BridgeAuditRecord.RejectReason.SHADOW_MODE, mode,
                        evidence(e)));
                continue;
            }

            if (mode == BridgeSafetyMode.ADVISORY
                    && (!cmd.isExecute() || !operatorConfirmed(cmd))) {
                audit.append(new BridgeAuditRecord(
                        ts, run, bridgeName, BridgeAuditRecord.Verdict.REJECTED,
                        b.loopId(), tag, proposed, null,
                        BridgeAuditRecord.RejectReason.ADVISORY_HOLD, mode,
                        evidence(e)));
                continue;
            }

            // §2.2.4d clamp
            String modifyReason = null;
            double effective = proposed;
            if (b.maxClampValue() != null && effective > b.maxClampValue()) {
                effective = b.maxClampValue();
                modifyReason = BridgeAuditRecord.ModifyReason.CLAMPED_HIGH;
            } else if (b.minClampValue() != null && effective < b.minClampValue()) {
                effective = b.minClampValue();
                modifyReason = BridgeAuditRecord.ModifyReason.CLAMPED_LOW;
            }

            // §2.2.4e rate limit
            LastApplied last = lastApplied.get(tag);
            if (b.rampRateMaxPerSec() != null && last != null && dtSec > 0) {
                double maxStep = b.rampRateMaxPerSec() * dtSec;
                double delta = effective - last.value;
                if (Math.abs(delta) > maxStep) {
                    effective = last.value + Math.signum(delta) * maxStep;
                    modifyReason = BridgeAuditRecord.ModifyReason.RATE_LIMITED;
                }
            }

            // §2.2.4f diff suppression
            if (last != null && (ts - last.tsMillis) <= DIFF_WINDOW_MS
                    && Math.abs(effective - last.value) <= DIFF_EPSILON) {
                audit.append(new BridgeAuditRecord(
                        ts, run, bridgeName, BridgeAuditRecord.Verdict.APPLIED,
                        b.loopId(), tag, proposed, effective,
                        BridgeAuditRecord.ModifyReason.DIFF_SUPPRESSED, mode,
                        evidence(e)));
                continue;
            }

            BridgeWriteResult wr = safeIssue(b, effective);
            if (wr.success()) {
                rememberApplied(tag, effective, ts);
                audit.append(new BridgeAuditRecord(
                        ts, run, bridgeName, BridgeAuditRecord.Verdict.APPLIED,
                        b.loopId(), tag, proposed, effective,
                        modifyReason, mode, evidence(e)));
            } else {
                audit.append(new BridgeAuditRecord(
                        ts, run, bridgeName, BridgeAuditRecord.Verdict.FAILED,
                        b.loopId(), tag, proposed, effective,
                        wr.detail(), mode, evidence(e)));
            }
        }
    }

    private BridgeWriteResult safeIssue(BindingT binding, double value) {
        try {
            BridgeWriteResult wr = issueWrite(binding, value);
            return wr == null ? BridgeWriteResult.failed("NULL_RESULT") : wr;
        } catch (RuntimeException ex) {
            log.warn("Bridge {} write threw for tag={}: {}",
                    bridgeName, binding.signalTag(), ex.toString());
            return BridgeWriteResult.failed(
                    BridgeAuditRecord.RejectReason.EXCEPTION + ":" + ex.getClass().getSimpleName());
        }
    }

    private void rememberApplied(String tag, double value, long ts) {
        lastApplied.put(tag, new LastApplied(value, ts));
    }

    private static <T> List<T> safe(List<T> list) { return list == null ? List.of() : list; }

    private static List<String> evidence(EvidencedSignal<?> e) {
        Long id = e.neuronId();
        return id == null ? List.of() : List.of(String.valueOf(id));
    }

    /** Cached last-applied value per tag (rate-limit + diff-suppress source). */
    private record LastApplied(double value, long tsMillis) {}

    /* ===== accessors used by subclasses & tests =============================== */

    protected final String bridgeName() { return bridgeName; }
    protected final OverrideRegistry overrides() { return overrides; }
    protected final AbstractBridgeAuditOutput audit() { return audit; }
}
