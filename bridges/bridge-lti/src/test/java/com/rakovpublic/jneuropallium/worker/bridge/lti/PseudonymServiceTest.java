/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.lti;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PseudonymServiceTest {

    @Test
    void pseudonymiseProducesStableFixedLengthHex() {
        PseudonymService svc = new PseudonymService(true, "salt");
        String a = svc.pseudonymise("alice@example.edu");
        String b = svc.pseudonymise("alice@example.edu");
        assertEquals(a, b, "deterministic for same input");
        assertEquals(PseudonymService.ID_LEN, a.length());
        assertFalse(a.contains("alice"), "raw identifier must not leak");
        assertNotNull(a);
    }

    @Test
    void differentSaltsProduceDifferentPseudonyms() {
        String s1 = new PseudonymService(true, "salt-a").pseudonymise("alice");
        String s2 = new PseudonymService(true, "salt-b").pseudonymise("alice");
        assertNotEquals(s1, s2);
    }

    @Test
    void disabledModePassesThrough() {
        PseudonymService svc = new PseudonymService(false, null);
        assertEquals("alice", svc.pseudonymise("alice"));
    }

    @Test
    void nullAndEmptyMapToNull() {
        PseudonymService svc = new PseudonymService(true, "salt");
        assertNull(svc.pseudonymise(null));
        assertNull(svc.pseudonymise(""));
    }

    @Test
    void fromConfigReadsSaltEnv() {
        LtiBridgeConfig.PrivacyConfig p =
                LtiBridgeConfig.PrivacyConfig.of(true, "NON_EXISTENT_SALT_VAR", true);
        PseudonymService svc = PseudonymService.fromConfig(p);
        assertTrue(svc.isEnabled());
        // No env var bound — service still works, salt is empty string.
        String pseudo = svc.pseudonymise("alice@example.edu");
        assertEquals(PseudonymService.ID_LEN, pseudo.length());
    }
}
