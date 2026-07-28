/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.dicom;

import com.fasterxml.jackson.databind.JsonMappingException;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 07-DICOM.md §6 — {@code writes:} block is rejected at config-load with a
 * clear message. Verifies the structural rejection mechanism.
 */
class DicomBridgeConfigLoaderTest {

    @Test
    void writesBlockIsRejectedAtLoad() {
        String yaml = """
                mode: DICOMWEB
                dicomweb:
                  baseUrl: "https://orthanc.test/dicom-web"
                  pollIntervalSeconds: 60
                audit:
                  localAuditFile: "/tmp/dicom.jsonl"
                writes:
                  - bindingId: ANY
                """;
        Exception ex = assertThrows(Exception.class, () -> DicomBridgeConfigLoader.load(yaml));
        Throwable cause = unwrap(ex);
        assertTrue(cause.getMessage().contains("writes:"),
                "expected message to mention the writes: block, was: " + cause.getMessage());
        assertTrue(cause.getMessage().contains("READ-ONLY"),
                "expected message to reference READ-ONLY ceiling, was: " + cause.getMessage());
    }

    @Test
    void minimalDicomwebConfigLoads() throws IOException {
        String yaml = """
                mode: DICOMWEB
                dicomweb:
                  baseUrl: "https://orthanc.test/dicom-web"
                  pollIntervalSeconds: 60
                audit:
                  localAuditFile: "/tmp/dicom.jsonl"
                reads:
                  - bindingId: "RECENT-RADIOLOGY-SR"
                    studyFilter:
                      modality: "SR"
                      accessionPattern: "RAD-*"
                      windowHours: 24
                    targetSignal: "IMAGING_FINDING"
                    signalTagPrefix: "EHR.RADIOLOGY"
                """;
        DicomBridgeConfig cfg = DicomBridgeConfigLoader.load(yaml);
        assertEquals(DicomBridgeConfig.Mode.DICOMWEB, cfg.mode());
        assertNotNull(cfg.dicomweb());
        assertEquals(1, cfg.reads().size());
        assertEquals("RECENT-RADIOLOGY-SR", cfg.reads().get(0).bindingId());
        assertEquals("SR", cfg.reads().get(0).studyFilter().modality());
    }

    @Test
    void pollIntervalBelowMinimumIsRejected() {
        String yaml = """
                mode: DICOMWEB
                dicomweb:
                  baseUrl: "https://orthanc.test/dicom-web"
                  pollIntervalSeconds: 5
                audit:
                  localAuditFile: "/tmp/dicom.jsonl"
                """;
        Exception ex = assertThrows(Exception.class, () -> DicomBridgeConfigLoader.load(yaml));
        Throwable cause = unwrap(ex);
        assertTrue(cause.getMessage().contains("pollIntervalSeconds"),
                "expected poll interval rejection, was: " + cause.getMessage());
    }

    @Test
    void dicomwebModeRequiresDicomwebBlock() {
        String yaml = """
                mode: DICOMWEB
                audit:
                  localAuditFile: "/tmp/dicom.jsonl"
                """;
        Exception ex = assertThrows(Exception.class, () -> DicomBridgeConfigLoader.load(yaml));
        Throwable cause = unwrap(ex);
        assertTrue(cause.getMessage().contains("dicomweb:"),
                "expected message to require dicomweb: block, was: " + cause.getMessage());
    }

    private static Throwable unwrap(Throwable t) {
        Throwable cur = t;
        while (cur instanceof JsonMappingException && cur.getCause() != null) {
            cur = cur.getCause();
        }
        return cur;
    }
}
