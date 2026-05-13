/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.fhir;

import com.rakovpublic.jneuropallium.ai.enums.HarmVerdict;
import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeSafetyMode;
import com.rakovpublic.jneuropallium.worker.net.layers.IResult;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical.AlertSeverity;
import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.AdverseEventAlertSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.ClinicalVetoSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.TreatmentProposalSignal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link FhirAdvisoryOutputAggregator} tests (06-FHIR.md §3 rule 2,
 * §5 egress table, §9 S10).
 */
class FhirAdvisoryOutputAggregatorTest {

    private static final class FakeResult implements IResult<IResultSignal> {
        private final IResultSignal signal;
        FakeResult(IResultSignal s) { this.signal = s; }
        @Override public IResultSignal getResult() { return signal; }
        @Override public Long getNeuronId() { return 42L; }
    }

    private FhirBridgeConfig configFor(Path audit, Path advisory) {
        return new FhirBridgeConfig(
                new FhirBridgeConfig.FhirEndpointConfig(
                        "https://example/api/FHIR/R4",
                        FhirBridgeConfig.FhirVersion.R4,
                        60L),
                FhirBridgeConfig.SecurityConfig.of(
                        FhirBridgeConfig.SecurityType.NONE,
                        null, null, null, null, null, null, null, null),
                new FhirBridgeConfig.CohortConfig(List.of("pid-1"), null),
                List.of(),
                FhirBridgeConfig.PrivacyConfig.of(true, null, true),
                new FhirBridgeConfig.AuditConfig(audit.toString(), advisory.toString()),
                Map.<String, BridgeSafetyMode>of(),
                null);
    }

    @Test
    void treatmentProposalIsWrittenToAdvisoryAndAuditNotFhir(@TempDir Path tmp) throws IOException {
        Path auditFile = tmp.resolve("audit.jsonl");
        Path advisoryFile = tmp.resolve("advisory.jsonl");
        FhirBridgeConfig cfg = configFor(auditFile, advisoryFile);
        InMemoryFhirTransport transport = new InMemoryFhirTransport();
        try (FhirAuditOutput audit = new FhirAuditOutput(auditFile);
             FhirClientService svc = new FhirClientService(cfg, transport, audit);
             FhirAdvisoryOutputAggregator agg = new FhirAdvisoryOutputAggregator(svc, audit)) {
            svc.start();
            TreatmentProposalSignal prop = new TreatmentProposalSignal(
                    "RxNorm:5856", 0.8, 0.1,
                    "consider raising basal insulin",
                    "pid-1");
            agg.save(List.<IResult>of(new FakeResult(prop)), 1_700_000_000L, 7L, null);
        }
        String advisory = Files.readString(advisoryFile);
        assertTrue(advisory.contains("ADVISORY"), "advisory verdict must be present");
        assertTrue(advisory.contains("RxNorm:5856"));
        assertFalse(advisory.contains("\"pid-1\""),
                "§3 rule 3 — raw cohort id must not appear in advisory file");
        // Audit JSONL must record an APPLIED verdict for the proposal.
        String auditContent = Files.readString(auditFile);
        assertTrue(auditContent.contains("TREATMENT_PROPOSAL"),
                "audit must record TREATMENT_PROPOSAL");
    }

    @Test
    void clinicalVetoIsRecordedAsRejection(@TempDir Path tmp) throws IOException {
        Path auditFile = tmp.resolve("audit.jsonl");
        Path advisoryFile = tmp.resolve("advisory.jsonl");
        FhirBridgeConfig cfg = configFor(auditFile, advisoryFile);
        InMemoryFhirTransport transport = new InMemoryFhirTransport();
        try (FhirAuditOutput audit = new FhirAuditOutput(auditFile);
             FhirClientService svc = new FhirClientService(cfg, transport, audit);
             FhirAdvisoryOutputAggregator agg = new FhirAdvisoryOutputAggregator(svc, audit)) {
            svc.start();
            ClinicalVetoSignal veto = new ClinicalVetoSignal(
                    "RxNorm:5856", "contraindicated-with-renal-failure",
                    HarmVerdict.HARMFUL,
                    "KDIGO-2024",
                    List.of("RxNorm:00000"),
                    "pid-1");
            agg.save(List.<IResult>of(new FakeResult(veto)), 1_700_000_000L, 7L, null);
        }
        String advisory = Files.readString(advisoryFile);
        assertTrue(advisory.contains("VETO"));
        assertTrue(advisory.contains("KDIGO-2024"));
        String auditContent = Files.readString(auditFile);
        assertTrue(auditContent.contains("CLINICAL_VETO"),
                "audit must record CLINICAL_VETO");
    }

    @Test
    void adverseEventAlertIsWrittenWithSeverity(@TempDir Path tmp) throws IOException {
        Path auditFile = tmp.resolve("audit.jsonl");
        Path advisoryFile = tmp.resolve("advisory.jsonl");
        FhirBridgeConfig cfg = configFor(auditFile, advisoryFile);
        InMemoryFhirTransport transport = new InMemoryFhirTransport();
        try (FhirAuditOutput audit = new FhirAuditOutput(auditFile);
             FhirClientService svc = new FhirClientService(cfg, transport, audit);
             FhirAdvisoryOutputAggregator agg = new FhirAdvisoryOutputAggregator(svc, audit)) {
            svc.start();
            AdverseEventAlertSignal alert = new AdverseEventAlertSignal(
                    AlertSeverity.CRITICAL, "ALLERGY:penicillin", "pid-1");
            agg.save(List.<IResult>of(new FakeResult(alert)), 1_700_000_000L, 7L, null);
        }
        String advisory = Files.readString(advisoryFile);
        assertTrue(advisory.contains("ALERT"));
        assertTrue(advisory.contains("CRITICAL"));
    }

    /** §3 rule 3 — even if a signal carries the raw cohort id, it is stripped at egress. */
    @Test
    void rawPatientIdIsScrubbedAtEgress(@TempDir Path tmp) throws IOException {
        Path auditFile = tmp.resolve("audit.jsonl");
        Path advisoryFile = tmp.resolve("advisory.jsonl");
        FhirBridgeConfig cfg = configFor(auditFile, advisoryFile);
        InMemoryFhirTransport transport = new InMemoryFhirTransport();
        try (FhirAuditOutput audit = new FhirAuditOutput(auditFile);
             FhirClientService svc = new FhirClientService(cfg, transport, audit);
             FhirAdvisoryOutputAggregator agg = new FhirAdvisoryOutputAggregator(svc, audit)) {
            svc.start();
            TreatmentProposalSignal prop = new TreatmentProposalSignal(
                    "RxNorm:5856", 0.8, 0.1,
                    "test", "patient-with-leaked-id");
            agg.save(List.<IResult>of(new FakeResult(prop)), 1_700_000_000L, 7L, null);
        }
        String advisory = Files.readString(advisoryFile);
        assertFalse(advisory.contains("patient-with-leaked-id"),
                "raw cohort id must not appear in advisory output");
    }
}
