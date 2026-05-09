/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.otel;

import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeAuditRecord;
import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeSafetyMode;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * §5 — the noop must accept every call without throwing and the factory must
 * return it when the bridge is disabled (§6 {@code exporter.type: NONE}).
 */
class NoopOtelInstrumentationTest {

    @Test
    void factoryReturnsNoopWhenExporterDisabled() {
        OtelBridgeConfig cfg = new OtelBridgeConfig(
                "x", "y", "z",
                new OtelBridgeConfig.ExporterConfig(
                        OtelBridgeConfig.ExporterConfig.ExporterType.NONE,
                        null, 1L, Map.of()),
                Map.of(),
                OtelBridgeConfig.MetricsConfig.defaults(),
                OtelBridgeConfig.TracesConfig.defaults(),
                OtelBridgeConfig.LogsConfig.defaults(),
                OtelBridgeConfig.RedactionConfig.defaults());
        assertSame(NoopOtelInstrumentation.INSTANCE, OtelBridgeFactory.create(cfg));
    }

    @Test
    void everyMethodIsSilent() {
        OtelInstrumentation otel = NoopOtelInstrumentation.INSTANCE;
        assertDoesNotThrow(() -> {
            try (var s = otel.tickSpan(1L)) {
                try (var l = otel.layerSpan("L0")) {
                    otel.observe(null, "L0");
                }
                otel.harmVeto("r", "n1");
                otel.loopIntervention("loop", "rate");
                otel.aggregatorVerdict("fmi", "tag", "APPLIED", "ok", 1.0, 1.0);
                otel.audit(new BridgeAuditRecord(0L, 0L, "fmi",
                        BridgeAuditRecord.Verdict.APPLIED, null, "tag",
                        null, null, null, BridgeSafetyMode.AUTONOMOUS, List.of()));
                otel.recordEnergy(0.1);
                otel.recordLoopAmplitude("loop", 0.2);
                otel.forceSample("test");
            }
            otel.close();
        });
    }
}
