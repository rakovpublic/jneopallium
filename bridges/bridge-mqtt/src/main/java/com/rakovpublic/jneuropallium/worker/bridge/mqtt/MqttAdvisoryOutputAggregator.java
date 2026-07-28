/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.rakovpublic.jneuropallium.ai.signals.fast.TransparencyLogSignal;
import com.rakovpublic.jneuropallium.worker.application.IOutputAggregator;
import com.rakovpublic.jneuropallium.worker.bridge.common.AbstractBridgeAuditOutput;
import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeAuditRecord;
import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeSafetyMode;
import com.rakovpublic.jneuropallium.worker.net.layers.IResult;
import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.ActuatorCommandSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.SetpointSignal;
import com.rakovpublic.jneuropallium.worker.util.IContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * {@link IOutputAggregator} for the MQTT advisory egress
 * (02-MQTT-SPARKPLUG.md §3, §5 egress table).
 *
 * <p>The bridge ceiling is structurally {@code ADVISORY} — the §0.1–§0.3
 * autonomous-write algorithm doesn't apply here, but per-tag {@code SHADOW}
 * still suppresses outbound traffic, and the binding's
 * {@code [minClampValue, maxClampValue]} is applied before publish (§6).
 *
 * <p>Signal handling:
 * <ul>
 *   <li>{@link SetpointSignal} → publish target on advisory topic.</li>
 *   <li>{@link ActuatorCommandSignal} (with {@code execute=true}) → same.</li>
 *   <li>{@link TransparencyLogSignal} → publish to {@code audit.mqttAuditTopic}
 *       when configured (the bridge's audit-mirror channel).</li>
 * </ul>
 *
 * <p>{@code AUTONOMOUS} per-tag safety mode is rejected by
 * {@link MqttBridgeConfig}'s constructor; this aggregator therefore only
 * needs to differentiate {@code SHADOW} (drop) from {@code ADVISORY}
 * (publish).
 */
public final class MqttAdvisoryOutputAggregator implements IOutputAggregator {

    private static final Logger log = LoggerFactory.getLogger(MqttAdvisoryOutputAggregator.class);
    private static final ObjectMapper JSON =
            new ObjectMapper().disable(SerializationFeature.INDENT_OUTPUT);

    private final MqttClientService svc;
    private final AbstractBridgeAuditOutput audit;
    private final MqttBridgeConfig config;

    public MqttAdvisoryOutputAggregator(MqttClientService svc,
                                        AbstractBridgeAuditOutput audit) {
        this.svc = Objects.requireNonNull(svc, "svc");
        this.audit = Objects.requireNonNull(audit, "audit");
        this.config = svc.config();
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
                if (s instanceof SetpointSignal sp) {
                    handleNumeric(sp.getTag(), sp.getSetpoint(), timestamp, run, neuronId);
                } else if (s instanceof ActuatorCommandSignal ac) {
                    handleActuator(ac, timestamp, run, neuronId);
                } else if (s instanceof TransparencyLogSignal tl) {
                    handleTransparency(tl, timestamp, run, neuronId);
                }
            } catch (RuntimeException ex) {
                audit.append(new BridgeAuditRecord(
                        timestamp, run, MqttClientService.BRIDGE_NAME,
                        BridgeAuditRecord.Verdict.FAILED,
                        null, tagOf(s), null, null,
                        BridgeAuditRecord.RejectReason.EXCEPTION + ":" + ex.getClass().getSimpleName(),
                        null, evidence(neuronId)));
            }
        }
    }

    /* ===== command paths ====================================================== */

    private void handleNumeric(String tag, double proposed, long ts, long run, Long neuronId) {
        if (tag == null) {
            audit.append(new BridgeAuditRecord(
                    ts, run, MqttClientService.BRIDGE_NAME,
                    BridgeAuditRecord.Verdict.REJECTED,
                    null, null, proposed, null,
                    BridgeAuditRecord.RejectReason.UNKNOWN_TAG, null, evidence(neuronId)));
            return;
        }
        MqttTopicBinding b = svc.writeBindingForTag(tag);
        if (b == null) {
            audit.append(new BridgeAuditRecord(
                    ts, run, MqttClientService.BRIDGE_NAME,
                    BridgeAuditRecord.Verdict.REJECTED,
                    null, tag, proposed, null,
                    BridgeAuditRecord.RejectReason.UNKNOWN_TAG, null, evidence(neuronId)));
            return;
        }

        BridgeSafetyMode mode = config.perTagSafetyMode().getOrDefault(b.bindingId(), BridgeSafetyMode.ADVISORY);
        if (mode == BridgeSafetyMode.SHADOW) {
            audit.append(new BridgeAuditRecord(
                    ts, run, MqttClientService.BRIDGE_NAME,
                    BridgeAuditRecord.Verdict.REJECTED,
                    b.loopId(), tag, proposed, null,
                    BridgeAuditRecord.RejectReason.SHADOW_MODE, mode, evidence(neuronId)));
            return;
        }

        // §6 clamps
        String modifyReason = null;
        double effective = proposed;
        if (b.maxClampValue() != null && effective > b.maxClampValue()) {
            effective = b.maxClampValue();
            modifyReason = BridgeAuditRecord.ModifyReason.CLAMPED_HIGH;
        } else if (b.minClampValue() != null && effective < b.minClampValue()) {
            effective = b.minClampValue();
            modifyReason = BridgeAuditRecord.ModifyReason.CLAMPED_LOW;
        }

        boolean ok = svc.publish(b.bindingId(), effective, ts, run, tag);
        if (ok) {
            audit.append(new BridgeAuditRecord(
                    ts, run, MqttClientService.BRIDGE_NAME,
                    BridgeAuditRecord.Verdict.APPLIED,
                    b.loopId(), tag, proposed, effective,
                    modifyReason, mode, evidence(neuronId)));
        }
        // Failure path is audited by MqttClientService.publish itself.
    }

    private void handleActuator(ActuatorCommandSignal ac, long ts, long run, Long neuronId) {
        String tag = ac.getTag();
        if (!ac.isExecute()) {
            // Caller marked the signal as shadow at the source; record the hold and bail.
            audit.append(new BridgeAuditRecord(
                    ts, run, MqttClientService.BRIDGE_NAME,
                    BridgeAuditRecord.Verdict.REJECTED,
                    null, tag, ac.getTargetValue(), null,
                    BridgeAuditRecord.RejectReason.SHADOW_MODE, BridgeSafetyMode.SHADOW,
                    evidence(neuronId)));
            return;
        }
        handleNumeric(tag, ac.getTargetValue(), ts, run, neuronId);
    }

    private void handleTransparency(TransparencyLogSignal tl, long ts, long run, Long neuronId) {
        if (config.audit() == null
                || config.audit().mqttAuditTopic() == null
                || config.audit().mqttAuditTopic().isBlank()) {
            return;  // no audit channel configured
        }
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("type", TransparencyLogSignal.class.getSimpleName());
        dto.put("actionPlanId", tl.getActionPlanId());
        dto.put("verdict", tl.getVerdict() == null ? null : tl.getVerdict().name());
        dto.put("reason", tl.getDiscriminatorReason());
        dto.put("evidence", tl.getEvidenceNeuronIds());
        dto.put("ts", tl.getTimestamp());
        try {
            byte[] payload = JSON.writeValueAsBytes(dto);
            svc.publishRaw(config.audit().mqttAuditTopic(), payload, config.audit().mqttAuditQos());
            audit.append(new BridgeAuditRecord(
                    ts, run, MqttClientService.BRIDGE_NAME,
                    BridgeAuditRecord.Verdict.APPLIED,
                    null, "AUDIT", null, null, null, null, evidence(neuronId)));
        } catch (Exception ex) {
            audit.append(new BridgeAuditRecord(
                    ts, run, MqttClientService.BRIDGE_NAME,
                    BridgeAuditRecord.Verdict.FAILED,
                    null, "AUDIT", null, null,
                    MqttClientService.Reason.PUBLISH_ERROR + ":" + ex.getMessage(),
                    null, evidence(neuronId)));
        }
    }

    /* ===== helpers ============================================================ */

    private static String tagOf(IResultSignal<?> s) {
        if (s instanceof SetpointSignal sp) return sp.getTag();
        if (s instanceof ActuatorCommandSignal ac) return ac.getTag();
        return null;
    }

    private static List<String> evidence(Long neuronId) {
        return neuronId == null ? List.of() : List.of(String.valueOf(neuronId));
    }
}
