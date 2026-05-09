/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.otel;

import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeAuditRecord;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/**
 * Instrumentation surface (09-OPENTELEMETRY.md §5) — the only entry-point the
 * rest of the worker calls into the OTel bridge.
 *
 * <p><strong>Export-only.</strong> No method on this interface accepts data
 * <em>back</em> from OTel into Jneopallium signals (§3). Audit-trail integrity
 * depends on this being asymmetric.
 *
 * <p>Two implementations exist:
 * <ul>
 *   <li>{@link NoopOtelInstrumentation} — default; zero observability cost,
 *       used in deployments that don't enable OTel.</li>
 *   <li>{@link SdkOtelInstrumentation} — backed by the OpenTelemetry Java SDK;
 *       wired only when the bridge is enabled.</li>
 * </ul>
 *
 * <p>All methods are thread-safe and non-blocking on the critical path. They
 * MUST NOT throw — exporter failures are absorbed inside the implementation.
 */
public interface OtelInstrumentation {

    /**
     * Wrap one tick. The returned {@link Scope} closes the underlying span
     * (and detaches it from the current context) when {@code close()} is
     * invoked, so callers should use try-with-resources:
     * <pre>{@code
     * try (OtelInstrumentation.Scope ignored = otel.tickSpan(runId)) {
     *     // execute the tick
     * }
     * }</pre>
     *
     * @param run monotonic run / tick identifier (becomes a span attribute)
     */
    Scope tickSpan(long run);

    /**
     * Wrap one layer's processing as a child span of the current tick span.
     * Returns a no-op scope if no tick span is active on the current thread.
     */
    Scope layerSpan(String layerName);

    /**
     * Record one signal observation under the current scope (§5). The
     * implementation may add it as an event/attribute on the active span and
     * increment a per-class counter.
     */
    void observe(ISignal<?> signal, String layerName);

    /**
     * Record one harm veto (§5, §9 S8). Increments the
     * {@code jneo.harm_veto.count} counter and forces the current tick span
     * to be sampled (§9 S9 — always-sample-on-veto).
     */
    void harmVeto(String reason, String evidenceNeurons);

    /**
     * Record one loop intervention (§8 phase 2). Increments
     * {@code jneo.loop_intervention.count}.
     */
    void loopIntervention(String loopId, String reason);

    /**
     * Record an aggregator verdict (§5). The verdict is added as attributes on
     * the active span and the bridge counter
     * {@code jneo.aggregator.verdict.count} is incremented with
     * {@code bridge=…, verdict=…} attributes.
     */
    void aggregatorVerdict(String bridge,
                           String tag,
                           String verdict,
                           String reason,
                           Double proposed,
                           Double effective);

    /**
     * Record one {@link BridgeAuditRecord} as a structured log record (§5,
     * §8 phase 1) and tick the {@code jneo.audit.applied.count} (or
     * {@code .rejected.count}) counter.
     */
    void audit(BridgeAuditRecord record);

    /**
     * Record an energy gauge observation (§1 mapping table —
     * {@code EnergySignal} → gauge metric). Implementations route this to the
     * {@code jneo.energy} async gauge.
     */
    void recordEnergy(double value);

    /**
     * Record a loop amplitude gauge observation (§1 mapping —
     * {@code LoopAlertSignal} → gauge). Routed to {@code jneo.loop.amplitude}.
     */
    void recordLoopAmplitude(String loopId, double amplitude);

    /**
     * Force the active tick span to be sampled regardless of the configured
     * sampler ratio (§8 phase 3 — always-sample-on-incident hook). Safe to
     * call when no tick span is active (becomes a no-op).
     */
    void forceSample(String cause);

    /**
     * Release any underlying SDK resources. After {@code close()}, all
     * methods become no-ops. Idempotent.
     */
    void close();

    /**
     * AutoCloseable scope returned by {@link #tickSpan(long)} and
     * {@link #layerSpan(String)}. Closing detaches the span and ends it.
     */
    interface Scope extends AutoCloseable {
        @Override void close();

        /** No-op scope, exported so callers can be branch-free. */
        Scope NOOP = () -> {};
    }
}
