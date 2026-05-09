/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.otel;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 09-OPENTELEMETRY.md §6 — verifies the loader honours the §6 schema and
 * fails on unknown fields per 00-FRAMEWORK §3.
 */
class OtelBridgeConfigLoaderTest {

    @Test
    void loadsExampleConfig() throws IOException {
        String yaml = """
                serviceName: "jneopallium-bridge"
                serviceVersion: "1.0-SNAPSHOT"
                serviceInstanceId: "host-1"

                exporter:
                  type: "OTLP_GRPC"
                  endpoint: "http://otel-collector:4317"
                  timeoutMs: 10000
                  headers:
                    authorization: "Bearer token"

                resourceAttributes:
                  deployment.environment: "production"
                  plant.id: "PLANT-01"

                metrics:
                  enabled: true
                  intervalMs: 10000

                traces:
                  enabled: true
                  samplerRatio: 0.1

                logs:
                  enabled: true

                redaction:
                  redactSignalTags: false
                  redactPatterns: []
                """;
        OtelBridgeConfig cfg = OtelBridgeConfigLoader.load(yaml);

        assertEquals("jneopallium-bridge", cfg.serviceName());
        assertEquals(OtelBridgeConfig.ExporterConfig.ExporterType.OTLP_GRPC, cfg.exporter().type());
        assertEquals("http://otel-collector:4317", cfg.exporter().endpoint());
        assertEquals("Bearer token", cfg.exporter().headers().get("authorization"));
        assertEquals("PLANT-01", cfg.resourceAttributes().get("plant.id"));
        assertEquals(0.1, cfg.traces().samplerRatio());
        assertTrue(cfg.metrics().enabled());
        assertTrue(cfg.logs().enabled());
        assertFalse(cfg.redaction().redactSignalTags());
        assertTrue(cfg.redaction().redactPatterns().isEmpty());
    }

    @Test
    void rejectsUnknownProperty() {
        String yaml = """
                serviceName: "x"
                exporter:
                  type: "NONE"
                  bogusField: "no"
                """;
        assertThrows(IOException.class, () -> OtelBridgeConfigLoader.load(yaml));
    }

    @Test
    void rejectsInvalidSamplerRatio() {
        String yaml = """
                serviceName: "x"
                exporter:
                  type: "NONE"
                traces:
                  enabled: true
                  samplerRatio: 1.5
                """;
        assertThrows(IOException.class, () -> OtelBridgeConfigLoader.load(yaml));
    }
}
