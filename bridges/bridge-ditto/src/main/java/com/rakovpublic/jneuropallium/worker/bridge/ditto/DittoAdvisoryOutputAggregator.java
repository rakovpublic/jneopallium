/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.ditto;

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

import java.util.List;
import java.util.Objects;

/**
 * {@link IOutputAggregator} for the Ditto advisory egress
 * (10-DITTO.md §4 egress table).
 *
 * <p>The bridge ceiling is structurally {@code ADVISORY} — the §0.1–§0.3
 * autonomous-write algorithm doesn't apply here, but per-tag {@code SHADOW}
 * still suppresses outbound traffic, the binding's
 * {@code [minClampValue, maxClampValue]} is applied before write, and the
 * {@code recommended_*} / {@code advisory_*} feature-name guard is
 * re-enforced at runtime by {@link DittoClientService#writeProperty}.
 *
 * <p>Signal handling:
 * <ul>
 *   <li>{@link SetpointSignal} → modify the configured advisory feature
 *       property on the same thing.</li>
 *   <li>{@link ActuatorCommandSignal} (with {@code execute=true}) → same.</li>
 * </ul>
 *
 * <p>{@code AUTONOMOUS} per-tag safety mode is rejected by
 * {@link DittoBridgeConfig}'s constructor; this aggregator therefore only
 * needs to differentiate {@code SHADOW} (drop) from {@code ADVISORY}
 * (write).
 */
public final class DittoAdvisoryOutputAggregator implements IOutputAggregator {

    private static final Logger log = LoggerFactory.getLogger(DittoAdvisoryOutputAggregator.class);

    private final DittoClientService svc;
    private final AbstractBridgeAuditOutput audit;
    private final DittoBridgeConfig config;

    public DittoAdvisoryOutputAggregator(DittoClientService svc,
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
                }
            } catch (RuntimeException ex) {
                audit.append(new BridgeAuditRecord(
                        timestamp, run, DittoClientService.BRIDGE_NAME,
                        BridgeAuditRecord.Verdict.FAILED,
                        null, tagOf(s), null, null,
                        BridgeAuditRecord.RejectReason.EXCEPTION + ":" + ex.getClass().getSimpleName(),
                        null, evidence(neuronId)));
            }
        }
    }

    private void handleNumeric(String tag, double proposed, long ts, long run, Long neuronId) {
        if (tag == null) {
            audit.append(new BridgeAuditRecord(
                    ts, run, DittoClientService.BRIDGE_NAME,
                    BridgeAuditRecord.Verdict.REJECTED,
                    null, null, proposed, null,
                    BridgeAuditRecord.RejectReason.UNKNOWN_TAG, null, evidence(neuronId)));
            return;
        }
        DittoFeatureBinding b = svc.writeBindingForTag(tag);
        if (b == null) {
            audit.append(new BridgeAuditRecord(
                    ts, run, DittoClientService.BRIDGE_NAME,
                    BridgeAuditRecord.Verdict.REJECTED,
                    null, tag, proposed, null,
                    BridgeAuditRecord.RejectReason.UNKNOWN_TAG, null, evidence(neuronId)));
            return;
        }

        BridgeSafetyMode mode = config.perTagSafetyMode().getOrDefault(b.bindingId(), BridgeSafetyMode.ADVISORY);
        if (mode == BridgeSafetyMode.SHADOW) {
            audit.append(new BridgeAuditRecord(
                    ts, run, DittoClientService.BRIDGE_NAME,
                    BridgeAuditRecord.Verdict.REJECTED,
                    b.loopId(), tag, proposed, null,
                    BridgeAuditRecord.RejectReason.SHADOW_MODE, mode, evidence(neuronId)));
            return;
        }

        // Clamp before writing (§4 — clamps still apply on advisory channel).
        String modifyReason = null;
        double effective = proposed;
        if (b.maxClampValue() != null && effective > b.maxClampValue()) {
            effective = b.maxClampValue();
            modifyReason = BridgeAuditRecord.ModifyReason.CLAMPED_HIGH;
        } else if (b.minClampValue() != null && effective < b.minClampValue()) {
            effective = b.minClampValue();
            modifyReason = BridgeAuditRecord.ModifyReason.CLAMPED_LOW;
        }

        boolean ok = svc.writeProperty(b.bindingId(), effective, ts, run, tag);
        if (ok) {
            audit.append(new BridgeAuditRecord(
                    ts, run, DittoClientService.BRIDGE_NAME,
                    BridgeAuditRecord.Verdict.APPLIED,
                    b.loopId(), tag, proposed, effective,
                    modifyReason, mode, evidence(neuronId)));
        }
        // Failure / non-advisory paths are audited inside DittoClientService.writeProperty.
    }

    private void handleActuator(ActuatorCommandSignal ac, long ts, long run, Long neuronId) {
        String tag = ac.getTag();
        if (!ac.isExecute()) {
            audit.append(new BridgeAuditRecord(
                    ts, run, DittoClientService.BRIDGE_NAME,
                    BridgeAuditRecord.Verdict.REJECTED,
                    null, tag, ac.getTargetValue(), null,
                    BridgeAuditRecord.RejectReason.SHADOW_MODE, BridgeSafetyMode.SHADOW,
                    evidence(neuronId)));
            return;
        }
        handleNumeric(tag, ac.getTargetValue(), ts, run, neuronId);
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
