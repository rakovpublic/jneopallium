/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.integration.nengo;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests for {@link NengoInputFrame} (15-NENGO.md §4 / S10). */
final class NengoInputFrameTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static Map<String, Double> values() {
        Map<String, Double> v = new LinkedHashMap<>();
        v.put("dx_target", 0.42);
        v.put("battery", 0.95);
        return v;
    }

    @Test
    void validateHappyPath() {
        NengoInputFrame f = new NengoInputFrame(
                "1", "NENGO_INPUT", "f-000001", 1L,
                10_000L, 10_250L, "OK", values(), null);
        assertNull(f.validate());
    }

    @Test
    void rejectsMissingSafetyStatus() {
        NengoInputFrame f = new NengoInputFrame(
                "1", "NENGO_INPUT", "f-000001", 1L,
                10_000L, 10_250L, null, values(), null);
        String reason = f.validate();
        assertNotNull(reason);
        assertTrue(reason.contains("safety_status"), reason);
    }

    @Test
    void rejectsBadSchemaVersion() {
        NengoInputFrame f = new NengoInputFrame(
                "99", "NENGO_INPUT", "f-000001", 1L,
                10_000L, 10_250L, "OK", values(), null);
        assertTrue(f.validate().startsWith("SCHEMA_VERSION_MISMATCH"));
    }

    @Test
    void rejectsBadSource() {
        NengoInputFrame f = new NengoInputFrame(
                "1", "JNEOPALLIUM_OUTPUT", "f-000001", 1L,
                10_000L, 10_250L, "OK", values(), null);
        assertEquals("BAD_SOURCE:JNEOPALLIUM_OUTPUT", f.validate());
    }

    @Test
    void rejectsNonFiniteValue() {
        Map<String, Double> v = new LinkedHashMap<>();
        v.put("dx_target", Double.NaN);
        NengoInputFrame f = new NengoInputFrame(
                "1", "NENGO_INPUT", "f-000001", 1L,
                10_000L, 10_250L, "OK", v, null);
        assertTrue(f.validate().startsWith("NON_FINITE_VALUE:dx_target"));
    }

    @Test
    void jsonRoundTrip() throws Exception {
        NengoInputFrame original = new NengoInputFrame(
                "1", "NENGO_INPUT", "f-000042", 42L,
                1_762_886_400_000L, 1_762_886_400_250L, "OK", values(), null);
        String json = MAPPER.writeValueAsString(original);
        NengoInputFrame parsed = MAPPER.readValue(json, NengoInputFrame.class);
        assertEquals(original.frameId(), parsed.frameId());
        assertEquals(original.sequenceNo(), parsed.sequenceNo());
        assertEquals(original.values(), parsed.values());
        assertNull(parsed.transparencyLogId());
        // schema_version snake_case is preserved
        assertTrue(json.contains("\"schema_version\":\"1\""));
        assertTrue(json.contains("\"safety_status\":\"OK\""));
    }
}
