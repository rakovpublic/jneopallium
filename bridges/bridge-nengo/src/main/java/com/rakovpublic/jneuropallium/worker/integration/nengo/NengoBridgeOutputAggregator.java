/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.integration.nengo;

import com.rakovpublic.jneuropallium.ai.signals.fast.HarmVetoSignal;
import com.rakovpublic.jneuropallium.ai.signals.fast.MotorCommandSignal;
import com.rakovpublic.jneuropallium.ai.signals.fast.TransparencyLogSignal;
import com.rakovpublic.jneuropallium.worker.application.IOutputAggregator;
import com.rakovpublic.jneuropallium.worker.bridge.common.AbstractBridgeAuditOutput;
import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeAuditRecord;
import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeSafetyMode;
import com.rakovpublic.jneuropallium.worker.net.layers.IResult;
import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;
import com.rakovpublic.jneuropallium.worker.util.IContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

/**
 * Nengo egress (15-NENGO.md §5, §6.3).
 *
 * <p>The aggregator does <i>not</i> extend
 * {@link com.rakovpublic.jneuropallium.worker.bridge.common.AbstractBridgeOutputAggregator}
 * because that base operates on the industrial-signal vocabulary
 * ({@code ActuatorCommandSignal} / {@code InterlockSignal} /
 * {@code OperatorOverrideSignal}) and the Nengo integration produces and
 * consumes AI-signal classes ({@code MotorCommandSignal},
 * {@code HarmVetoSignal}). The §0 ground rules are still enforced:
 *
 * <ul>
 *   <li>{@code MotorCommandSignal.execute=false} → audit
 *       {@code REJECTED reason=SHADOW_MODE}; no frame written.</li>
 *   <li>Per-tag effective {@link BridgeSafetyMode}: {@code SHADOW}
 *       rejects; {@code ADVISORY} requires {@code execute=true};
 *       {@code AUTONOMOUS} only loadable when {@code simulatorOnly=true}
 *       (enforced in {@link NengoBridgeConfig}).</li>
 *   <li>{@link HarmVetoSignal} → fail-safe STOP frame with the
 *       configured {@code values} (typically zero); audited as
 *       {@code INTERLOCK_TRIP}.</li>
 *   <li>A {@link TransparencyLogSignal} in the same tick attaches its
 *       id to the next emitted frame (S13).</li>
 *   <li>Output watchdog: if {@code outputDecayMs} elapses without an
 *       approved command, one STOP frame is emitted (S11).</li>
 * </ul>
 */
public final class NengoBridgeOutputAggregator implements IOutputAggregator, AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(NengoBridgeOutputAggregator.class);

    public static final String BRIDGE_NAME = "nengo";
    /** Tag used in audit records for the motor egress channel. */
    public static final String MOTOR_TAG = "ROBOT.MOTOR";

    private final NengoBridgeConfig config;
    private final NengoChannelService channel;
    private final JneopalliumToNengoMapper mapper;
    private final AbstractBridgeAuditOutput audit;

    private long lastAppliedTs = -1L;
    private long tickRun;

    public NengoBridgeOutputAggregator(NengoBridgeConfig config,
                                       NengoChannelService channel,
                                       JneopalliumToNengoMapper mapper,
                                       AbstractBridgeAuditOutput audit) {
        this.config = Objects.requireNonNull(config, "config");
        this.channel = Objects.requireNonNull(channel, "channel");
        this.mapper = Objects.requireNonNull(mapper, "mapper");
        this.audit = Objects.requireNonNull(audit, "audit");
        if (channel.direction() != NengoChannelService.Direction.WRITE) {
            throw new IllegalArgumentException(
                    "NengoBridgeOutputAggregator requires a WRITE channel");
        }
    }

    @Override
    public synchronized void save(List<IResult> results, long timestamp, long run, IContext context) {
        long ts = timestamp <= 0 ? System.currentTimeMillis() : timestamp;
        long tickId = ++tickRun;

        // Two passes: capture any transparency-log id first so it can be
        // attached to the same-tick motor frame.
        if (results != null) {
            for (IResult r : results) {
                if (r == null) continue;
                IResultSignal<?> s = r.getResult();
                if (s instanceof TransparencyLogSignal tx) {
                    mapper.noteTransparency(tx);
                }
            }
        }

        boolean wroteAny = false;
        if (results != null) {
            for (IResult r : results) {
                if (r == null) continue;
                IResultSignal<?> s = r.getResult();
                Long neuronId = r.getNeuronId();
                try {
                    if (s instanceof MotorCommandSignal mc) {
                        wroteAny |= handleMotor(mc, ts, tickId, neuronId);
                    } else if (s instanceof HarmVetoSignal hv) {
                        wroteAny |= handleVeto(hv, ts, tickId, neuronId);
                    }
                } catch (RuntimeException ex) {
                    audit.append(new BridgeAuditRecord(
                            ts, tickId, BRIDGE_NAME, BridgeAuditRecord.Verdict.FAILED,
                            null, MOTOR_TAG, null, null,
                            BridgeAuditRecord.RejectReason.EXCEPTION + ":"
                                    + ex.getClass().getSimpleName(),
                            modeForMotor(),
                            evidence(neuronId)));
                    log.warn("Nengo egress threw: {}", ex.getMessage());
                }
            }
        }

        // Watchdog decay: if no applied output and outputDecayMs elapsed,
        // write a single STOP frame.
        long decayMs = config.watchdog().outputDecayMs();
        if (!wroteAny && lastAppliedTs > 0 && ts - lastAppliedTs >= decayMs) {
            NengoOutputFrame stop = mapper.buildWatchdogStopFrame(ts);
            boolean ok = channel.writeFrame(stop);
            audit.append(new BridgeAuditRecord(
                    ts, tickId, BRIDGE_NAME,
                    ok ? BridgeAuditRecord.Verdict.INTERLOCK_TRIP : BridgeAuditRecord.Verdict.FAILED,
                    null, MOTOR_TAG, null, 0.0,
                    "WATCHDOG_DECAY", modeForMotor(), List.of()));
            // Reset so we don't keep emitting STOP every tick.
            lastAppliedTs = ts;
        }
    }

    private boolean handleMotor(MotorCommandSignal mc, long ts, long run, Long neuronId) {
        BridgeSafetyMode mode = modeForMotor();

        if (mode == BridgeSafetyMode.SHADOW) {
            audit.append(new BridgeAuditRecord(
                    ts, run, BRIDGE_NAME, BridgeAuditRecord.Verdict.REJECTED,
                    null, MOTOR_TAG, null, null,
                    BridgeAuditRecord.RejectReason.SHADOW_MODE, mode,
                    evidence(neuronId)));
            return false;
        }

        if (!mc.isExecute()) {
            audit.append(new BridgeAuditRecord(
                    ts, run, BRIDGE_NAME, BridgeAuditRecord.Verdict.REJECTED,
                    null, MOTOR_TAG, null, null,
                    "EXECUTE_FALSE", mode,
                    evidence(neuronId)));
            return false;
        }

        if (mode == BridgeSafetyMode.AUTONOMOUS && !config.simulatorOnly()) {
            // Belt-and-braces — already enforced at load. If reached here,
            // refuse loudly and audit.
            audit.append(new BridgeAuditRecord(
                    ts, run, BRIDGE_NAME, BridgeAuditRecord.Verdict.REJECTED,
                    null, MOTOR_TAG, null, null,
                    "AUTONOMOUS_REQUIRES_SIMULATOR_ONLY", mode,
                    evidence(neuronId)));
            return false;
        }

        NengoOutputFrame frame = mapper.buildFrame(mc, ts);
        if (frame == null) {
            audit.append(new BridgeAuditRecord(
                    ts, run, BRIDGE_NAME, BridgeAuditRecord.Verdict.REJECTED,
                    null, MOTOR_TAG, null, null,
                    "UNBOUND_SIGNAL_TYPE", mode,
                    evidence(neuronId)));
            return false;
        }
        boolean ok = channel.writeFrame(frame);
        if (ok) lastAppliedTs = ts;
        audit.append(new BridgeAuditRecord(
                ts, run, BRIDGE_NAME,
                ok ? BridgeAuditRecord.Verdict.APPLIED : BridgeAuditRecord.Verdict.FAILED,
                null, MOTOR_TAG, null, magnitudeOrNull(mc),
                ok ? "MOTOR_FRAME" : "CHANNEL_WRITE_FAILED",
                mode, evidence(neuronId)));
        return ok;
    }

    private boolean handleVeto(HarmVetoSignal hv, long ts, long run, Long neuronId) {
        NengoOutputFrame frame = mapper.buildFrame(hv, ts);
        if (frame == null) return false;
        boolean ok = channel.writeFrame(frame);
        if (ok) lastAppliedTs = ts;
        audit.append(new BridgeAuditRecord(
                ts, run, BRIDGE_NAME,
                ok ? BridgeAuditRecord.Verdict.INTERLOCK_TRIP : BridgeAuditRecord.Verdict.FAILED,
                null, MOTOR_TAG, null, 0.0,
                "HARM_VETO:" + (hv.getVetoReason() == null ? "n/a" : hv.getVetoReason()),
                modeForMotor(), evidence(neuronId)));
        return ok;
    }

    private BridgeSafetyMode modeForMotor() {
        return config.perTagSafetyMode().getOrDefault(MOTOR_TAG, BridgeSafetyMode.SHADOW);
    }

    private static Double magnitudeOrNull(MotorCommandSignal mc) {
        double m = mc.getMagnitudeEstimate();
        return Double.isFinite(m) ? m : null;
    }

    private static List<String> evidence(Long neuronId) {
        return neuronId == null ? List.of() : List.of(String.valueOf(neuronId));
    }

    /* ===== test / introspection ============================================ */

    public long lastAppliedTs() { return lastAppliedTs; }
    public NengoBridgeConfig config() { return config; }

    @Override
    public void close() {
        channel.close();
    }
}
