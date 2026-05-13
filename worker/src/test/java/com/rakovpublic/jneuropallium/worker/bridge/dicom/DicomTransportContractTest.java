/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.dicom;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Structural test for 07-DICOM.md §3, §4 diagram, §9 S10. Asserts that the
 * {@link DicomwebTransport} and {@link DimseClient} interfaces — the only
 * surfaces the bridge can use to reach a PACS — have no method whose name
 * suggests a non-read operation.
 *
 * <p>Also verifies that the bridge package contains no aggregator class:
 * §9 S10 — "Unit test attempts to construct a {@code DicomCommandOutputAggregator}
 * (class doesn't exist) → Compile error".
 */
class DicomTransportContractTest {

    private static final List<String> FORBIDDEN_NAMES = List.of(
            "create", "update", "delete", "patch", "put", "post", "save", "submit",
            "write", "send", "store", "stow", "cstore", "ccancel"
    );

    @Test
    void dicomwebTransportSurfaceIsReadOnly() {
        for (Method m : DicomwebTransport.class.getDeclaredMethods()) {
            String name = m.getName().toLowerCase(Locale.ROOT);
            for (String forbidden : FORBIDDEN_NAMES) {
                assertFalse(name.equals(forbidden) || name.startsWith(forbidden),
                        "DicomwebTransport has method '" + m.getName()
                                + "' that suggests a non-read operation. "
                                + "07-DICOM.md §3, §4 diagram forbid any write surface.");
            }
        }
    }

    @Test
    void dimseClientSurfaceIsReadOnly() {
        for (Method m : DimseClient.class.getDeclaredMethods()) {
            String name = m.getName().toLowerCase(Locale.ROOT);
            for (String forbidden : FORBIDDEN_NAMES) {
                assertFalse(name.equals(forbidden) || name.startsWith(forbidden),
                        "DimseClient has method '" + m.getName()
                                + "' that suggests a non-read DIMSE operation. "
                                + "07-DICOM.md §3, §4 diagram forbid C-STORE / STOW-RS.");
            }
        }
    }

    @Test
    void dicomwebReadAndSearchMethodsExist() {
        boolean qido = Arrays.stream(DicomwebTransport.class.getDeclaredMethods())
                .anyMatch(m -> m.getName().equals("qido"));
        boolean wado = Arrays.stream(DicomwebTransport.class.getDeclaredMethods())
                .anyMatch(m -> m.getName().equals("wadoMetadata"));
        assertTrue(qido, "DicomwebTransport.qido() is required");
        assertTrue(wado, "DicomwebTransport.wadoMetadata() is required");
    }

    /** §9 S10 — no aggregator class exists in the bridge package. */
    @Test
    void noOutputAggregatorClassExistsInDicomBridge() {
        String pkg = DicomwebTransport.class.getPackageName();
        for (String forbidden : List.of(
                pkg + ".DicomCommandOutputAggregator",
                pkg + ".DicomOutputAggregator",
                pkg + ".DicomAdvisoryOutputAggregator")) {
            assertFalse(classExists(forbidden),
                    "Class " + forbidden + " must not exist — DICOM bridge ceiling is READ-ONLY "
                            + "(07-DICOM.md §3, §7).");
        }
    }

    /** §10 R1 — WADO metadata Accept header is structurally pinned. */
    @Test
    void wadoAcceptHeaderIsDicomJson() {
        assertTrue("application/dicom+json".equals(JdkHttpDicomwebTransport.ACCEPT_HEADER),
                "JdkHttpDicomwebTransport.ACCEPT_HEADER must be application/dicom+json "
                        + "to keep the pixel-data path unreachable (07-DICOM.md §10 R1).");
    }

    private static boolean classExists(String name) {
        try {
            Class.forName(name);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
