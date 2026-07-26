/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.plc4x;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeSafetyMode;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/** Loader tests covering the YAML schema in 01-PLC4X.md §5. */
class Plc4xConfigLoaderTest {

    private static final String SAMPLE_YAML = """
            connections:
              - id: "S7-LINE-A"
                connectionString: "s7://10.10.0.1?remote-rack=0&remote-slot=1"
                requestTimeout:  "PT5S"
                keepAliveInterval: "PT10S"
              - id: "MODBUS-PUMPHOUSE"
                connectionString: "modbus-tcp://10.10.0.2:502?unit-identifier=1"
                requestTimeout:  "PT2S"

            reads:
              - bindingId: "TIC-101"
                connectionId: "S7-LINE-A"
                fieldAddress: "%DB1.DBD0:REAL"
                signalTag: "PLANT.TIC101.PV"
                pollIntervalMs: 250
              - bindingId: "PUMP-RUN"
                connectionId: "MODBUS-PUMPHOUSE"
                fieldAddress: "coil:0"
                signalTag: "PLANT.PUMP01.STATE"
                pollIntervalMs: 500

            writes:
              - bindingId: "TIC-101"
                connectionId: "S7-LINE-A"
                fieldAddress: "%DB1.DBD8:REAL"
                signalTag: "PLANT.TIC101.SP"
                failSafeValue: 0.0
                rampRateMaxPerSec: 2.0
                minClampValue: 0.0
                maxClampValue: 100.0

            events:
              - bindingId: "TROUBLE-ALARMS"
                connectionId: "S7-LINE-A"
                fieldAddress: "%DB100.DBW0:WORD"
                signalTag: "PLANT.LINE_A.TROUBLE"
                pollIntervalMs: 1000
                severityMap:
                  "0x0001": "LOW"
                  "0x0002": "HIGH"
                  "0x0010": "CRITICAL"

            audit:
              localAuditFile: "/var/log/jneopallium/plc4x-audit.jsonl"
              writeRejectedToAudit: true

            perTagSafetyMode:
              "TIC-101": "SHADOW"

            tickInterval: "PT0.25S"
            """;

    @Test
    void loadsCanonicalYamlFromSpec() throws IOException {
        Plc4xConfig cfg = Plc4xConfigLoader.load(SAMPLE_YAML);

        assertEquals(2, cfg.connections().size());
        assertEquals("S7-LINE-A", cfg.connections().get(0).id());
        assertEquals(Duration.ofSeconds(5), cfg.connections().get(0).requestTimeout());
        assertEquals(Duration.ofSeconds(10), cfg.connections().get(0).keepAliveInterval());

        assertEquals(2, cfg.reads().size());
        assertEquals("PLANT.TIC101.PV", cfg.reads().get(0).signalTag());
        assertEquals(250L, cfg.reads().get(0).pollIntervalMs());

        assertEquals(1, cfg.writes().size());
        Plc4xConfig.WriteBindingConfig w = cfg.writes().get(0);
        assertEquals(0.0, w.failSafeValue());
        assertEquals(2.0, w.rampRateMaxPerSec());
        assertEquals(100.0, w.maxClampValue());

        assertEquals(1, cfg.events().size());
        assertEquals(3, cfg.events().get(0).severityMap().size());
        assertEquals("CRITICAL", cfg.events().get(0).severityMap().get("0x0010"));

        assertEquals("/var/log/jneopallium/plc4x-audit.jsonl", cfg.audit().localAuditFile());
        assertTrue(cfg.audit().writeRejectedToAudit());

        assertEquals(BridgeSafetyMode.SHADOW, cfg.perTagSafetyMode().get("TIC-101"));
        assertEquals(Duration.ofMillis(250), cfg.tickInterval());
    }

    @Test
    void unknownPropertyFailsLoad() {
        String bad = """
                connections:
                  - id: "S7"
                    connectionString: "s7://1.2.3.4"
                    requestTimeout: "PT1S"
                    nonsenseField: 42
                reads: []
                writes: []
                events: []
                audit:
                  localAuditFile: "/tmp/x.jsonl"
                  writeRejectedToAudit: true
                perTagSafetyMode: {}
                tickInterval: "PT0.25S"
                """;
        assertThrows(UnrecognizedPropertyException.class, () -> Plc4xConfigLoader.load(bad));
    }

    @Test
    void rejectsZeroPollInterval() {
        String bad = """
                connections:
                  - id: "S7"
                    connectionString: "s7://1.2.3.4"
                    requestTimeout: "PT1S"
                reads:
                  - bindingId: "X"
                    connectionId: "S7"
                    fieldAddress: "%DB1.DBD0:REAL"
                    signalTag: "X.PV"
                    pollIntervalMs: 0
                writes: []
                events: []
                audit:
                  localAuditFile: "/tmp/x.jsonl"
                  writeRejectedToAudit: true
                perTagSafetyMode: {}
                tickInterval: "PT0.25S"
                """;
        // ValueInstantiationException wraps the IllegalArgumentException
        IOException ex = assertThrows(IOException.class, () -> Plc4xConfigLoader.load(bad));
        assertTrue(ex.getMessage().contains("pollIntervalMs"),
                "expected pollIntervalMs in error, got: " + ex.getMessage());
    }

    @Test
    void defaultsApplyWhenOptionalFieldsMissing() throws IOException {
        String minimal = """
                connections:
                  - id: "S7"
                    connectionString: "s7://1.2.3.4"
                """;
        Plc4xConfig cfg = Plc4xConfigLoader.load(minimal);
        assertEquals(1, cfg.connections().size());
        assertEquals(Duration.ofSeconds(5), cfg.connections().get(0).requestTimeout());
        assertEquals(Duration.ofMillis(250), cfg.tickInterval());
        assertTrue(cfg.reads().isEmpty());
        assertTrue(cfg.writes().isEmpty());
        assertTrue(cfg.events().isEmpty());
    }
}
