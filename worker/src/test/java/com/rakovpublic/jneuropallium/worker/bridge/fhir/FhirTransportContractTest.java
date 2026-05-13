/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.fhir;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Structural test for 06-FHIR.md §3 rule 1, §9 S10 (write attempt
 * blocked). This is the unit test the spec calls out: it asserts that
 * the {@link FhirTransport} interface — the only surface the bridge can
 * use to reach a FHIR server — has no method whose name suggests a
 * non-read operation.
 *
 * <p>If a future contributor adds a {@code create()} / {@code update()} /
 * {@code delete()} / {@code patch()} method to {@link FhirTransport},
 * this test fails and forces a deliberate review of the bridge's safety
 * ceiling (06-FHIR.md §0 — permanently advisory).
 */
class FhirTransportContractTest {

    private static final List<String> FORBIDDEN_NAMES = List.of(
            "create", "update", "delete", "patch", "put", "post", "save", "submit",
            "write", "send"
    );

    @Test
    void fhirTransportSurfaceIsReadOnly() {
        Method[] declared = FhirTransport.class.getDeclaredMethods();
        for (Method m : declared) {
            String name = m.getName().toLowerCase(Locale.ROOT);
            for (String forbidden : FORBIDDEN_NAMES) {
                assertFalse(name.equals(forbidden) || name.startsWith(forbidden),
                        "FhirTransport has method '" + m.getName()
                                + "' that suggests a non-read operation. "
                                + "06-FHIR.md §3 rule 1 forbids any write surface in this bridge. "
                                + "If you genuinely need a write, build a separate bridge with its own "
                                + "certification path (06-FHIR.md §11).");
            }
        }
    }

    /** Make sure the read surface is still intact. */
    @Test
    void readAndSearchMethodsExist() {
        boolean read = Arrays.stream(FhirTransport.class.getDeclaredMethods())
                .anyMatch(m -> m.getName().equals("read"));
        boolean search = Arrays.stream(FhirTransport.class.getDeclaredMethods())
                .anyMatch(m -> m.getName().equals("search"));
        assertFalse(!read, "FhirTransport.read() is required");
        assertFalse(!search, "FhirTransport.search() is required");
    }
}
