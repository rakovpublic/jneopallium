/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.fhir;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeSafetyMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link FhirBridgeConfigLoader} tests (06-FHIR.md §6, §10 R1, §11).
 */
class FhirBridgeConfigLoaderTest {

    @Test
    void loadsMinimalConfig() throws Exception {
        String yaml = """
                fhir:
                  baseUrl: "https://hapi.fhir.org/baseR4"
                  fhirVersion: R4
                  pollIntervalSeconds: 60
                security:
                  type: NONE
                cohort:
                  patientIds: ["example-pid-1"]
                reads:
                  - bindingId: VITAL-HR
                    fhirSearch: "Observation?category=vital-signs&code=8867-4&patient={pid}"
                    targetSignal: VITAL
                    signalTag: EHR.HR
                privacy:
                  pseudonymise: true
                  saltEnv: FHIR_PSEUDO_SALT
                  redactFreeText: true
                audit:
                  localAuditFile: /tmp/fhir-audit.jsonl
                  advisoryFile: /tmp/fhir-advisory.jsonl
                """;
        FhirBridgeConfig cfg = FhirBridgeConfigLoader.load(yaml);
        assertEquals("https://hapi.fhir.org/baseR4", cfg.fhir().baseUrl());
        assertEquals(FhirBridgeConfig.FhirVersion.R4, cfg.fhir().fhirVersion());
        assertEquals(1, cfg.reads().size());
        assertEquals(FhirBridgeConfig.TargetSignal.VITAL, cfg.reads().get(0).targetSignal());
        assertEquals(1, cfg.cohort().patientIds().size());
        assertTrue(cfg.privacy().pseudonymise());
        assertTrue(cfg.privacy().redactFreeText());
        assertNotNull(cfg.audit().advisoryFile());
    }

    /** Unknown YAML keys are rejected at load (00-FRAMEWORK §3). */
    @Test
    void unknownKeyIsRejected() {
        String yaml = """
                fhir:
                  baseUrl: "https://hapi.fhir.org/baseR4"
                  pollIntervalSeconds: 60
                audit:
                  localAuditFile: /tmp/x.jsonl
                bogusKey: 42
                """;
        assertThrows(UnrecognizedPropertyException.class, () -> FhirBridgeConfigLoader.load(yaml));
    }

    /** §6 — AUTONOMOUS is structurally rejected (bridge never writes to FHIR). */
    @Test
    void autonomousModeIsRejected() {
        String yaml = """
                fhir:
                  baseUrl: "https://hapi.fhir.org/baseR4"
                  pollIntervalSeconds: 60
                audit:
                  localAuditFile: /tmp/x.jsonl
                perTagSafetyMode:
                  ANY: AUTONOMOUS
                """;
        String msg = causeMessage(() -> FhirBridgeConfigLoader.load(yaml));
        assertTrue(msg.contains("AUTONOMOUS"),
                "Error must mention AUTONOMOUS, got: " + msg);
    }

    /** §10 R2 — poll interval must not run below the 15-second floor. */
    @Test
    void pollIntervalBelowFloorIsRejected() {
        String yaml = """
                fhir:
                  baseUrl: "https://example/api/FHIR/R4"
                  pollIntervalSeconds: 5
                audit:
                  localAuditFile: /tmp/x.jsonl
                """;
        String msg = causeMessage(() -> FhirBridgeConfigLoader.load(yaml));
        assertTrue(msg.contains("pollIntervalSeconds"),
                "Error must mention pollIntervalSeconds, got: " + msg);
    }

    /** Duplicate binding ids are rejected. */
    @Test
    void duplicateBindingIdIsRejected() {
        String yaml = """
                fhir:
                  baseUrl: "https://example/api/FHIR/R4"
                  pollIntervalSeconds: 60
                reads:
                  - bindingId: DUP
                    fhirSearch: "Observation?patient={pid}"
                    targetSignal: VITAL
                  - bindingId: DUP
                    fhirSearch: "Condition?patient={pid}"
                    targetSignal: DIAGNOSIS
                audit:
                  localAuditFile: /tmp/x.jsonl
                """;
        String msg = causeMessage(() -> FhirBridgeConfigLoader.load(yaml));
        assertTrue(msg.contains("duplicate"),
                "Error must mention duplicate, got: " + msg);
    }

    /**
     * Jackson wraps any {@link IllegalArgumentException} thrown from a
     * record's compact constructor in a {@code ValueInstantiationException}.
     * Unwrap to the root cause's message so callers can assert on the
     * structural rule that was violated.
     */
    private static String causeMessage(org.junit.jupiter.api.function.Executable e) {
        Throwable t = assertThrows(Throwable.class, e);
        while (t.getCause() != null && t.getCause() != t) t = t.getCause();
        return t.getMessage() == null ? "" : t.getMessage();
    }

    /** SHADOW and ADVISORY safety modes are accepted; AUTONOMOUS is not. */
    @Test
    void shadowAndAdvisoryAccepted() throws Exception {
        String yaml = """
                fhir:
                  baseUrl: "https://example/api/FHIR/R4"
                  pollIntervalSeconds: 60
                audit:
                  localAuditFile: /tmp/x.jsonl
                perTagSafetyMode:
                  ANY: ADVISORY
                  OTHER: SHADOW
                """;
        FhirBridgeConfig cfg = FhirBridgeConfigLoader.load(yaml);
        assertEquals(BridgeSafetyMode.ADVISORY, cfg.perTagSafetyMode().get("ANY"));
        assertEquals(BridgeSafetyMode.SHADOW, cfg.perTagSafetyMode().get("OTHER"));
    }
}
