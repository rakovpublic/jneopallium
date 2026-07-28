/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.output.opcua;

import com.rakovpublic.jneuropallium.worker.application.IOutputAggregator;
import com.rakovpublic.jneuropallium.worker.net.layers.IResult;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.SafetyMode;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.opcua.MiloOpcUaClientService;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.opcua.OpcUaBridgeConfig;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.opcua.OpcUaNodeBinding;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.opcua.OpcUaSignalMapper;
import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.ActuatorCommandSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.InterlockSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.OperatorOverrideSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.SetpointSignal;
import com.rakovpublic.jneuropallium.worker.util.IContext;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.rakovpublic.jneuropallium.worker.output.opcua.OpcUaTransparencyLogOutput.Verdict;

/**
 * Safety-critical aggregator that turns neuron output into OPC UA writes.
 *
 * <p>Spec §0 ground rules enforced here:
 * <ol>
 *   <li>Interlocks have direct authority — fail-safe write first.</li>
 *   <li>Operator override wins for regulatory control.</li>
 *   <li>Per-loop {@link SafetyMode} (SHADOW/ADVISORY/AUTONOMOUS) gates writes.</li>
 *   <li>Every accepted, suppressed, clamped or rejected write produces an audit record.</li>
 *   <li>Quality and timestamps are not invented at the bridge.</li>
 * </ol>
 *
 * <p>Per-write protections: clamp to [min,max], rate-limit per
 * {@code rampRateMaxPerSec}, diff-suppress repeats inside 5 s with delta
 * &lt; 1e-6.
 */
public final class OpcUaCommandOutputAggregator implements IOutputAggregator, AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(OpcUaCommandOutputAggregator.class);

    private static final double DIFF_SUPPRESS_EPSILON = 1e-6;
    private static final long DIFF_SUPPRESS_WINDOW_MS = 5_000L;

    private final OpcUaBridgeConfig cfg;
    private final MiloOpcUaClientService svc;
    private final OpcUaTransparencyLogOutput audit;
    private final OverrideRegistry overrides = new OverrideRegistry();
    private final Map<String, OpcUaNodeBinding> writeBindings;

    public OpcUaCommandOutputAggregator(OpcUaBridgeConfig cfg,
                                        MiloOpcUaClientService svc,
                                        OpcUaTransparencyLogOutput audit) {
        this.cfg = Objects.requireNonNull(cfg);
        this.svc = Objects.requireNonNull(svc);
        this.audit = Objects.requireNonNull(audit);
        var map = new java.util.HashMap<String, OpcUaNodeBinding>();
        for (OpcUaBridgeConfig.NodeBindingConfig c : cfg.writes()) {
            OpcUaNodeBinding b = svc.bindingBySignalTag(c.signalTag());
            map.put(c.signalTag(), b != null ? b : new OpcUaNodeBinding(c));
        }
        this.writeBindings = Map.copyOf(map);
    }

    @Override
    public void save(List<IResult> results, long timestamp, long run, IContext context) {
        Partition p = partition(results);

        java.util.Set<String> heldThisTick = new java.util.HashSet<>();
        for (InterlockSignal s : p.interlocks) {
            heldThisTick.addAll(executeInterlock(s, timestamp, run));
        }

        for (OperatorOverrideSignal o : p.overrides) {
            overrides.record(o, timestamp);
        }
        overrides.expireOlderThan(timestamp);

        for (Command c : p.commands) {
            try {
                if (heldThisTick.contains(c.tag())) {
                    audit.record(Verdict.REJECTED,
                            bindingLoopId(c.tag()), c.tag(), c.proposed(), null,
                            "INTERLOCK_HOLD",
                            currentSafetyMode(c.tag()).name(),
                            timestamp, run);
                    continue;
                }
                processCommand(c, timestamp, run);
            } catch (Exception e) {
                log.error("Command processing failed for tag={}", c.tag(), e);
                audit.record(Verdict.FAILED, "?", c.tag(), c.proposed(), null,
                        "EXCEPTION:" + e.getClass().getSimpleName(),
                        currentSafetyMode(c.tag()).name(),
                        timestamp, run);
            }
        }
    }

    private String bindingLoopId(String tag) {
        OpcUaNodeBinding b = writeBindings.get(tag);
        return b == null ? "?" : b.loopId;
    }

    /* ============================================================ */

    /** @return signal tags that were tripped to fail-safe — must be held for the rest of the tick. */
    private java.util.Set<String> executeInterlock(InterlockSignal s, long ts, long run) {
        java.util.Set<String> held = new java.util.HashSet<>();
        for (OpcUaNodeBinding b : writeBindings.values()) {
            if (!matchesInterlock(s, b)) continue;
            held.add(b.signalTag);
            Double fs = b.config.failSafeValue();
            if (fs == null) {
                audit.record(Verdict.REJECTED, b.loopId, b.signalTag, null, null,
                        "INTERLOCK_NO_FAILSAFE", currentSafetyMode(b.signalTag).name(), ts, run);
                continue;
            }
            DataValue dv = OpcUaSignalMapper.toDataValue(fs);
            StatusCode sc = svc.writeValue(b.nodeId, dv);
            if (sc != null && sc.isGood()) {
                b.recordWrite(fs, ts);
                audit.record(Verdict.INTERLOCK_TRIP, b.loopId, b.signalTag, null, fs,
                        "INTERLOCK:" + s.getInterlockId(),
                        currentSafetyMode(b.signalTag).name(), ts, run);
            } else {
                audit.record(Verdict.FAILED, b.loopId, b.signalTag, null, fs,
                        "INTERLOCK_WRITE_FAILED:" + (sc == null ? "TRANSPORT" : sc),
                        currentSafetyMode(b.signalTag).name(), ts, run);
            }
        }
        return held;
    }

    private boolean matchesInterlock(InterlockSignal s, OpcUaNodeBinding b) {
        if (s == null || s.getInterlockId() == null) return false;
        return s.getInterlockId().equals(b.loopId) || s.getInterlockId().equals(b.signalTag);
    }

    private void processCommand(Command c, long ts, long run) {
        OpcUaNodeBinding b = writeBindings.get(c.tag());
        SafetyMode mode = currentSafetyMode(c.tag());
        if (b == null) {
            audit.record(Verdict.REJECTED, "?", c.tag(), c.proposed(), null,
                    "UNKNOWN_TAG", mode.name(), ts, run);
            return;
        }
        // Interlock hold: a tripped interlock for this loop in the same tick wins
        if (overrides.isActive(c.tag(), ts)) {
            audit.record(Verdict.OVERRIDE_HOLD, b.loopId, b.signalTag, c.proposed(), null,
                    "OPERATOR_OVERRIDE", mode.name(), ts, run);
            return;
        }
        switch (mode) {
            case SHADOW -> {
                audit.record(Verdict.REJECTED, b.loopId, b.signalTag, c.proposed(), null,
                        "SHADOW_MODE", mode.name(), ts, run);
                return;
            }
            case ADVISORY -> {
                if (!c.execute()) {
                    audit.record(Verdict.REJECTED, b.loopId, b.signalTag, c.proposed(), null,
                            "ADVISORY_REQUIRES_CONFIRMATION", mode.name(), ts, run);
                    return;
                }
            }
            case AUTONOMOUS -> { /* fall through */ }
        }

        // Clamp
        double effective = c.proposed();
        String clampReason = null;
        if (b.config.minClampValue() != null && effective < b.config.minClampValue()) {
            effective = b.config.minClampValue();
            clampReason = "CLAMPED_LOW";
        } else if (b.config.maxClampValue() != null && effective > b.config.maxClampValue()) {
            effective = b.config.maxClampValue();
            clampReason = "CLAMPED_HIGH";
        }

        // Rate limit
        String rateReason = null;
        if (b.config.rampRateMaxPerSec() != null && b.getLastWrittenAt() > 0) {
            double dtSec = Math.max(0.001, (ts - b.getLastWrittenAt()) / 1000.0);
            double maxStep = b.config.rampRateMaxPerSec() * dtSec;
            double last = b.getLastWritten();
            double delta = effective - last;
            if (Math.abs(delta) > maxStep) {
                effective = last + Math.signum(delta) * maxStep;
                rateReason = "RATE_LIMITED";
            }
        }

        // Diff-suppress
        if (b.getLastWrittenAt() > 0
                && Math.abs(effective - b.getLastWritten()) < DIFF_SUPPRESS_EPSILON
                && (ts - b.getLastWrittenAt()) < DIFF_SUPPRESS_WINDOW_MS) {
            audit.record(Verdict.APPLIED, b.loopId, b.signalTag, c.proposed(), effective,
                    "DIFF_SUPPRESSED", mode.name(), ts, run);
            return;
        }

        DataValue dv = OpcUaSignalMapper.toDataValue(effective);
        StatusCode sc = svc.writeValue(b.nodeId, dv);
        String reason = clampReason != null ? clampReason
                : rateReason != null ? rateReason
                : null;
        if (sc != null && sc.isGood()) {
            b.recordWrite(effective, ts);
            audit.record(Verdict.APPLIED, b.loopId, b.signalTag, c.proposed(), effective,
                    reason, mode.name(), ts, run);
        } else {
            audit.record(Verdict.FAILED, b.loopId, b.signalTag, c.proposed(), effective,
                    "WRITE_FAILED:" + (sc == null ? "TRANSPORT" : sc),
                    mode.name(), ts, run);
        }
    }

    private SafetyMode currentSafetyMode(String tag) {
        OpcUaNodeBinding b = writeBindings.get(tag);
        if (b != null) {
            SafetyMode m = cfg.perLoopSafetyMode().get(b.loopId);
            if (m != null) return m;
        }
        return SafetyMode.SHADOW;
    }

    private Partition partition(List<IResult> results) {
        List<InterlockSignal> interlocks = new ArrayList<>();
        List<OperatorOverrideSignal> overridesList = new ArrayList<>();
        List<Command> commands = new ArrayList<>();
        if (results == null) return new Partition(interlocks, overridesList, commands);
        for (IResult r : results) {
            IResultSignal sig = r.getResult();
            if (sig instanceof InterlockSignal il) {
                if (il.isTripped()) interlocks.add(il);
            } else if (sig instanceof OperatorOverrideSignal ov) {
                overridesList.add(ov);
            } else if (sig instanceof SetpointSignal sp) {
                commands.add(new Command(sp.getTag(), sp.getSetpoint(), true));
            } else if (sig instanceof ActuatorCommandSignal ac) {
                commands.add(new Command(ac.getTag(), ac.getTargetValue(), ac.isExecute()));
            }
        }
        return new Partition(interlocks, overridesList, commands);
    }

    @Override
    public void close() {
        // ownership of svc / audit is external
    }

    /* === helper types === */

    private record Partition(List<InterlockSignal> interlocks,
                             List<OperatorOverrideSignal> overrides,
                             List<Command> commands) {}

    private record Command(String tag, double proposed, boolean execute) {}
}
