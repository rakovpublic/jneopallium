/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.otel;

import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeAuditRecord;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/**
 * Zero-cost {@link OtelInstrumentation} (09-OPENTELEMETRY.md §5). This is the
 * default implementation when the bridge is not enabled, ensuring deployments
 * that don't want OTel pay no observability cost.
 *
 * <p>Every method is a static-noop — JIT can inline-elide entirely. The
 * bridge's §10 R1 budget (&lt; 5 % overhead vs noop) is measured against this
 * baseline.
 */
public final class NoopOtelInstrumentation implements OtelInstrumentation {

    public static final NoopOtelInstrumentation INSTANCE = new NoopOtelInstrumentation();

    private NoopOtelInstrumentation() {}

    @Override public Scope tickSpan(long run)            { return Scope.NOOP; }
    @Override public Scope layerSpan(String layerName)   { return Scope.NOOP; }
    @Override public void observe(ISignal<?> signal, String layerName) {}
    @Override public void harmVeto(String reason, String evidenceNeurons) {}
    @Override public void loopIntervention(String loopId, String reason) {}
    @Override public void aggregatorVerdict(String bridge, String tag, String verdict,
                                            String reason, Double proposed, Double effective) {}
    @Override public void audit(BridgeAuditRecord record) {}
    @Override public void recordEnergy(double value)     {}
    @Override public void recordLoopAmplitude(String loopId, double amplitude) {}
    @Override public void forceSample(String cause)      {}
    @Override public void close()                        {}
}
