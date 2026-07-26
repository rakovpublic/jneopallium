/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.fhir;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeSafetyMode;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.DemographicSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.LabResultSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.MedicationAdminSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.VitalSignal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end test of the FHIR bridge via {@link InMemoryFhirTransport}
 * (06-FHIR.md §9 acceptance scenarios S7, S8, S9, S10, S11).
 */
class FhirBridgeIntegrationTest {

    private final ObjectMapper json = new ObjectMapper();

    private FhirBridgeConfig configFor(Path auditFile, Path advisoryFile) {
        return new FhirBridgeConfig(
                new FhirBridgeConfig.FhirEndpointConfig(
                        "https://hapi.fhir.org/baseR4",
                        FhirBridgeConfig.FhirVersion.R4,
                        60L),
                FhirBridgeConfig.SecurityConfig.of(FhirBridgeConfig.SecurityType.NONE,
                        null, null, null, null, null, null, null, null),
                new FhirBridgeConfig.CohortConfig(List.of("example-pid-1"), null),
                List.of(
                        new FhirBridgeConfig.ReadBindingConfig(
                                "PATIENT",
                                "Patient/{pid}",
                                FhirBridgeConfig.TargetSignal.DEMOGRAPHIC,
                                "EHR.PATIENT"),
                        new FhirBridgeConfig.ReadBindingConfig(
                                "VITAL-HR",
                                "Observation?category=vital-signs&code=8867-4&patient={pid}",
                                FhirBridgeConfig.TargetSignal.VITAL,
                                "EHR.HR"),
                        new FhirBridgeConfig.ReadBindingConfig(
                                "LAB-GLUCOSE",
                                "Observation?category=laboratory&code=2339-0&patient={pid}",
                                FhirBridgeConfig.TargetSignal.LAB_RESULT,
                                "EHR.GLUCOSE"),
                        new FhirBridgeConfig.ReadBindingConfig(
                                "MED-ADMIN-INSULIN",
                                "MedicationAdministration?medication.code=insulin&patient={pid}",
                                FhirBridgeConfig.TargetSignal.MED_ADMIN,
                                "EHR.INSULIN")),
                FhirBridgeConfig.PrivacyConfig.of(true, null, true),
                new FhirBridgeConfig.AuditConfig(
                        auditFile.toString(),
                        advisoryFile.toString()),
                Map.of("EHR.HR", BridgeSafetyMode.ADVISORY),
                null);
    }

    /** §9 S7 — HAPI public test fetch — emits a DemographicSignal for the cohort patient. */
    @Test
    void s7_hapiFetchEmitsDemographic(@TempDir Path tmp) throws Exception {
        Path auditFile = tmp.resolve("audit.jsonl");
        FhirBridgeConfig cfg = configFor(auditFile, tmp.resolve("advisory.jsonl"));
        InMemoryFhirTransport transport = new InMemoryFhirTransport();
        // Patient/{pid} read returns a Bundle entry with a single Patient.
        transport.putSearchEntries("Patient/example-pid-1",
                json.readTree("""
                        {
                          "resourceType": "Patient",
                          "id": "example-pid-1",
                          "gender": "male",
                          "birthDate": "1970-01-01"
                        }
                        """));
        try (FhirAuditOutput audit = new FhirAuditOutput(auditFile);
             FhirClientService svc = new FhirClientService(cfg, transport, audit)) {
            svc.start();
            svc.poll();
            FhirObservationInput input = new FhirObservationInput("fhir-demo",
                    svc, List.of("PATIENT"));
            List<IInputSignal> signals = input.readSignals();
            assertFalse(signals.isEmpty(), "expected at least one DemographicSignal");
            DemographicSignal demo = (DemographicSignal) signals.get(0);
            assertEquals(com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical.Sex.MALE,
                    demo.getSex());
            assertNotNull(demo.getPatientId());
            assertFalse("example-pid-1".equals(demo.getPatientId()),
                    "S9 — raw cohort id must never appear on emitted signals");
        }
    }

    /** §9 S8 — when the transport reports not-ready, the poll cycle is a no-op (graceful). */
    @Test
    void s8_transportNotReadyIsGraceful(@TempDir Path tmp) throws Exception {
        Path auditFile = tmp.resolve("audit.jsonl");
        FhirBridgeConfig cfg = configFor(auditFile, tmp.resolve("advisory.jsonl"));
        InMemoryFhirTransport transport = new InMemoryFhirTransport().setReady(false);
        try (FhirAuditOutput audit = new FhirAuditOutput(auditFile);
             FhirClientService svc = new FhirClientService(cfg, transport, audit)) {
            svc.start();
            svc.poll(); // must not throw, must drain nothing
            FhirObservationInput input = new FhirObservationInput("fhir",
                    svc, List.of("VITAL-HR"));
            assertTrue(input.readSignals().isEmpty(),
                    "no signals should be emitted while transport is unauthenticated");
        }
    }

    /** §9 S9 — pseudonymisation pipeline: raw cohort id never appears in audit JSONL. */
    @Test
    void s9_pseudonymisationKeepsRawIdOutOfAudit(@TempDir Path tmp) throws Exception {
        Path auditFile = tmp.resolve("audit.jsonl");
        Path advisoryFile = tmp.resolve("advisory.jsonl");
        FhirBridgeConfig cfg = configFor(auditFile, advisoryFile);
        InMemoryFhirTransport transport = new InMemoryFhirTransport();
        transport.putSearchEntries(
                "Observation?category=vital-signs&code=8867-4&patient=example-pid-1",
                json.readTree("""
                        {
                          "resourceType": "Observation",
                          "subject": { "reference": "Patient/example-pid-1" },
                          "code": { "coding": [{ "system": "http://loinc.org", "code": "8867-4" }] },
                          "effectiveDateTime": "2026-01-01T12:00:00Z",
                          "valueQuantity": { "value": 80 }
                        }
                        """));
        try (FhirAuditOutput audit = new FhirAuditOutput(auditFile);
             FhirClientService svc = new FhirClientService(cfg, transport, audit)) {
            svc.start();
            svc.poll();
        }
        if (Files.exists(auditFile)) {
            String content = Files.readString(auditFile);
            assertFalse(content.contains("example-pid-1"),
                    "S9 — raw cohort id must not appear in audit JSONL, was: " + content);
        }
    }

    /** §11 S11 — free-text note redaction: counter increments, content does not surface. */
    @Test
    void s11_freeTextRedactionIsRecordedAsCountOnly(@TempDir Path tmp) throws Exception {
        Path auditFile = tmp.resolve("audit.jsonl");
        FhirBridgeConfig cfg = configFor(auditFile, tmp.resolve("advisory.jsonl"));
        InMemoryFhirTransport transport = new InMemoryFhirTransport();
        transport.putSearchEntries(
                "Observation?category=vital-signs&code=8867-4&patient=example-pid-1",
                json.readTree("""
                        {
                          "resourceType": "Observation",
                          "subject": { "reference": "Patient/example-pid-1" },
                          "code": { "coding": [{ "system": "http://loinc.org", "code": "8867-4" }] },
                          "effectiveDateTime": "2026-01-01T12:00:00Z",
                          "valueQuantity": { "value": 80 },
                          "note": [{ "text": "patient mentioned suicidal ideation" }]
                        }
                        """));
        try (FhirAuditOutput audit = new FhirAuditOutput(auditFile);
             FhirClientService svc = new FhirClientService(cfg, transport, audit)) {
            svc.start();
            svc.poll();
            FhirObservationInput input = new FhirObservationInput("fhir",
                    svc, List.of("VITAL-HR"));
            List<IInputSignal> signals = input.readSignals();
            assertEquals(1, signals.size());
            VitalSignal v = (VitalSignal) signals.get(0);
            // Vital signal has no note field — the note never propagates.
            assertEquals(80.0, v.getMeasurement(), 1e-9);
        }
        String content = Files.exists(auditFile) ? Files.readString(auditFile) : "";
        assertFalse(content.contains("suicidal"),
                "S11 — sensitive note text must not appear in audit JSONL");
        assertTrue(content.contains("FREE_TEXT_REDACTED"),
                "S11 — audit must record a count of redactions (got: " + content + ")");
    }

    /** Multiple bindings (lab + medication) are routed into the correct decoded queues. */
    @Test
    void multipleBindingsRouteToCorrectQueues(@TempDir Path tmp) throws Exception {
        Path auditFile = tmp.resolve("audit.jsonl");
        FhirBridgeConfig cfg = configFor(auditFile, tmp.resolve("advisory.jsonl"));
        InMemoryFhirTransport transport = new InMemoryFhirTransport();
        transport.putSearchEntries(
                "Observation?category=laboratory&code=2339-0&patient=example-pid-1",
                json.readTree("""
                        {
                          "resourceType": "Observation",
                          "subject": { "reference": "Patient/example-pid-1" },
                          "code": { "coding": [{ "system": "http://loinc.org", "code": "2339-0" }] },
                          "effectiveDateTime": "2026-01-01T12:00:00Z",
                          "valueQuantity": { "value": 140 }
                        }
                        """));
        transport.putSearchEntries(
                "MedicationAdministration?medication.code=insulin&patient=example-pid-1",
                json.readTree("""
                        {
                          "resourceType": "MedicationAdministration",
                          "subject": { "reference": "Patient/example-pid-1" },
                          "medicationCodeableConcept": {
                            "coding": [{ "system": "http://www.nlm.nih.gov/research/umls/rxnorm",
                                         "code": "5856" }]
                          },
                          "effectiveDateTime": "2026-01-01T12:30:00Z",
                          "dosage": { "dose": { "value": 5, "unit": "IU" } }
                        }
                        """));
        try (FhirAuditOutput audit = new FhirAuditOutput(auditFile);
             FhirClientService svc = new FhirClientService(cfg, transport, audit)) {
            svc.start();
            svc.poll();
            FhirObservationInput labs = new FhirObservationInput("labs",
                    svc, List.of("LAB-GLUCOSE"));
            FhirMedicationInput meds = new FhirMedicationInput("meds",
                    svc, List.of("MED-ADMIN-INSULIN"));
            List<IInputSignal> labSigs = labs.readSignals();
            List<IInputSignal> medSigs = meds.readSignals();
            assertEquals(1, labSigs.size());
            assertEquals(1, medSigs.size());
            assertTrue(labSigs.get(0) instanceof LabResultSignal);
            assertTrue(medSigs.get(0) instanceof MedicationAdminSignal);
        }
    }
}
