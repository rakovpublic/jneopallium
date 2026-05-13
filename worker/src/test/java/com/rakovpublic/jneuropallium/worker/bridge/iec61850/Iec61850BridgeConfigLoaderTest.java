/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.iec61850;

import com.fasterxml.jackson.databind.JsonMappingException;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 11-IEC61850.md §6 — {@code writes:} block is rejected at config-load
 * with a clear message; verifies the structural rejection mechanism
 * plus the basic load-success path.
 */
class Iec61850BridgeConfigLoaderTest {

    @Test
    void writesBlockIsRejectedAtLoad() {
        String yaml = """
                ied:
                  - id: "RELAY-A1"
                    host: "10.50.1.10"
                    port: 102
                    sclFile: "/etc/jneopallium/scl/SUB1.icd"
                audit:
                  localAuditFile: "/tmp/iec61850.jsonl"
                writes:
                  - bindingId: ANY
                """;
        Exception ex = assertThrows(Exception.class, () -> Iec61850BridgeConfigLoader.load(yaml));
        Throwable cause = unwrap(ex);
        assertTrue(cause.getMessage().contains("writes:"),
                "expected message to mention the writes: block, was: " + cause.getMessage());
        assertTrue(cause.getMessage().contains("READ-ONLY"),
                "expected message to reference READ-ONLY ceiling, was: " + cause.getMessage());
        assertTrue(cause.getMessage().contains("iec61850-control"),
                "expected message to point to the separate control bridge, was: "
                        + cause.getMessage());
    }

    @Test
    void minimalConfigLoads() throws IOException {
        String yaml = """
                ied:
                  - id: "RELAY-A1"
                    host: "10.50.1.10"
                    port: 102
                    sclFile: "/etc/jneopallium/scl/SUB1.icd"
                    reportControlBlock: "LD0/LLN0.RP.urcbA01"
                reads:
                  - bindingId: "BUSBAR-V"
                    iedId: "RELAY-A1"
                    daPath: "LD0/MMXU1.PhV.phsA.cVal.mag.f"
                    signalTag: "SUB1.BUSBAR.VA"
                  - bindingId: "BREAKER-CB1-POS"
                    iedId: "RELAY-A1"
                    daPath: "LD0/XCBR1.Pos.stVal"
                    signalTag: "SUB1.CB1.POS"
                events:
                  - bindingId: "PROT-OPS"
                    iedId: "RELAY-A1"
                    reportControlBlock: "LD0/LLN0.RP.urcbProt01"
                    targetSignal: "ALARM"
                    severityMap:
                      PIOC: CRITICAL
                      PTOC: CRITICAL
                      PTUV: HIGH
                audit:
                  localAuditFile: "/tmp/iec61850.jsonl"
                """;
        Iec61850BridgeConfig cfg = Iec61850BridgeConfigLoader.load(yaml);
        assertEquals(1, cfg.ied().size());
        assertEquals("RELAY-A1", cfg.ied().get(0).id());
        assertEquals(2, cfg.reads().size());
        assertEquals(Iec61850BridgeConfig.TargetSignal.MEASUREMENT,
                cfg.reads().get(0).targetSignal());
        assertEquals(Iec61850BridgeConfig.TargetSignal.STATUS,
                cfg.reads().get(1).targetSignal(),
                "DA paths under XCBR/XSWI must auto-classify as STATUS");
        assertEquals(1, cfg.events().size());
        assertEquals("CRITICAL", cfg.events().get(0).severityMap().get("PIOC"));
        assertNotNull(cfg.audit());
    }

    @Test
    void unknownIedIdInReadBindingIsRejected() {
        String yaml = """
                ied:
                  - id: "RELAY-A1"
                    host: "10.50.1.10"
                    port: 102
                    sclFile: "/tmp/SUB1.icd"
                reads:
                  - bindingId: "ORPHAN"
                    iedId: "DOES-NOT-EXIST"
                    daPath: "LD0/MMXU1.PhV.phsA.cVal.mag.f"
                    signalTag: "x"
                audit:
                  localAuditFile: "/tmp/iec61850.jsonl"
                """;
        Exception ex = assertThrows(Exception.class, () -> Iec61850BridgeConfigLoader.load(yaml));
        Throwable cause = unwrap(ex);
        assertTrue(cause.getMessage().contains("DOES-NOT-EXIST"),
                "expected the unknown iedId in the message, was: " + cause.getMessage());
    }

    @Test
    void duplicateBindingIdIsRejected() {
        String yaml = """
                ied:
                  - id: "RELAY-A1"
                    host: "10.50.1.10"
                    port: 102
                    sclFile: "/tmp/SUB1.icd"
                reads:
                  - bindingId: "DUP"
                    iedId: "RELAY-A1"
                    daPath: "LD0/MMXU1.PhV.phsA.cVal.mag.f"
                    signalTag: "x"
                  - bindingId: "DUP"
                    iedId: "RELAY-A1"
                    daPath: "LD0/MMXU2.A.phsA.cVal.mag.f"
                    signalTag: "y"
                audit:
                  localAuditFile: "/tmp/iec61850.jsonl"
                """;
        Exception ex = assertThrows(Exception.class, () -> Iec61850BridgeConfigLoader.load(yaml));
        Throwable cause = unwrap(ex);
        assertTrue(cause.getMessage().contains("duplicate bindingId"),
                "expected duplicate bindingId rejection, was: " + cause.getMessage());
    }

    @Test
    void unknownKeyIsRejected() {
        String yaml = """
                ied:
                  - id: "RELAY-A1"
                    host: "10.50.1.10"
                    port: 102
                    sclFile: "/tmp/SUB1.icd"
                audit:
                  localAuditFile: "/tmp/iec61850.jsonl"
                bogusKey: "should-fail"
                """;
        assertThrows(Exception.class, () -> Iec61850BridgeConfigLoader.load(yaml),
                "FAIL_ON_UNKNOWN_PROPERTIES must reject typos at load time");
    }

    private static Throwable unwrap(Throwable t) {
        Throwable cur = t;
        while (cur instanceof JsonMappingException && cur.getCause() != null) {
            cur = cur.getCause();
        }
        return cur;
    }
}
