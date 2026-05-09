/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
/**
 * OpenTelemetry bridge for Jneopallium (09-OPENTELEMETRY.md).
 *
 * <p>The OTel bridge is the <em>trust bridge</em>: every other bridge exposes
 * Jneopallium to the world; this one exposes Jneopallium to operators,
 * auditors, and security teams. Its safety ceiling is
 * <strong>EXPORT-ONLY</strong>. There is no read path back into Jneopallium
 * signals — by design, no {@code OtelInput} class exists, no
 * {@code OtelCommandOutputAggregator} class exists. Data flows out, never in
 * (§3, §11 S11).
 *
 * <h2>Public surface</h2>
 * <dl>
 *   <dt>{@link com.rakovpublic.jneuropallium.worker.bridge.otel.OtelBridgeConfig}</dt>
 *   <dd>Immutable YAML-loaded configuration record (§6).</dd>
 *
 *   <dt>{@link com.rakovpublic.jneuropallium.worker.bridge.otel.OtelInstrumentation}</dt>
 *   <dd>The interface the rest of the worker calls (§5). Three families of
 *       calls: tick / layer span lifecycle, signal observation
 *       (incl. harm vetoes, loop interventions), and bridge audit records.</dd>
 *
 *   <dt>{@link com.rakovpublic.jneuropallium.worker.bridge.otel.NoopOtelInstrumentation}</dt>
 *   <dd>Default, zero-cost implementation when the bridge is disabled.</dd>
 *
 *   <dt>{@link com.rakovpublic.jneuropallium.worker.bridge.otel.SdkOtelInstrumentation}</dt>
 *   <dd>Active OpenTelemetry-SDK-backed implementation. Wired only when
 *       {@code otel.exporter.type != NONE}.</dd>
 *
 *   <dt>{@link com.rakovpublic.jneuropallium.worker.bridge.otel.OtelBridgeFactory}</dt>
 *   <dd>Single entry-point: pick noop vs SDK based on config; build the SDK
 *       with the configured OTLP gRPC/HTTP exporters; apply the parent-based
 *       trace-id-ratio sampler.</dd>
 * </dl>
 *
 * <h2>Phase plan (§8)</h2>
 * <ol>
 *   <li><strong>Phase 1.</strong> Trace + log path. Per-tick span with layer
 *       child spans. {@link com.rakovpublic.jneuropallium.worker.bridge.common.BridgeAuditRecord}s
 *       are emitted as structured log records.</li>
 *   <li><strong>Phase 2.</strong> Metrics path: counters
 *       ({@code jneo.harm_veto.count}, {@code jneo.loop_intervention.count},
 *       {@code jneo.audit.applied.count} etc.) and gauges
 *       ({@code jneo.energy}, {@code jneo.loop.amplitude}).</li>
 *   <li><strong>Phase 3.</strong> Sampler refinement — every harm veto, every
 *       interlock trip, and every {@code REJECTED} aggregator verdict forces
 *       its tick span to be sampled regardless of the configured ratio
 *       ({@link com.rakovpublic.jneuropallium.worker.bridge.otel.OtelInstrumentation#forceSample(String)}).</li>
 * </ol>
 *
 * <h2>Why no aggregator</h2>
 * <p>All other bridges in {@code worker.bridge.<id>} ship an
 * {@code <Bridge>CommandOutputAggregator}. The OTel bridge does not. There is
 * no path from observability data into worker decisions — see §3 of the spec.
 * The S11 scenario asserts the absence of {@code OtelInput} as a structural
 * test of this guarantee.
 */
package com.rakovpublic.jneuropallium.worker.bridge.otel;
