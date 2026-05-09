/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.util.LinkedHashMap;
import com.rakovpublic.jneuropallium.ai.signals.fast.TransparencyLogSignal;
import com.rakovpublic.jneuropallium.worker.application.IOutputAggregator;
import com.rakovpublic.jneuropallium.worker.bridge.common.AbstractBridgeAuditOutput;
import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeAuditRecord;
import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeSafetyMode;
import com.rakovpublic.jneuropallium.worker.net.layers.IResult;
import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.IncidentReportSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.QuarantineRequestSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.ThreatHypothesisSignal;
import com.rakovpublic.jneuropallium.worker.util.IContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * {@link IOutputAggregator} for the Kafka advisory egress (08-KAFKA.md §3,
 * §4 egress table).
 *
 * <p>Unlike the industrial bridges, Kafka writes are pure messages: there is
 * no fail-safe value, no clamp range, no rate-limit. The aggregator
 * therefore inspects each result signal and, if it matches a known advisory
 * signal class with a configured write binding, encodes it to JSON and
 * hands it to {@link KafkaClientService#publish}.
 *
 * <p>The §1 safety ceiling for this bridge is {@code ADVISORY}; any
 * {@code AUTONOMOUS} promotion is rejected by {@link KafkaBridgeConfig}'s
 * compact constructor.
 *
 * <p>Default mapping (overridable in {@code perTagSafetyMode} and
 * {@code writes:} config):
 * <ul>
 *   <li>{@link IncidentReportSignal} → {@code jneo.advisory.incidents}</li>
 *   <li>{@link QuarantineRequestSignal} → {@code jneo.advisory.quarantine_requests}</li>
 *   <li>{@link ThreatHypothesisSignal} → {@code jneo.advisory.hypotheses}</li>
 *   <li>{@link TransparencyLogSignal} → {@code jneo.advisory.audit}</li>
 * </ul>
 */
public final class KafkaAdvisoryOutputAggregator implements IOutputAggregator {

    private static final Logger log = LoggerFactory.getLogger(KafkaAdvisoryOutputAggregator.class);

    public static final String DEFAULT_INCIDENT_TAG    = "SEC.INCIDENT";
    public static final String DEFAULT_QUARANTINE_TAG  = "SEC.QUARANTINE";
    public static final String DEFAULT_HYPOTHESIS_TAG  = "SEC.HYPOTHESIS";
    public static final String DEFAULT_TRANSPARENCY_TAG = "SEC.AUDIT";

    private final KafkaClientService svc;
    private final AbstractBridgeAuditOutput audit;
    private final Map<String, BridgeSafetyMode> perTag;
    private final Map<Class<? extends ISignal<?>>, String> bindingByClass;
    private final ObjectMapper json = new ObjectMapper().disable(SerializationFeature.INDENT_OUTPUT);

    public KafkaAdvisoryOutputAggregator(KafkaClientService svc,
                                         KafkaBridgeConfig config,
                                         AbstractBridgeAuditOutput audit) {
        this.svc = Objects.requireNonNull(svc, "svc");
        this.audit = Objects.requireNonNull(audit, "audit");
        this.perTag = config.perTagSafetyMode();
        this.bindingByClass = resolveBindings(config);
    }

    @Override
    public void save(List<IResult> results, long timestamp, long run, IContext context) {
        if (results == null || results.isEmpty()) return;
        for (IResult r : results) {
            if (r == null) continue;
            IResultSignal<?> s = r.getResult();
            if (s == null) continue;

            String bindingId = bindingByClass.get(s.getClass());
            if (bindingId == null) continue;  // not an advisory signal we publish

            String signalTag = signalTagFor(s);

            // §1 safety ceiling: ADVISORY. SHADOW silences outbound traffic;
            // AUTONOMOUS would be rejected by config validation (defence in depth).
            BridgeSafetyMode mode = perTag.getOrDefault(signalTag, BridgeSafetyMode.ADVISORY);
            if (mode == BridgeSafetyMode.SHADOW) {
                audit.append(new BridgeAuditRecord(
                        timestamp, run, KafkaClientService.BRIDGE_NAME,
                        BridgeAuditRecord.Verdict.REJECTED,
                        bindingId, signalTag, null, null,
                        BridgeAuditRecord.RejectReason.SHADOW_MODE, mode,
                        evidenceOf(r)));
                continue;
            }

            byte[] value;
            try {
                value = json.writeValueAsBytes(toDto(s));
            } catch (Exception ex) {
                audit.append(new BridgeAuditRecord(
                        timestamp, run, KafkaClientService.BRIDGE_NAME,
                        BridgeAuditRecord.Verdict.FAILED,
                        bindingId, signalTag, null, null,
                        "ENCODE_ERROR:" + ex.getMessage(), mode,
                        evidenceOf(r)));
                continue;
            }

            String key = keyOf(s);
            svc.publish(bindingId, key, value, run, signalTag);
        }
    }

    /** Convenience for direct API users that call publish() outside the worker tick. */
    public void publishOne(IResultSignal<?> s, long run) {
        if (s == null) return;
        String bindingId = bindingByClass.get(s.getClass());
        if (bindingId == null) {
            log.debug("No binding for {}; dropping", s.getClass().getSimpleName());
            return;
        }
        try {
            svc.publish(bindingId, keyOf(s), json.writeValueAsBytes(toDto(s)), run, signalTagFor(s));
        } catch (Exception ex) {
            log.warn("publishOne failed for {}: {}", s.getClass().getSimpleName(), ex.getMessage());
        }
    }

    /* ===== helpers ========================================================= */

    private Map<Class<? extends ISignal<?>>, String> resolveBindings(KafkaBridgeConfig config) {
        Map<Class<? extends ISignal<?>>, String> out = new HashMap<>();
        for (KafkaBridgeConfig.WriteBindingConfig w : config.writes()) {
            Class<? extends ISignal<?>> cls = signalClassForTag(w.signalTag());
            if (cls != null) out.put(cls, w.bindingId());
        }
        return out;
    }

    private static Class<? extends ISignal<?>> signalClassForTag(String tag) {
        if (tag == null) return null;
        return switch (tag) {
            case DEFAULT_INCIDENT_TAG -> IncidentReportSignal.class;
            case DEFAULT_QUARANTINE_TAG -> QuarantineRequestSignal.class;
            case DEFAULT_HYPOTHESIS_TAG -> ThreatHypothesisSignal.class;
            case DEFAULT_TRANSPARENCY_TAG -> TransparencyLogSignal.class;
            default -> null;
        };
    }

    private static String signalTagFor(IResultSignal<?> s) {
        if (s instanceof IncidentReportSignal)    return DEFAULT_INCIDENT_TAG;
        if (s instanceof QuarantineRequestSignal) return DEFAULT_QUARANTINE_TAG;
        if (s instanceof ThreatHypothesisSignal)  return DEFAULT_HYPOTHESIS_TAG;
        if (s instanceof TransparencyLogSignal)   return DEFAULT_TRANSPARENCY_TAG;
        return s.getClass().getSimpleName();
    }

    private static String keyOf(IResultSignal<?> s) {
        if (s instanceof IncidentReportSignal i)    return i.getIncidentId();
        if (s instanceof QuarantineRequestSignal q) return q.getEntityId();
        if (s instanceof ThreatHypothesisSignal t)  return t.getHypothesisId();
        if (s instanceof TransparencyLogSignal tl)  return tl.getActionPlanId();
        return null;
    }

    private static List<String> evidenceOf(IResult r) {
        Long n = r.getNeuronId();
        return n == null ? List.of() : List.of(String.valueOf(n));
    }

    /**
     * Build a stable, consumer-friendly JSON projection of one advisory
     * signal (08-KAFKA.md §4 egress topics). The native signal class carries
     * AbstractSignal bookkeeping (sourceLayer, epoch, …) that should not
     * leak into the SOC-facing topic schema.
     */
    private static Map<String, Object> toDto(IResultSignal<?> s) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("type", s.getClass().getSimpleName());
        if (s instanceof IncidentReportSignal i) {
            dto.put("incidentId", i.getIncidentId());
            dto.put("severity", i.getSeverity() == null ? null : i.getSeverity().name());
            dto.put("summary", i.getSummary());
            dto.put("linkedEvents", i.getLinkedEvents());
        } else if (s instanceof QuarantineRequestSignal q) {
            dto.put("entityId", q.getEntityId());
            dto.put("kind", q.getKind() == null ? null : q.getKind().name());
            dto.put("durationTicks", q.getDurationTicks());
            dto.put("reason", q.getReason());
        } else if (s instanceof ThreatHypothesisSignal t) {
            dto.put("hypothesisId", t.getHypothesisId());
            dto.put("category", t.getCategory() == null ? null : t.getCategory().name());
            dto.put("posterior", t.getPosterior());
            dto.put("evidenceIds", t.getEvidenceIds());
        } else if (s instanceof TransparencyLogSignal tl) {
            dto.put("actionPlanId", tl.getActionPlanId());
            dto.put("discriminatorReason", tl.getDiscriminatorReason());
            dto.put("verdict", tl.getVerdict() == null ? null : tl.getVerdict().name());
            dto.put("evidenceNeuronIds", tl.getEvidenceNeuronIds());
            dto.put("timestamp", tl.getTimestamp());
        }
        return dto;
    }
}
