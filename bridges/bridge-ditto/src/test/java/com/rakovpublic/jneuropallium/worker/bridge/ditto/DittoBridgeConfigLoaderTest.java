/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.ditto;

import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeSafetyMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Loader tests for {@link DittoBridgeConfigLoader} and validation rules in
 * {@link DittoBridgeConfig} (10-DITTO.md §5, §8 S9).
 */
class DittoBridgeConfigLoaderTest {

    @Test
    void loadsCanonicalSpecYaml() throws Exception {
        String yaml = """
                connection:
                  baseUrl: "https://ditto.factory.local"
                authentication:
                  type: "OAuth2BearerToken"
                  tokenEndpoint: "https://idp.local/token"
                  clientId: "jneopallium-bridge"
                  clientSecretEnv: "DITTO_CLIENT_SECRET"
                things:
                  - "factory.line-a:pump-1"
                  - "factory.line-a:reactor-1"
                reads:
                  - bindingId: "PUMP1-VIB"
                    thingId: "factory.line-a:pump-1"
                    feature: "vibration"
                    property: "rms_z"
                    signalTag: "PUMP01.VIB.Z"
                  - bindingId: "REACTOR1-TEMP"
                    thingId: "factory.line-a:reactor-1"
                    feature: "temperature"
                    property: "current"
                    signalTag: "REACTOR01.TEMP"
                writes:
                  - bindingId: "REACTOR1-ADVISED-SP"
                    thingId: "factory.line-a:reactor-1"
                    feature: "recommended_setpoint"
                    property: "value"
                    signalTag: "REACTOR01.SP.ADV"
                audit:
                  localAuditFile: "/var/log/jneopallium/ditto-audit.jsonl"
                perTagSafetyMode:
                  REACTOR1-ADVISED-SP: ADVISORY
                """;
        DittoBridgeConfig cfg = DittoBridgeConfigLoader.load(yaml);
        assertEquals("https://ditto.factory.local", cfg.connection().baseUrl());
        assertEquals(2, cfg.things().size());
        assertEquals(2, cfg.reads().size());
        assertEquals("PUMP01.VIB.Z", cfg.reads().get(0).signalTag());
        assertEquals(1, cfg.writes().size());
        assertEquals("recommended_setpoint", cfg.writes().get(0).feature());
        assertEquals(BridgeSafetyMode.ADVISORY,
                cfg.perTagSafetyMode().get("REACTOR1-ADVISED-SP"));
    }

    /** S9: a write to a non-advisory feature is rejected at load. */
    @Test
    void rejectsNonAdvisoryFeaturePrefix() {
        String yaml = """
                connection:
                  baseUrl: "https://ditto.factory.local"
                writes:
                  - bindingId: "BAD"
                    thingId: "factory.line-a:reactor-1"
                    feature: "setpoint"
                    property: "value"
                    signalTag: "X"
                audit:
                  localAuditFile: "/tmp/ditto-audit.jsonl"
                """;
        Exception e = assertThrows(Exception.class, () -> DittoBridgeConfigLoader.load(yaml));
        Throwable t = e;
        while (t != null && (t.getMessage() == null
                || !t.getMessage().contains("advisory feature"))) {
            t = t.getCause();
        }
        assertNotNull(t, "expected the failure to mention the advisory feature rule");
    }

    /** S9 (positive): {@code advisory_*} prefix is accepted. */
    @Test
    void acceptsAdvisoryPrefix() throws Exception {
        String yaml = """
                connection:
                  baseUrl: "https://ditto.factory.local"
                writes:
                  - bindingId: "OK"
                    thingId: "factory.line-a:reactor-1"
                    feature: "advisory_maintenance"
                    property: "window"
                    signalTag: "MAINT"
                audit:
                  localAuditFile: "/tmp/ditto-audit.jsonl"
                """;
        DittoBridgeConfig cfg = DittoBridgeConfigLoader.load(yaml);
        assertEquals("advisory_maintenance", cfg.writes().get(0).feature());
    }

    /** AUTONOMOUS rejected at config — the bridge ceiling is ADVISORY. */
    @Test
    void rejectsAutonomousMode() {
        String yaml = """
                connection:
                  baseUrl: "https://ditto.factory.local"
                audit:
                  localAuditFile: "/tmp/ditto-audit.jsonl"
                perTagSafetyMode:
                  X: AUTONOMOUS
                """;
        Exception e = assertThrows(Exception.class, () -> DittoBridgeConfigLoader.load(yaml));
        Throwable t = e;
        while (t != null && (t.getMessage() == null || !t.getMessage().contains("ADVISORY"))) {
            t = t.getCause();
        }
        assertNotNull(t, "expected the failure to mention the ADVISORY ceiling");
    }

    /** 00-FRAMEWORK §3: typos in config must fail loading. */
    @Test
    void rejectsUnknownProperty() {
        String yaml = """
                connection:
                  baseUrl: "https://ditto.factory.local"
                  thisFieldDoesNotExist: 42
                audit:
                  localAuditFile: "/tmp/ditto-audit.jsonl"
                """;
        assertThrows(Exception.class, () -> DittoBridgeConfigLoader.load(yaml));
    }
}
