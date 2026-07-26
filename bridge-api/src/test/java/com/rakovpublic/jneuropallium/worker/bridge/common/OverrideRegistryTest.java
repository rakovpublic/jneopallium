/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OverrideRegistryTest {

    @Test
    void putAndActiveBeforeExpiry() {
        OverrideRegistry r = new OverrideRegistry();
        long now = 1_000_000L;
        r.put("PLANT.FIC101.SP", "op-7", "test", 42.0, 5_000, now);
        assertTrue(r.active("PLANT.FIC101.SP", now + 1_000).isPresent());
        assertEquals(42.0, r.active("PLANT.FIC101.SP", now + 1_000).get().manualValue());
    }

    @Test
    void expiresAfterTtl() {
        OverrideRegistry r = new OverrideRegistry();
        long now = 1_000_000L;
        r.put("tag", "op", "r", 1.0, 1_000, now);
        assertTrue(r.active("tag", now + 999).isPresent());
        assertFalse(r.active("tag", now + 1_001).isPresent());
        assertEquals(0, r.size(), "expired entry was swept lazily");
    }

    @Test
    void clearRemoves() {
        OverrideRegistry r = new OverrideRegistry();
        r.put("a", "op", null, 1.0, 5_000);
        r.put("b", "op", null, 2.0, 5_000);
        r.clear("a");
        assertFalse(r.isActive("a"));
        assertTrue(r.isActive("b"));
        r.clearAll();
        assertEquals(0, r.size());
    }

    @Test
    void zeroOrNegativeTtlDeletes() {
        OverrideRegistry r = new OverrideRegistry();
        r.put("t", "op", null, 1.0, 5_000);
        assertTrue(r.isActive("t"));
        r.put("t", "op", null, 1.0, 0);
        assertFalse(r.isActive("t"));
    }
}
