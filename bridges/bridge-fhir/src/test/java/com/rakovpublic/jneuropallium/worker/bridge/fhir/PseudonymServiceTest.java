/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.fhir;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link PseudonymService} tests (06-FHIR.md §3 rule 3, §9 S9, §10 R1).
 */
class PseudonymServiceTest {

    @Test
    void enabledPseudonymsAreDeterministicAndDistinct() {
        PseudonymService p = new PseudonymService(true, "salt-A");
        String a = p.pseudonymise("patient-1");
        String b = p.pseudonymise("patient-2");
        String aAgain = p.pseudonymise("patient-1");
        assertNotNull(a);
        assertNotNull(b);
        assertEquals(PseudonymService.ID_LEN, a.length());
        assertEquals(a, aAgain, "same input → same pseudonym");
        assertNotEquals(a, b, "different inputs → different pseudonyms");
    }

    @Test
    void saltAffectsOutput() {
        PseudonymService p1 = new PseudonymService(true, "salt-A");
        PseudonymService p2 = new PseudonymService(true, "salt-B");
        assertNotEquals(p1.pseudonymise("patient-1"), p2.pseudonymise("patient-1"),
                "different salts must change the pseudonym (§10 R1 — non-reversible)");
    }

    @Test
    void disabledServiceReturnsInputUnchanged() {
        PseudonymService p = new PseudonymService(false, "ignored");
        assertEquals("patient-1", p.pseudonymise("patient-1"));
    }

    @Test
    void nullAndEmptyAreMappedToNull() {
        PseudonymService p = new PseudonymService(true, "salt");
        assertNull(p.pseudonymise(null));
        assertNull(p.pseudonymise(""));
    }

    @Test
    void pseudonymIsLowercaseHex() {
        PseudonymService p = new PseudonymService(true, "salt");
        String pid = p.pseudonymise("patient-x");
        assertTrue(pid.matches("[0-9a-f]+"),
                "pseudonym must be lowercase hex, got: " + pid);
    }
}
