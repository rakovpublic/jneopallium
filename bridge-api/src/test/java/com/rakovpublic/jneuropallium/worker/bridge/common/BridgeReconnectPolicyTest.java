/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.common;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class BridgeReconnectPolicyTest {

    @Test
    void exponentialBackoffCappedAt30s() {
        BridgeReconnectPolicy p = new BridgeReconnectPolicy();
        assertEquals(Duration.ofSeconds(1),  p.nextDelay());
        assertEquals(Duration.ofSeconds(2),  p.nextDelay());
        assertEquals(Duration.ofSeconds(4),  p.nextDelay());
        assertEquals(Duration.ofSeconds(8),  p.nextDelay());
        assertEquals(Duration.ofSeconds(16), p.nextDelay());
        assertEquals(Duration.ofSeconds(30), p.nextDelay()); // would be 32, capped
        assertEquals(Duration.ofSeconds(30), p.nextDelay()); // stays capped
    }

    @Test
    void resetGoesBackToInitial() {
        BridgeReconnectPolicy p = new BridgeReconnectPolicy();
        p.nextDelay(); p.nextDelay(); p.nextDelay();
        assertEquals(3, p.attempt());
        p.reset();
        assertEquals(0, p.attempt());
        assertEquals(Duration.ofSeconds(1), p.nextDelay());
    }

    @Test
    void neverOverflows() {
        BridgeReconnectPolicy p = new BridgeReconnectPolicy();
        for (int i = 0; i < 100; i++) {
            Duration d = p.nextDelay();
            assertFalse(d.isNegative());
            assertTrue(d.compareTo(Duration.ofSeconds(30)) <= 0);
        }
    }

    @Test
    void rejectsBadArgs() {
        assertThrows(IllegalArgumentException.class,
                () -> new BridgeReconnectPolicy(Duration.ZERO, Duration.ofSeconds(1)));
        assertThrows(IllegalArgumentException.class,
                () -> new BridgeReconnectPolicy(Duration.ofSeconds(10), Duration.ofSeconds(1)));
    }
}
