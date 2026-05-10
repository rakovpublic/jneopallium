/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.ros2;

import com.rakovpublic.jneuropallium.ai.signals.fast.HarmVetoSignal;
import com.rakovpublic.jneuropallium.ai.signals.fast.MotorCommandSignal;
import com.rakovpublic.jneuropallium.ai.signals.fast.TransparencyLogSignal;
import com.rakovpublic.jneuropallium.worker.application.IOutputAggregator;
import com.rakovpublic.jneuropallium.worker.bridge.common.AbstractBridgeAuditOutput;
import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeAuditRecord;
import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeSafetyMode;
import com.rakovpublic.jneuropallium.worker.net.layers.IResult;
import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm.FormationSignal;
import com.rakovpublic.jneuropallium.worker.util.IContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

/**
 * {@link IOutputAggregator} for the ROS 2 advisory egress (04-ROS2-DDS.md §3,
 * §5 egress table).
 *
 * <p>Per §3 the bridge's structural ceiling is {@code ADVISORY}: outbound
 * traffic goes only to a configurable advisory namespace consumed by the
 * external autonomy supervisor. The §0.1–§0.3 chain still runs at the
 * tag-level: {@code SHADOW} suppresses publication, and only an
 * {@link MotorCommandSignal} carrying {@code execute=true} is forwarded.
 *
 * <p>Signal handling:
 * <ul>
 *   <li>{@link MotorCommandSignal} → encoded as the binding's {@code msgType}
 *       (typically {@code geometry_msgs/msg/Twist}), clamped on the linear/x
 *       axis when configured.</li>
 *   <li>{@link FormationSignal} → publish slot index as a tiny JSON.</li>
 *   <li>{@link HarmVetoSignal} → publish veto reason on a {@code std_msgs/msg/String}.</li>
 *   <li>{@link TransparencyLogSignal} → publish to {@code audit.advisoryAuditTopic}
 *       when configured (the bridge's audit-mirror channel).</li>
 * </ul>
 *
 * <p>{@code AUTONOMOUS} per-tag promotion is rejected by
 * {@link Ros2BridgeConfig}'s constructor unless {@code simulatorOnly} is
 * set; this aggregator only differentiates {@code SHADOW} (drop) from
 * {@code ADVISORY} / {@code AUTONOMOUS} (publish) at runtime.
 */
public final class Ros2AdvisoryOutputAggregator implements IOutputAggregator {

    private static final Logger log = LoggerFactory.getLogger(Ros2AdvisoryOutputAggregator.class);

    private final Ros2ClientService svc;
    private final AbstractBridgeAuditOutput audit;
    private final Ros2BridgeConfig config;
    private final Ros2MessageMapper mapper;

    public Ros2AdvisoryOutputAggregator(Ros2ClientService svc,
                                        AbstractBridgeAuditOutput audit) {
        this.svc = Objects.requireNonNull(svc, "svc");
        this.audit = Objects.requireNonNull(audit, "audit");
        this.config = svc.config();
        this.mapper = new Ros2MessageMapper();
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
                if (s instanceof MotorCommandSignal mc) {
                    handleMotor(mc, timestamp, run, neuronId);
                } else if (s instanceof FormationSignal fs) {
                    handleFormation(fs, timestamp, run, neuronId);
                } else if (s instanceof HarmVetoSignal hv) {
                    handleVeto(hv, timestamp, run, neuronId);
                } else if (s instanceof TransparencyLogSignal tl) {
                    handleTransparency(tl, timestamp, run, neuronId);
                }
            } catch (RuntimeException ex) {
                audit.append(new BridgeAuditRecord(
                        timestamp, run, Ros2ClientService.BRIDGE_NAME,
                        BridgeAuditRecord.Verdict.FAILED,
                        null, null, null, null,
                        BridgeAuditRecord.RejectReason.EXCEPTION + ":" + ex.getClass().getSimpleName(),
                        null, evidence(neuronId)));
            }
        }
    }

    /* ===== command paths ====================================================== */

    private void handleMotor(MotorCommandSignal mc, long ts, long run, Long neuronId) {
        // §0.1: only forward when planning chain has set execute=true.
        if (!mc.isExecute()) {
            audit.append(new BridgeAuditRecord(
                    ts, run, Ros2ClientService.BRIDGE_NAME,
                    BridgeAuditRecord.Verdict.REJECTED,
                    null, mc.getName(), magnitude(mc), null,
                    BridgeAuditRecord.RejectReason.SHADOW_MODE,
                    BridgeSafetyMode.SHADOW, evidence(neuronId)));
            return;
        }
        String tag = mc.getName();
        Ros2TopicBinding b = tag == null ? null : svc.writeBindingForTag(tag);
        if (b == null) {
            audit.append(new BridgeAuditRecord(
                    ts, run, Ros2ClientService.BRIDGE_NAME,
                    BridgeAuditRecord.Verdict.REJECTED,
                    null, tag, magnitude(mc), null,
                    BridgeAuditRecord.RejectReason.UNKNOWN_TAG, null, evidence(neuronId)));
            return;
        }

        BridgeSafetyMode mode = config.perTagSafetyMode()
                .getOrDefault(b.bindingId(), BridgeSafetyMode.ADVISORY);
        if (mode == BridgeSafetyMode.SHADOW) {
            audit.append(new BridgeAuditRecord(
                    ts, run, Ros2ClientService.BRIDGE_NAME,
                    BridgeAuditRecord.Verdict.REJECTED,
                    b.loopId(), tag, magnitude(mc), null,
                    BridgeAuditRecord.RejectReason.SHADOW_MODE, mode, evidence(neuronId)));
            return;
        }

        // Clamp the dominant linear axis on a Twist; for other shapes, clamp the L2 magnitude.
        double[] params = mc.getParams();
        String modifyReason = null;
        if (params != null && params.length > 0) {
            double primary = params[0];
            if (b.maxClampValue() != null && primary > b.maxClampValue()) {
                params[0] = b.maxClampValue();
                modifyReason = BridgeAuditRecord.ModifyReason.CLAMPED_HIGH;
            } else if (b.minClampValue() != null && primary < b.minClampValue()) {
                params[0] = b.minClampValue();
                modifyReason = BridgeAuditRecord.ModifyReason.CLAMPED_LOW;
            }
            mc.setParams(params);
        }

        String json = encode(b, mc);
        boolean ok = svc.publish(b.bindingId(), json, ts, run, tag);
        if (ok) {
            audit.append(new BridgeAuditRecord(
                    ts, run, Ros2ClientService.BRIDGE_NAME,
                    BridgeAuditRecord.Verdict.APPLIED,
                    b.loopId(), tag, magnitude(mc), magnitude(mc),
                    modifyReason, mode, evidence(neuronId)));
        }
    }

    private String encode(Ros2TopicBinding b, MotorCommandSignal mc) {
        return switch (b.msgType()) {
            case "geometry_msgs/msg/Twist" -> mapper.encodeTwist(mc);
            case "std_msgs/msg/Float64" -> mapper.encodeStdFloat64(magnitude(mc));
            default -> mapper.encodeStdString("MotorCommand:" + b.signalTag());
        };
    }

    private void handleFormation(FormationSignal fs, long ts, long run, Long neuronId) {
        String tag = fs.getName();
        Ros2TopicBinding b = tag == null ? null : svc.writeBindingForTag(tag);
        if (b == null) return;
        BridgeSafetyMode mode = config.perTagSafetyMode()
                .getOrDefault(b.bindingId(), BridgeSafetyMode.ADVISORY);
        if (mode == BridgeSafetyMode.SHADOW) {
            audit.append(new BridgeAuditRecord(
                    ts, run, Ros2ClientService.BRIDGE_NAME,
                    BridgeAuditRecord.Verdict.REJECTED,
                    b.loopId(), tag, (double) fs.getSlotIndex(), null,
                    BridgeAuditRecord.RejectReason.SHADOW_MODE, mode, evidence(neuronId)));
            return;
        }
        String body = "formation=" + fs.getTemplate() + " slot=" + fs.getSlotIndex();
        boolean ok = svc.publish(b.bindingId(), mapper.encodeStdString(body), ts, run, tag);
        if (ok) {
            audit.append(new BridgeAuditRecord(
                    ts, run, Ros2ClientService.BRIDGE_NAME,
                    BridgeAuditRecord.Verdict.APPLIED,
                    b.loopId(), tag, (double) fs.getSlotIndex(), (double) fs.getSlotIndex(),
                    null, mode, evidence(neuronId)));
        }
    }

    private void handleVeto(HarmVetoSignal hv, long ts, long run, Long neuronId) {
        // The veto channel is highest-priority; bind by the signal's name (per §5).
        String tag = hv.getName();
        Ros2TopicBinding b = tag == null ? null : svc.writeBindingForTag(tag);
        if (b == null) return;
        BridgeSafetyMode mode = config.perTagSafetyMode()
                .getOrDefault(b.bindingId(), BridgeSafetyMode.ADVISORY);
        if (mode == BridgeSafetyMode.SHADOW) {
            audit.append(new BridgeAuditRecord(
                    ts, run, Ros2ClientService.BRIDGE_NAME,
                    BridgeAuditRecord.Verdict.REJECTED,
                    b.loopId(), tag, null, null,
                    BridgeAuditRecord.RejectReason.SHADOW_MODE, mode, evidence(neuronId)));
            return;
        }
        String reason = hv.getVetoReason() == null ? "VETO" : hv.getVetoReason();
        boolean ok = svc.publish(b.bindingId(), mapper.encodeStdString(reason), ts, run, tag);
        if (ok) {
            audit.append(new BridgeAuditRecord(
                    ts, run, Ros2ClientService.BRIDGE_NAME,
                    BridgeAuditRecord.Verdict.APPLIED,
                    b.loopId(), tag, null, null, null, mode, evidence(neuronId)));
        }
    }

    private void handleTransparency(TransparencyLogSignal tl, long ts, long run, Long neuronId) {
        if (config.audit() == null
                || config.audit().advisoryAuditTopic() == null
                || config.audit().advisoryAuditTopic().isBlank()) {
            return;
        }
        String body = "actionPlan=" + tl.getActionPlanId() + " verdict=" + tl.getVerdict();
        svc.publishRaw(config.audit().advisoryAuditTopic(), mapper.encodeStdString(body));
        audit.append(new BridgeAuditRecord(
                ts, run, Ros2ClientService.BRIDGE_NAME,
                BridgeAuditRecord.Verdict.APPLIED,
                null, "AUDIT", null, null, null, null, evidence(neuronId)));
    }

    /* ===== helpers ============================================================ */

    private static Double magnitude(MotorCommandSignal mc) {
        return mc == null ? null : mc.getMagnitudeEstimate();
    }

    private static List<String> evidence(Long neuronId) {
        return neuronId == null ? List.of() : List.of(String.valueOf(neuronId));
    }
}
