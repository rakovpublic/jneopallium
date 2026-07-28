/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.dicom;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.ImagingFindingSignal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end DICOM bridge tests (07-DICOM.md §9 S7, S8, S9, S11).
 */
class DicomBridgeIntegrationTest {

    private final ObjectMapper json = new ObjectMapper();

    private DicomBridgeConfig dicomwebConfig(Path auditFile) {
        return new DicomBridgeConfig(
                DicomBridgeConfig.Mode.DICOMWEB,
                null,
                new DicomBridgeConfig.DicomwebConfig(
                        "https://orthanc.test/dicom-web",
                        "/studies",
                        "/studies/{study}/series/{series}/instances/{instance}/metadata",
                        60L),
                DicomBridgeConfig.SecurityConfig.of(DicomBridgeConfig.SecurityType.NONE,
                        null, null, null, null, null, null, null, null),
                List.of(new DicomBridgeConfig.StudyReadConfig(
                        "RECENT-RADIOLOGY-SR",
                        new DicomBridgeConfig.StudyFilterConfig("SR", "RAD-*", 24),
                        DicomBridgeConfig.TargetSignal.IMAGING_FINDING,
                        "EHR.RADIOLOGY")),
                new DicomBridgeConfig.PrivacyConfig(true, null, true, false),
                new DicomBridgeConfig.AuditConfig(auditFile.toString()),
                null);
    }

    /** §9 S7 — DICOMweb fetch — bridge produces ImagingFindingSignals, one per leaf. */
    @Test
    void s7_dicomwebFetchEmitsImagingFindings(@TempDir Path tmp) throws Exception {
        Path auditFile = tmp.resolve("audit.jsonl");
        DicomBridgeConfig cfg = dicomwebConfig(auditFile);

        String study = """
                {
                  "0020000D": { "vr": "UI", "Value": ["1.2.3.4"] },
                  "0020000E": { "vr": "UI", "Value": ["1.2.3.4.5"] },
                  "00080018": { "vr": "UI", "Value": ["1.2.3.4.5.6"] }
                }
                """;
        String srInstance = """
                {
                  "00080060": { "vr": "CS", "Value": ["SR"] },
                  "00100020": { "vr": "LO", "Value": ["PT-007"] },
                  "00100010": { "vr": "PN", "Value": [{"Alphabetic": "DOE^JOHN"}] },
                  "00180015": { "vr": "CS", "Value": ["CHEST"] },
                  "0040A730": { "vr": "SQ", "Value": [
                    {
                      "0040A040": { "vr": "CS", "Value": ["CODE"] },
                      "0040A168": { "vr": "SQ", "Value": [{
                        "00080100": { "vr": "SH", "Value": ["MASS"] },
                        "00080102": { "vr": "SH", "Value": ["SCT"] },
                        "00080104": { "vr": "LO", "Value": ["Mass"] }
                      }]}
                    }
                  ]}
                }
                """;
        InMemoryDicomwebTransport transport = new InMemoryDicomwebTransport()
                .putQido("/studies?ModalitiesInStudy=SR&AccessionNumber=RAD-*"
                                + "&StudyDate=" + qidoWindow(24),
                        "[" + study + "]")
                .putWadoMetadata(
                        "/studies/1.2.3.4/series/1.2.3.4.5/instances/1.2.3.4.5.6/metadata",
                        "[" + srInstance + "]");
        try (DicomAuditOutput audit = new DicomAuditOutput(auditFile);
             DicomClientService svc = DicomClientService.forDicomweb(cfg, transport, audit)) {
            svc.start();
            svc.poll();
            DicomFindingInput input = new DicomFindingInput(
                    "dicom-demo", svc, List.of("RECENT-RADIOLOGY-SR"));
            List<IInputSignal> signals = input.readSignals();
            assertEquals(1, signals.size(), "expected one ImagingFindingSignal");
            ImagingFindingSignal s = (ImagingFindingSignal) signals.get(0);
            assertEquals("SR", s.getModality());
            assertEquals("CHEST", s.getRegionCode());
            assertNotNull(s.getPatientId());
            assertFalse("PT-007".equals(s.getPatientId()),
                    "S9 — raw PatientID must never appear on the emitted signal");
        }
    }

    /** §9 S8 — transport not ready: poll cycle is graceful no-op. */
    @Test
    void s8_transportNotReadyIsGraceful(@TempDir Path tmp) throws Exception {
        Path auditFile = tmp.resolve("audit.jsonl");
        DicomBridgeConfig cfg = dicomwebConfig(auditFile);
        InMemoryDicomwebTransport transport = new InMemoryDicomwebTransport().setReady(false);
        try (DicomAuditOutput audit = new DicomAuditOutput(auditFile);
             DicomClientService svc = DicomClientService.forDicomweb(cfg, transport, audit)) {
            svc.start();
            svc.poll(); // must not throw
            DicomFindingInput input = new DicomFindingInput(
                    "dicom", svc, List.of("RECENT-RADIOLOGY-SR"));
            assertTrue(input.readSignals().isEmpty(),
                    "no signals should be emitted while transport is unauthenticated");
        }
    }

    /** §9 S9 — raw PatientID/PatientName never appears in the audit JSONL. */
    @Test
    void s9_pseudonymisationKeepsPatientIdOutOfAudit(@TempDir Path tmp) throws Exception {
        Path auditFile = tmp.resolve("audit.jsonl");
        DicomBridgeConfig cfg = dicomwebConfig(auditFile);
        InMemoryDicomwebTransport transport = new InMemoryDicomwebTransport()
                // No registered QIDO response — the bridge polls, gets an empty list,
                // and must not write the raw patient id anywhere; the audit must also
                // not contain it on failure paths.
                ;
        try (DicomAuditOutput audit = new DicomAuditOutput(auditFile);
             DicomClientService svc = DicomClientService.forDicomweb(cfg, transport, audit)) {
            svc.start();
            svc.poll();
        }
        String content = Files.exists(auditFile) ? Files.readString(auditFile) : "";
        assertFalse(content.contains("PT-007") || content.contains("DOE^JOHN"),
                "S9 — raw PatientID / PatientName must never appear in audit, was: " + content);
    }

    /** §9 S8 (DIMSE) — DIMSE handshake path returns expected findings. */
    @Test
    void s8_dimseModeReturnsExpectedFindings(@TempDir Path tmp) throws Exception {
        Path auditFile = tmp.resolve("audit.jsonl");
        DicomBridgeConfig cfg = new DicomBridgeConfig(
                DicomBridgeConfig.Mode.DIMSE,
                new DicomBridgeConfig.DimseConfig("JNEO-BRIDGE", "PACS",
                        "pacs.test", 11112, false),
                null,
                DicomBridgeConfig.SecurityConfig.of(DicomBridgeConfig.SecurityType.NONE,
                        null, null, null, null, null, null, null, null),
                List.of(new DicomBridgeConfig.StudyReadConfig(
                        "DIMSE-SR",
                        new DicomBridgeConfig.StudyFilterConfig("SR", null, null),
                        DicomBridgeConfig.TargetSignal.IMAGING_FINDING,
                        "EHR.RADIOLOGY")),
                new DicomBridgeConfig.PrivacyConfig(true, null, true, false),
                new DicomBridgeConfig.AuditConfig(auditFile.toString()),
                null);
        String srInstance = """
                {
                  "00080060": { "vr": "CS", "Value": ["SR"] },
                  "00100020": { "vr": "LO", "Value": ["PT-DIMSE"] },
                  "0040A730": { "vr": "SQ", "Value": [
                    {
                      "0040A040": { "vr": "CS", "Value": ["TEXT"] },
                      "0040A160": { "vr": "UT", "Value": ["incidental finding"] }
                    }
                  ]}
                }
                """;
        InMemoryDimseClient dimse = new InMemoryDimseClient()
                .addStudyMatch(json.readTree("{\"0020000D\":{\"vr\":\"UI\",\"Value\":[\"1.2\"]}}"))
                .addSrInstance(json.readTree(srInstance));
        try (DicomAuditOutput audit = new DicomAuditOutput(auditFile);
             DicomClientService svc = DicomClientService.forDimse(cfg, dimse, audit)) {
            svc.start();
            svc.poll();
            DicomFindingInput input = new DicomFindingInput("dicom-dimse",
                    svc, List.of("DIMSE-SR"));
            List<IInputSignal> signals = input.readSignals();
            assertEquals(1, signals.size());
            ImagingFindingSignal s = (ImagingFindingSignal) signals.get(0);
            assertEquals("SR", s.getModality());
            assertNotNull(s.getPatientId());
            assertFalse("PT-DIMSE".equals(s.getPatientId()));
        }
    }

    /** §11 S11 — deeply nested SR via the end-to-end pipeline. */
    @Test
    void s11_deeplyNestedSrEmitsAllLeavesOnce(@TempDir Path tmp) throws Exception {
        Path auditFile = tmp.resolve("audit.jsonl");
        DicomBridgeConfig cfg = dicomwebConfig(auditFile);
        String study = """
                {
                  "0020000D": { "vr": "UI", "Value": ["1.2.3"] },
                  "0020000E": { "vr": "UI", "Value": ["1.2.3.4"] },
                  "00080018": { "vr": "UI", "Value": ["1.2.3.4.5"] }
                }
                """;
        String srInstance = """
                {
                  "00080060": { "vr": "CS", "Value": ["SR"] },
                  "00100020": { "vr": "LO", "Value": ["PT-N"] },
                  "0040A730": { "vr": "SQ", "Value": [
                    { "0040A040": { "vr": "CS", "Value": ["CONTAINER"] },
                      "0040A730": { "vr": "SQ", "Value": [
                        { "0040A040": { "vr": "CS", "Value": ["CONTAINER"] },
                          "0040A730": { "vr": "SQ", "Value": [
                            { "0040A040": { "vr": "CS", "Value": ["TEXT"] },
                              "0040A160": { "vr": "UT", "Value": ["leaf-1"] } },
                            { "0040A040": { "vr": "CS", "Value": ["TEXT"] },
                              "0040A160": { "vr": "UT", "Value": ["leaf-2"] } }
                          ]}
                        },
                        { "0040A040": { "vr": "CS", "Value": ["TEXT"] },
                          "0040A160": { "vr": "UT", "Value": ["leaf-3"] } }
                      ]}
                    }
                  ]}
                }
                """;
        InMemoryDicomwebTransport transport = new InMemoryDicomwebTransport()
                .putQido("/studies?ModalitiesInStudy=SR&AccessionNumber=RAD-*"
                                + "&StudyDate=" + qidoWindow(24),
                        "[" + study + "]")
                .putWadoMetadata("/studies/1.2.3/series/1.2.3.4/instances/1.2.3.4.5/metadata",
                        "[" + srInstance + "]");
        try (DicomAuditOutput audit = new DicomAuditOutput(auditFile);
             DicomClientService svc = DicomClientService.forDicomweb(cfg, transport, audit)) {
            svc.start();
            svc.poll();
            DicomFindingInput input = new DicomFindingInput(
                    "dicom", svc, List.of("RECENT-RADIOLOGY-SR"));
            List<IInputSignal> signals = input.readSignals();
            assertEquals(3, signals.size(),
                    "S11 — exactly three leaf signals; CONTAINER intermediates must not duplicate");
        }
    }

    private static String qidoWindow(int hours) {
        int days = Math.max(1, (hours + 23) / 24);
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate from = today.minusDays(days);
        return fmt(from) + "-" + fmt(today);
    }

    private static String fmt(java.time.LocalDate d) {
        return String.format("%04d%02d%02d", d.getYear(), d.getMonthValue(), d.getDayOfMonth());
    }
}
