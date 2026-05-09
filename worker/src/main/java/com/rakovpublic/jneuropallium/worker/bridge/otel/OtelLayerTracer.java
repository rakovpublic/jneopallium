/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.otel;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;

/**
 * Per-layer span helper (09-OPENTELEMETRY.md §5, §7).
 *
 * <p>The {@code jneo.tick} parent span is started by
 * {@link SdkOtelInstrumentation#tickSpan(long)}; this helper starts and
 * activates the child {@code jneo.layer.<name>} span underneath it.
 */
final class OtelLayerTracer {

    private final Tracer tracer;

    OtelLayerTracer(Tracer tracer) {
        this.tracer = tracer;
    }

    /**
     * Start a child layer span. If no parent is active on the current thread,
     * the span is still created (root) — the caller decides whether that is
     * meaningful by checking the active context first.
     */
    SpanScope start(String layerName) {
        Span span = tracer.spanBuilder("jneo.layer." + layerName)
                .setAttribute(OtelMeterRegistry.LAYER, layerName)
                .startSpan();
        var ctx = Context.current().with(span);
        var scope = ctx.makeCurrent();
        return new SpanScope(span, scope);
    }

    /**
     * Pair of (span, context-scope). Closing detaches the context first,
     * then ends the span — the same order
     * {@link io.opentelemetry.api.trace.Span#end()} requires when used with
     * try-with-resources.
     */
    static final class SpanScope implements AutoCloseable {
        final Span span;
        private final io.opentelemetry.context.Scope scope;
        private boolean closed;

        SpanScope(Span span, io.opentelemetry.context.Scope scope) {
            this.span = span;
            this.scope = scope;
        }

        @Override
        public void close() {
            if (closed) return;
            closed = true;
            try { scope.close(); } finally { span.end(); }
        }
    }
}
