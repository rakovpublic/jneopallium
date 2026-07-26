/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AbstractBridgeAuditOutputTest {

    private static final class TestAudit extends AbstractBridgeAuditOutput {
        TestAudit(Path file) { super(file); }
    }

    @Test
    void writesJsonlConformingToSchema(@TempDir Path tmp) throws IOException {
        Path file = tmp.resolve("audit.jsonl");
        ObjectMapper m = new ObjectMapper();
        try (TestAudit a = new TestAudit(file)) {
            a.append(new BridgeAuditRecord(
                    1740000000000L, 12345, "opcua",
                    BridgeAuditRecord.Verdict.APPLIED,
                    "FIC-101", "PLANT.FIC101.SP",
                    47.3, 45.0,
                    BridgeAuditRecord.ModifyReason.RATE_LIMITED,
                    BridgeSafetyMode.AUTONOMOUS,
                    List.of("Setpoint-12", "MPC-3")));
        }
        List<String> lines = Files.readAllLines(file);
        assertEquals(1, lines.size());
        JsonNode n = m.readTree(lines.get(0));
        assertEquals(1740000000000L, n.get("ts").asLong());
        assertEquals("opcua", n.get("bridge").asText());
        assertEquals("APPLIED", n.get("verdict").asText());
        assertEquals("FIC-101", n.get("loopId").asText());
        assertEquals("PLANT.FIC101.SP", n.get("tag").asText());
        assertEquals(47.3, n.get("proposed").asDouble(), 1e-9);
        assertEquals(45.0, n.get("effective").asDouble(), 1e-9);
        assertEquals("RATE_LIMITED", n.get("reason").asText());
        assertEquals("AUTONOMOUS", n.get("safetyMode").asText());
        assertTrue(n.get("evidenceNeurons").isArray());
        assertEquals("Setpoint-12", n.get("evidenceNeurons").get(0).asText());
    }

    @Test
    void omitsNullFields(@TempDir Path tmp) throws IOException {
        Path file = tmp.resolve("a.jsonl");
        try (TestAudit a = new TestAudit(file)) {
            a.append(new BridgeAuditRecord(
                    1L, 1, "k", BridgeAuditRecord.Verdict.APPLIED,
                    "L", "T", 1.0, 1.0, null, BridgeSafetyMode.AUTONOMOUS, null));
        }
        String line = Files.readAllLines(file).get(0);
        assertFalse(line.contains("\"reason\""), "null reason must be omitted: " + line);
    }

    @Test
    void unwritableTargetDegradesGracefully(@TempDir Path tmp) throws IOException {
        // Make the target a read-only directory so Files.newBufferedWriter fails.
        Path locked = tmp.resolve("locked-dir");
        Files.createDirectory(locked);
        // Path that is itself a directory cannot be opened for append.
        try (TestAudit a = new TestAudit(locked)) {
            assertTrue(a.isDegraded(), "should have entered degraded mode");
            // Append must not throw.
            a.append(new BridgeAuditRecord(1L, 1, "k",
                    BridgeAuditRecord.Verdict.APPLIED, "L", "T",
                    1.0, 1.0, null, BridgeSafetyMode.AUTONOMOUS, null));
        }
    }
}
