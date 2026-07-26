/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.fhir;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.rakovpublic.jneuropallium.worker.application.IOutputAggregator;
import com.rakovpublic.jneuropallium.worker.bridge.common.AbstractBridgeAuditOutput;
import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeAuditRecord;
import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeSafetyMode;
import com.rakovpublic.jneuropallium.worker.net.layers.IResult;
import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.AdverseEventAlertSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.ClinicalVetoSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.TreatmentProposalSignal;
import com.rakovpublic.jneuropallium.worker.util.IContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Objects;

/**
 * Outside-FHIR advisory egress for the FHIR bridge (06-FHIR.md §3 rule 2,
 * §4 architecture diagram, §5 egress table).
 *
 * <p><b>This aggregator does not write to FHIR.</b> It does not import any
 * FHIR client API, does not own a {@link FhirTransport} reference, and has
 * no code path that constructs a {@code MedicationRequest},
 * {@code ServiceRequest}, {@code Communication}, or any other FHIR
 * resource. Treatment proposals, clinical vetoes, and adverse-event
 * alerts are written to:
 *
 * <ol>
 *   <li>The bridge's local JSONL audit file (the universal §4 schema), and</li>
 *   <li>An optional advisory JSONL file at
 *       {@link FhirBridgeConfig.AuditConfig#advisoryFile()}, where
 *       physician-facing tooling (DICOM viewer overlay, EHR side-panel,
 *       message queue) consumes them.</li>
 * </ol>
 *
 * <p>Physician confirmation always remains external — the bridge ceiling
 * is permanently {@code ADVISORY} (§0).
 */
public final class FhirAdvisoryOutputAggregator implements IOutputAggregator, AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(FhirAdvisoryOutputAggregator.class);

    public static final String VERDICT_ADVISORY = "ADVISORY";
    public static final String VERDICT_VETO = "VETO";
    public static final String VERDICT_ALERT = "ALERT";

    private final FhirClientService svc;
    private final AbstractBridgeAuditOutput audit;
    private final ObjectMapper mapper;
    private final Path advisoryFile;
    private BufferedWriter advisoryWriter;
    private boolean advisoryDegraded;

    public FhirAdvisoryOutputAggregator(FhirClientService svc, AbstractBridgeAuditOutput audit) {
        this.svc = Objects.requireNonNull(svc, "svc");
        this.audit = Objects.requireNonNull(audit, "audit");
        this.mapper = new ObjectMapper().disable(SerializationFeature.INDENT_OUTPUT);
        FhirBridgeConfig.AuditConfig auditCfg = svc.config().audit();
        this.advisoryFile = auditCfg.advisoryFile() == null
                ? null : Paths.get(auditCfg.advisoryFile());
        openAdvisoryWriter();
    }

    private synchronized void openAdvisoryWriter() {
        if (advisoryFile == null) return;
        try {
            if (advisoryFile.getParent() != null) Files.createDirectories(advisoryFile.getParent());
            advisoryWriter = Files.newBufferedWriter(advisoryFile, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            log.warn("FhirAdvisoryOutputAggregator: advisory file {} unwritable, degraded: {}",
                    advisoryFile, e.getMessage());
            advisoryWriter = null;
            advisoryDegraded = true;
        }
    }

    @Override
    public synchronized void save(List<IResult> results, long timestamp, long run, IContext context) {
        if (results == null || results.isEmpty()) return;
        for (IResult r : results) {
            if (r == null) continue;
            IResultSignal<?> s = r.getResult();
            try {
                if (s instanceof TreatmentProposalSignal proposal) {
                    handleProposal(proposal, timestamp, run, r.getNeuronId());
                } else if (s instanceof ClinicalVetoSignal veto) {
                    handleVeto(veto, timestamp, run, r.getNeuronId());
                } else if (s instanceof AdverseEventAlertSignal alert) {
                    handleAlert(alert, timestamp, run, r.getNeuronId());
                }
            } catch (RuntimeException ex) {
                audit.append(new BridgeAuditRecord(
                        timestamp, run, FhirClientService.BRIDGE_NAME,
                        BridgeAuditRecord.Verdict.FAILED,
                        null, null, null, null,
                        BridgeAuditRecord.RejectReason.EXCEPTION + ":" + ex.getClass().getSimpleName(),
                        BridgeSafetyMode.ADVISORY, List.of()));
            }
        }
    }

    private void handleProposal(TreatmentProposalSignal proposal,
                                long ts, long run, Long neuronId) {
        AdvisoryRecord rec = new AdvisoryRecord(
                ts, run, VERDICT_ADVISORY,
                proposal.getRxNormOrProcedureCode(),
                proposal.getExpectedBenefit(),
                proposal.getExpectedRisk(),
                proposal.getRationale(),
                stripRawPid(proposal.getPatientId()),
                null, null,
                neuronId == null ? null : String.valueOf(neuronId));
        writeAdvisory(rec);
        audit.append(new BridgeAuditRecord(
                ts, run, FhirClientService.BRIDGE_NAME,
                BridgeAuditRecord.Verdict.APPLIED,
                "advisory", proposal.getRxNormOrProcedureCode(),
                proposal.getExpectedRisk(), proposal.getExpectedBenefit(),
                "TREATMENT_PROPOSAL", BridgeSafetyMode.ADVISORY,
                neuronId == null ? List.of() : List.of(String.valueOf(neuronId))));
    }

    private void handleVeto(ClinicalVetoSignal veto, long ts, long run, Long neuronId) {
        AdvisoryRecord rec = new AdvisoryRecord(
                ts, run, VERDICT_VETO,
                veto.getActionPlanId(),
                null, null,
                veto.getVetoReason(),
                stripRawPid(veto.getPatientId()),
                veto.getGuidelineCitation(),
                veto.getAlternativeCodes(),
                neuronId == null ? null : String.valueOf(neuronId));
        writeAdvisory(rec);
        audit.append(new BridgeAuditRecord(
                ts, run, FhirClientService.BRIDGE_NAME,
                BridgeAuditRecord.Verdict.REJECTED,
                "advisory", veto.getActionPlanId(),
                null, null,
                "CLINICAL_VETO:" + veto.getVetoReason(),
                BridgeSafetyMode.ADVISORY,
                neuronId == null ? List.of() : List.of(String.valueOf(neuronId))));
    }

    private void handleAlert(AdverseEventAlertSignal alert, long ts, long run, Long neuronId) {
        AdvisoryRecord rec = new AdvisoryRecord(
                ts, run, VERDICT_ALERT,
                alert.getEventCode(),
                null, null,
                alert.getDetail(),
                stripRawPid(alert.getPatientId()),
                alert.getSeverity() == null ? null : alert.getSeverity().name(),
                null,
                neuronId == null ? null : String.valueOf(neuronId));
        writeAdvisory(rec);
        audit.append(new BridgeAuditRecord(
                ts, run, FhirClientService.BRIDGE_NAME,
                BridgeAuditRecord.Verdict.APPLIED,
                "advisory", alert.getEventCode(),
                null, null,
                "ADVERSE_EVENT_ALERT:" + (alert.getSeverity() == null ? "INFO" : alert.getSeverity().name()),
                BridgeSafetyMode.ADVISORY,
                neuronId == null ? List.of() : List.of(String.valueOf(neuronId))));
    }

    /**
     * Defensive last line — even if a signal somehow carried the raw
     * cohort identifier, run it through pseudonymisation here. This is
     * the §3 rule 3 "safety filter" referenced in the spec.
     */
    private String stripRawPid(String maybeRawPid) {
        if (maybeRawPid == null) return null;
        if (maybeRawPid.length() == PseudonymService.ID_LEN
                && isHex(maybeRawPid)) {
            return maybeRawPid;
        }
        return svc.pseudonymService().pseudonymise(maybeRawPid);
    }

    private static boolean isHex(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            boolean ok = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
            if (!ok) return false;
        }
        return true;
    }

    private synchronized void writeAdvisory(AdvisoryRecord record) {
        if (advisoryWriter == null) return;
        try {
            advisoryWriter.write(mapper.writeValueAsString(record));
            advisoryWriter.newLine();
            advisoryWriter.flush();
        } catch (IOException e) {
            if (!advisoryDegraded) {
                log.warn("Advisory writer degraded: {}", e.getMessage());
                advisoryDegraded = true;
            }
        }
    }

    public boolean isAdvisoryDegraded() { return advisoryDegraded; }

    @Override
    public synchronized void close() {
        if (advisoryWriter != null) {
            try { advisoryWriter.close(); } catch (IOException e) {
                log.warn("Advisory writer close threw: {}", e.getMessage());
            }
            advisoryWriter = null;
        }
    }

    /**
     * Single-line JSON record written to the advisory file. Field shape is
     * stable so downstream tooling (EHR side-panel, message queue
     * subscriber) can deserialise the same schema regardless of the
     * advisory verdict.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyOrder({
            "ts", "run", "verdict", "code", "expectedBenefit", "expectedRisk",
            "rationale", "patientPseudonym", "severity", "alternativeCodes",
            "evidenceNeuron"
    })
    public record AdvisoryRecord(
            long ts,
            long run,
            String verdict,
            String code,
            Double expectedBenefit,
            Double expectedRisk,
            String rationale,
            String patientPseudonym,
            String severity,
            List<String> alternativeCodes,
            String evidenceNeuron
    ) {
        public AdvisoryRecord {
            alternativeCodes = alternativeCodes == null ? null : List.copyOf(alternativeCodes);
        }
    }
}
