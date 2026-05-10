/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.mavlink;

import com.rakovpublic.jneuropallium.ai.signals.fast.HarmVetoSignal;
import com.rakovpublic.jneuropallium.ai.signals.fast.TransparencyLogSignal;
import com.rakovpublic.jneuropallium.worker.application.IOutputAggregator;
import com.rakovpublic.jneuropallium.worker.bridge.common.AbstractBridgeAuditOutput;
import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeAuditRecord;
import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeSafetyMode;
import com.rakovpublic.jneuropallium.worker.net.layers.IResult;
import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm.FormationSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm.TaskAnnouncementSignal;
import com.rakovpublic.jneuropallium.worker.util.IContext;
import io.dronefleet.mavlink.common.MavSeverity;
import io.dronefleet.mavlink.common.NamedValueFloat;
import io.dronefleet.mavlink.common.Statustext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

/**
 * {@link IOutputAggregator} for the MAVLink advisory egress (12-MAVLINK.md §3,
 * §5 egress table).
 *
 * <p>Per §3 the bridge's structural ceiling is <b>SIM-ONLY</b>: outbound
 * traffic is restricted to the advisory message types {@code STATUSTEXT},
 * {@code NAMED_VALUE_FLOAT}, and the custom {@code JNEO_*} dialect. Writes
 * to actuating message types ({@code COMMAND_LONG}, {@code SET_MODE}, etc.)
 * are rejected at config load and again at runtime by
 * {@link MavlinkClientService}.
 *
 * <p>This implementation encodes all advisory egress as the dronefleet
 * {@link Statustext} or {@link NamedValueFloat} message records so the
 * bridge ships with a working advisory channel out of the box. The custom
 * {@code JNEO_*} dialect classes are stubbed via {@code STATUSTEXT} payloads
 * with a structured {@code "JNEO_FORMATION:..."} prefix; once the
 * {@code jneo.xml} dialect is generated and committed (§7), the encoder
 * helpers in {@link MavlinkAdvisoryEncoder} are the single place to swap.
 *
 * <p>{@link TransparencyLogSignal} is intentionally NOT forwarded onto the
 * MAVLink wire — §5 keeps the audit trail off the flight bus. The signal
 * is dropped here so it does not accidentally reach the autopilot.
 */
public final class MavlinkAdvisoryOutputAggregator implements IOutputAggregator {

    private static final Logger log = LoggerFactory.getLogger(MavlinkAdvisoryOutputAggregator.class);

    private final MavlinkClientService svc;
    private final AbstractBridgeAuditOutput audit;
    private final MavlinkBridgeConfig config;
    private final MavlinkAdvisoryEncoder encoder;

    public MavlinkAdvisoryOutputAggregator(MavlinkClientService svc,
                                           AbstractBridgeAuditOutput audit) {
        this.svc = Objects.requireNonNull(svc, "svc");
        this.audit = Objects.requireNonNull(audit, "audit");
        this.config = svc.config();
        this.encoder = new MavlinkAdvisoryEncoder();
    }

    @Override
    public void save(List<IResult> results, long timestamp, long run, IContext context) {
        if (results == null || results.isEmpty()) return;
        for (IResult r : results) {
            if (r == null) continue;
            IResultSignal<?> s = r.getResult();
            if (s == null) continue;
            Long neuronId = r.getNeuronId();
            try {
                if (s instanceof FormationSignal fs) {
                    handleFormation(fs, timestamp, run, neuronId);
                } else if (s instanceof TaskAnnouncementSignal ta) {
                    handleTaskAnnouncement(ta, timestamp, run, neuronId);
                } else if (s instanceof HarmVetoSignal hv) {
                    handleVeto(hv, timestamp, run, neuronId);
                } else if (s instanceof TransparencyLogSignal) {
                    // §5: keep audit OFF the flight bus. Silently drop.
                    log.debug("TransparencyLogSignal not forwarded to MAVLink (12-MAVLINK.md §5)");
                }
            } catch (RuntimeException ex) {
                audit.append(new BridgeAuditRecord(
                        timestamp, run, MavlinkClientService.BRIDGE_NAME,
                        BridgeAuditRecord.Verdict.FAILED,
                        null, null, null, null,
                        BridgeAuditRecord.RejectReason.EXCEPTION + ":" + ex.getClass().getSimpleName(),
                        null, evidence(neuronId)));
            }
        }
    }

    /* ===== command paths ============================================== */

    private void handleFormation(FormationSignal fs, long ts, long run, Long neuronId) {
        String tag = fs.getName();
        MavlinkMessageBinding b = tag == null ? null : svc.writeBindingForTag(tag);
        if (b == null) {
            audit.append(new BridgeAuditRecord(
                    ts, run, MavlinkClientService.BRIDGE_NAME,
                    BridgeAuditRecord.Verdict.REJECTED,
                    null, tag, (double) fs.getSlotIndex(), null,
                    BridgeAuditRecord.RejectReason.UNKNOWN_TAG, null, evidence(neuronId)));
            return;
        }
        BridgeSafetyMode mode = config.perTagSafetyMode()
                .getOrDefault(b.bindingId(), BridgeSafetyMode.ADVISORY);
        if (mode == BridgeSafetyMode.SHADOW) {
            audit.append(new BridgeAuditRecord(
                    ts, run, MavlinkClientService.BRIDGE_NAME,
                    BridgeAuditRecord.Verdict.REJECTED,
                    b.loopId(), tag, (double) fs.getSlotIndex(), null,
                    BridgeAuditRecord.RejectReason.SHADOW_MODE, mode, evidence(neuronId)));
            return;
        }
        Object payload = encoder.encodeFormation(fs);
        boolean ok = svc.send(b.bindingId(), payload, ts, run);
        if (ok) {
            audit.append(new BridgeAuditRecord(
                    ts, run, MavlinkClientService.BRIDGE_NAME,
                    BridgeAuditRecord.Verdict.APPLIED,
                    b.loopId(), tag, (double) fs.getSlotIndex(), (double) fs.getSlotIndex(),
                    null, mode, evidence(neuronId)));
        }
    }

    private void handleTaskAnnouncement(TaskAnnouncementSignal ta, long ts, long run, Long neuronId) {
        String tag = ta.getName();
        MavlinkMessageBinding b = tag == null ? null : svc.writeBindingForTag(tag);
        if (b == null) return;
        BridgeSafetyMode mode = config.perTagSafetyMode()
                .getOrDefault(b.bindingId(), BridgeSafetyMode.ADVISORY);
        if (mode == BridgeSafetyMode.SHADOW) {
            audit.append(new BridgeAuditRecord(
                    ts, run, MavlinkClientService.BRIDGE_NAME,
                    BridgeAuditRecord.Verdict.REJECTED,
                    b.loopId(), tag, ta.getReward(), null,
                    BridgeAuditRecord.RejectReason.SHADOW_MODE, mode, evidence(neuronId)));
            return;
        }
        Object payload = encoder.encodeTaskAnnouncement(ta);
        boolean ok = svc.send(b.bindingId(), payload, ts, run);
        if (ok) {
            audit.append(new BridgeAuditRecord(
                    ts, run, MavlinkClientService.BRIDGE_NAME,
                    BridgeAuditRecord.Verdict.APPLIED,
                    b.loopId(), tag, ta.getReward(), ta.getReward(),
                    null, mode, evidence(neuronId)));
        }
    }

    private void handleVeto(HarmVetoSignal hv, long ts, long run, Long neuronId) {
        // §5 — operator visibility on STATUSTEXT(severity=CRITICAL).
        String tag = hv.getName();
        MavlinkMessageBinding b = tag == null ? null : svc.writeBindingForTag(tag);
        if (b == null) return;
        BridgeSafetyMode mode = config.perTagSafetyMode()
                .getOrDefault(b.bindingId(), BridgeSafetyMode.ADVISORY);
        if (mode == BridgeSafetyMode.SHADOW) {
            audit.append(new BridgeAuditRecord(
                    ts, run, MavlinkClientService.BRIDGE_NAME,
                    BridgeAuditRecord.Verdict.REJECTED,
                    b.loopId(), tag, null, null,
                    BridgeAuditRecord.RejectReason.SHADOW_MODE, mode, evidence(neuronId)));
            return;
        }
        Statustext payload = encoder.encodeHarmVeto(hv);
        boolean ok = svc.send(b.bindingId(), payload, ts, run);
        if (ok) {
            audit.append(new BridgeAuditRecord(
                    ts, run, MavlinkClientService.BRIDGE_NAME,
                    BridgeAuditRecord.Verdict.APPLIED,
                    b.loopId(), tag, null, null, null, mode, evidence(neuronId)));
        }
    }

    private static List<String> evidence(Long neuronId) {
        return neuronId == null ? List.of() : List.of(String.valueOf(neuronId));
    }

    /**
     * Centralised payload builders (12-MAVLINK.md §5, §7). Once the
     * {@code jneo.xml} dialect is generated, swap the {@code Statustext}
     * stubs here for the corresponding {@code JneoFormation},
     * {@code JneoTaskAnnouncement}, {@code JneoHarmVeto} payloads.
     */
    public static final class MavlinkAdvisoryEncoder {

        /** Truncate to the STATUSTEXT 50-byte limit. */
        private static String fit(String s) {
            if (s == null) return "";
            return s.length() > 50 ? s.substring(0, 50) : s;
        }

        public Statustext encodeFormation(FormationSignal fs) {
            String body = "JNEO_FORMATION:"
                    + (fs.getTemplate() == null ? "FREE" : fs.getTemplate().name())
                    + ":slot=" + fs.getSlotIndex();
            return Statustext.builder()
                    .severity(MavSeverity.MAV_SEVERITY_INFO)
                    .text(fit(body))
                    .build();
        }

        public Statustext encodeTaskAnnouncement(TaskAnnouncementSignal ta) {
            String body = "JNEO_TASK_ANN:" + ta.getTaskId()
                    + ":kind=" + (ta.getKind() == null ? "EXPLORE" : ta.getKind().name())
                    + ":r=" + ta.getReward();
            return Statustext.builder()
                    .severity(MavSeverity.MAV_SEVERITY_INFO)
                    .text(fit(body))
                    .build();
        }

        public Statustext encodeHarmVeto(HarmVetoSignal hv) {
            String reason = hv.getVetoReason() == null ? "VETO" : hv.getVetoReason();
            return Statustext.builder()
                    .severity(MavSeverity.MAV_SEVERITY_CRITICAL)
                    .text(fit("JNEO_HARM_VETO:" + reason))
                    .build();
        }

        public NamedValueFloat encodeNamedValue(String name, float value) {
            return NamedValueFloat.builder()
                    .timeBootMs(System.currentTimeMillis())
                    .name(name == null ? "" : (name.length() > 10 ? name.substring(0, 10) : name))
                    .value(value)
                    .build();
        }
    }
}
