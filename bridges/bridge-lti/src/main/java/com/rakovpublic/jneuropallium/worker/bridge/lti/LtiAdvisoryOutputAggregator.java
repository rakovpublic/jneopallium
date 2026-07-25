/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.lti;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rakovpublic.jneuropallium.worker.application.IOutputAggregator;
import com.rakovpublic.jneuropallium.worker.bridge.common.AbstractBridgeAuditOutput;
import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeAuditRecord;
import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeSafetyMode;
import com.rakovpublic.jneuropallium.worker.net.layers.IResult;
import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.tutoring.ContentRecommendationSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.tutoring.HintSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.tutoring.InterventionSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.tutoring.MasteryUpdateSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.tutoring.ScaffoldingSignal;
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
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Egress for the LTI / xAPI bridge (14-LTI-XAPI.md §3, §4, §5 egress
 * table).
 *
 * <p><b>This aggregator never auto-grades and never auto-enrols.</b>
 * Egress channels:
 *
 * <ol>
 *   <li>xAPI {@code recommended} statement to the LRS — the bridge is the
 *       actor, never the learner (so the LRS record makes it clear the
 *       statement came from Jneopallium, not the learner).</li>
 *   <li>xAPI {@code experienced} statement for hints / scaffolds rendered
 *       in the tool UI — same actor invariant.</li>
 *   <li>AGS {@code Score} with {@code gradingProgress=PendingManual} when
 *       a {@link MasteryUpdateSignal} is bound to an AGS line item — the
 *       LMS is responsible for the instructor confirmation step.</li>
 *   <li>Local JSONL advisory file — the always-on record an instructor /
 *       reviewer / SIS audit can consume offline.</li>
 * </ol>
 *
 * <p>Every egress event also produces a row in the universal bridge audit
 * record file (00-FRAMEWORK §4).
 */
public final class LtiAdvisoryOutputAggregator implements IOutputAggregator, AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(LtiAdvisoryOutputAggregator.class);

    public static final String VERDICT_ADVISORY  = "ADVISORY";
    public static final String VERDICT_PROPOSAL  = "SCORE_PROPOSAL";
    public static final String VERDICT_HINT      = "HINT";
    public static final String VERDICT_INTERVENE = "INTERVENTION";

    /** Default tool actor written into xAPI statements when no override binds. */
    public static final String DEFAULT_TOOL_ACTOR = "Jneopallium-Tutor";

    private final XapiClientService svc;
    private final AbstractBridgeAuditOutput audit;
    private final ObjectMapper mapper;
    private final Path advisoryFile;
    private BufferedWriter advisoryWriter;
    private boolean advisoryDegraded;
    private LtiClientService.LaunchContext activeLaunch;

    public LtiAdvisoryOutputAggregator(XapiClientService svc, AbstractBridgeAuditOutput audit) {
        this.svc = Objects.requireNonNull(svc, "svc");
        this.audit = Objects.requireNonNull(audit, "audit");
        this.mapper = new ObjectMapper().disable(SerializationFeature.INDENT_OUTPUT);
        LtiBridgeConfig.AuditConfig auditCfg = svc.config().audit();
        this.advisoryFile = auditCfg.advisoryFile() == null
                ? null : Paths.get(auditCfg.advisoryFile());
        openAdvisoryWriter();
    }

    /** Bind an active LTI launch so AGS / line-item URLs are available for proposals. */
    public synchronized void setActiveLaunch(LtiClientService.LaunchContext ctx) {
        this.activeLaunch = ctx;
    }

    public synchronized LtiClientService.LaunchContext activeLaunch() {
        return activeLaunch;
    }

    private synchronized void openAdvisoryWriter() {
        if (advisoryFile == null) return;
        try {
            if (advisoryFile.getParent() != null) Files.createDirectories(advisoryFile.getParent());
            advisoryWriter = Files.newBufferedWriter(advisoryFile, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            log.warn("LtiAdvisoryOutputAggregator: advisory file {} unwritable, degraded: {}",
                    advisoryFile, e.getMessage());
            advisoryWriter = null;
            advisoryDegraded = true;
        }
    }

    @Override
    public synchronized void save(List<IResult> results, long timestamp, long run, IContext context) {
        if (results == null || results.isEmpty()) return;
        LtiBridgeConfig.WriteBindingConfig recommendBinding = findWriteBinding("recommend");
        LtiBridgeConfig.WriteBindingConfig agsBinding = findAgsBinding();
        for (IResult r : results) {
            if (r == null) continue;
            IResultSignal<?> s = r.getResult();
            try {
                if (s instanceof ContentRecommendationSignal rec) {
                    handleRecommendation(rec, recommendBinding, timestamp, run, r.getNeuronId());
                } else if (s instanceof MasteryUpdateSignal mu) {
                    handleMastery(mu, agsBinding, timestamp, run, r.getNeuronId());
                } else if (s instanceof HintSignal h) {
                    handleHint(h, timestamp, run, r.getNeuronId());
                } else if (s instanceof InterventionSignal i) {
                    handleIntervention(i, timestamp, run, r.getNeuronId());
                } else if (s instanceof ScaffoldingSignal sc) {
                    handleScaffold(sc, timestamp, run, r.getNeuronId());
                }
            } catch (RuntimeException ex) {
                audit.append(new BridgeAuditRecord(
                        timestamp, run, XapiClientService.BRIDGE_NAME,
                        BridgeAuditRecord.Verdict.FAILED,
                        null, null, null, null,
                        BridgeAuditRecord.RejectReason.EXCEPTION + ":" + ex.getClass().getSimpleName(),
                        BridgeSafetyMode.ADVISORY, List.of()));
                log.warn("LtiAdvisoryOutputAggregator: signal {} failed: {}",
                        s == null ? "null" : s.getClass().getSimpleName(), ex.getMessage());
            }
        }
    }

    private void handleRecommendation(ContentRecommendationSignal rec,
                                      LtiBridgeConfig.WriteBindingConfig binding,
                                      long ts, long run, Long neuronId) {
        String actor = binding == null || binding.targetActor() == null
                ? DEFAULT_TOOL_ACTOR : binding.targetActor();
        String tag = binding == null ? "TUTOR.RECOMMEND" : binding.signalTag();
        String verb = binding == null || binding.xapiVerb() == null
                ? XapiStatementMapper.VERB_EXPERIENCED : binding.xapiVerb();
        ObjectNode stmt = recommendedStatement(actor, verb, rec.getItemId(), rec.getRationale(),
                rec.getExpectedZPD());
        postStatementBestEffort(stmt, tag, "recommendation");
        AdvisoryRecord adv = new AdvisoryRecord(ts, run, VERDICT_ADVISORY,
                rec.getItemId(), rec.getRationale(), rec.getExpectedZPD(),
                learnerPseudonym(), null, null,
                neuronId == null ? null : String.valueOf(neuronId));
        writeAdvisory(adv);
        audit.append(new BridgeAuditRecord(
                ts, run, XapiClientService.BRIDGE_NAME,
                BridgeAuditRecord.Verdict.APPLIED,
                "advisory", tag, null, rec.getExpectedZPD(),
                "RECOMMEND",
                BridgeSafetyMode.ADVISORY,
                neuronId == null ? List.of() : List.of(String.valueOf(neuronId))));
    }

    private void handleMastery(MasteryUpdateSignal mu,
                               LtiBridgeConfig.WriteBindingConfig binding,
                               long ts, long run, Long neuronId) {
        // §3 — mastery update is purely a proposal. The aggregator posts an
        // AGS Score with gradingProgress=PendingManual when the binding asks
        // for it; otherwise the proposal is advisory-only.
        if (binding != null && binding.ltiAgs()
                && activeLaunch != null && activeLaunch.agsLineItemUrl() != null) {
            ObjectNode score = mapper.createObjectNode();
            score.put("scoreGiven", mu.getNewMasteryLevel());
            score.put("scoreMaximum", 1.0);
            score.put("activityProgress", "Submitted");
            score.put("gradingProgress", LtiBridgeConfig.GradingProgress.PENDING_MANUAL);
            score.put("timestamp", Instant.ofEpochMilli(ts).toString());
            score.put("userId", learnerPseudonym() == null ? "" : learnerPseudonym());
            try {
                svc.transport().postAgsScore(activeLaunch.agsLineItemUrl(), score);
            } catch (IOException | RuntimeException e) {
                log.warn("AGS post failed: {}", e.getMessage());
            }
        }
        AdvisoryRecord adv = new AdvisoryRecord(ts, run, VERDICT_PROPOSAL,
                mu.getConceptId(), null, mu.getNewMasteryLevel(),
                learnerPseudonym(), null,
                LtiBridgeConfig.GradingProgress.PENDING_MANUAL,
                neuronId == null ? null : String.valueOf(neuronId));
        writeAdvisory(adv);
        audit.append(new BridgeAuditRecord(
                ts, run, XapiClientService.BRIDGE_NAME,
                BridgeAuditRecord.Verdict.APPLIED,
                "advisory",
                binding == null ? "TUTOR.SCORE.PROPOSAL" : binding.signalTag(),
                null, mu.getNewMasteryLevel(),
                "PROPOSED_SCORE:gradingProgress=" + LtiBridgeConfig.GradingProgress.PENDING_MANUAL,
                BridgeSafetyMode.ADVISORY,
                neuronId == null ? List.of() : List.of(String.valueOf(neuronId))));
    }

    private void handleHint(HintSignal h, long ts, long run, Long neuronId) {
        ObjectNode stmt = experiencedStatement(DEFAULT_TOOL_ACTOR, h.getItemId(),
                h.getLevel() == null ? "hint" : h.getLevel().name());
        postStatementBestEffort(stmt, "TUTOR.HINT", "hint");
        AdvisoryRecord adv = new AdvisoryRecord(ts, run, VERDICT_HINT,
                h.getItemId(),
                h.getLevel() == null ? null : h.getLevel().name(),
                null, learnerPseudonym(),
                null, null,
                neuronId == null ? null : String.valueOf(neuronId));
        writeAdvisory(adv);
        audit.append(new BridgeAuditRecord(
                ts, run, XapiClientService.BRIDGE_NAME,
                BridgeAuditRecord.Verdict.APPLIED,
                "advisory", "TUTOR.HINT", null, null,
                "HINT:" + (h.getLevel() == null ? "n/a" : h.getLevel().name()),
                BridgeSafetyMode.ADVISORY,
                neuronId == null ? List.of() : List.of(String.valueOf(neuronId))));
    }

    private void handleIntervention(InterventionSignal i, long ts, long run, Long neuronId) {
        ObjectNode stmt = experiencedStatement(DEFAULT_TOOL_ACTOR,
                i.getType() == null ? "intervention" : i.getType().name(),
                i.getReason());
        postStatementBestEffort(stmt, "TUTOR.INTERVENE", "intervention");
        AdvisoryRecord adv = new AdvisoryRecord(ts, run, VERDICT_INTERVENE,
                i.getType() == null ? null : i.getType().name(),
                i.getReason(), null, learnerPseudonym(),
                null, null,
                neuronId == null ? null : String.valueOf(neuronId));
        writeAdvisory(adv);
        audit.append(new BridgeAuditRecord(
                ts, run, XapiClientService.BRIDGE_NAME,
                BridgeAuditRecord.Verdict.APPLIED,
                "advisory", "TUTOR.INTERVENE", null, null,
                "INTERVENTION:" + (i.getType() == null ? "n/a" : i.getType().name()),
                BridgeSafetyMode.ADVISORY,
                neuronId == null ? List.of() : List.of(String.valueOf(neuronId))));
    }

    private void handleScaffold(ScaffoldingSignal sc, long ts, long run, Long neuronId) {
        String typeName = sc.getType() == null ? "scaffolding" : sc.getType().name();
        ObjectNode stmt = experiencedStatement(DEFAULT_TOOL_ACTOR, "scaffolding", typeName);
        postStatementBestEffort(stmt, "TUTOR.SCAFFOLD", "scaffold");
        AdvisoryRecord adv = new AdvisoryRecord(ts, run, VERDICT_ADVISORY,
                "scaffolding", typeName, null, learnerPseudonym(),
                null, null,
                neuronId == null ? null : String.valueOf(neuronId));
        writeAdvisory(adv);
        audit.append(new BridgeAuditRecord(
                ts, run, XapiClientService.BRIDGE_NAME,
                BridgeAuditRecord.Verdict.APPLIED,
                "advisory", "TUTOR.SCAFFOLD", null, null,
                "SCAFFOLD",
                BridgeSafetyMode.ADVISORY,
                neuronId == null ? List.of() : List.of(String.valueOf(neuronId))));
    }

    private void postStatementBestEffort(ObjectNode stmt, String tag, String kind) {
        if (svc.transport() == null) return;
        try {
            svc.transport().postStatement(stmt);
        } catch (IOException | RuntimeException e) {
            log.warn("xAPI {} post failed: {}", kind, e.getMessage());
            audit.append(new BridgeAuditRecord(
                    System.currentTimeMillis(), 0, XapiClientService.BRIDGE_NAME,
                    BridgeAuditRecord.Verdict.FAILED,
                    "advisory", tag, null, null,
                    BridgeAuditRecord.RejectReason.EXCEPTION + ":" + e.getClass().getSimpleName(),
                    BridgeSafetyMode.ADVISORY, List.of()));
        }
    }

    private LtiBridgeConfig.WriteBindingConfig findWriteBinding(String verbContains) {
        for (LtiBridgeConfig.WriteBindingConfig w : svc.config().writes()) {
            if (w.xapiVerb() != null && w.xapiVerb().toLowerCase().contains(verbContains)) {
                return w;
            }
        }
        return null;
    }

    private LtiBridgeConfig.WriteBindingConfig findAgsBinding() {
        for (LtiBridgeConfig.WriteBindingConfig w : svc.config().writes()) {
            if (w.ltiAgs()) return w;
        }
        return null;
    }

    private String learnerPseudonym() {
        if (activeLaunch == null || activeLaunch.subject() == null) return null;
        return svc.pseudonymService().pseudonymise(activeLaunch.subject());
    }

    private ObjectNode recommendedStatement(String actor, String verb,
                                            String itemId, String rationale, double zpd) {
        ObjectNode stmt = mapper.createObjectNode();
        ObjectNode a = stmt.putObject("actor");
        a.put("name", actor);
        a.putObject("account").put("homePage", "https://jneopallium.rakov.org")
                .put("name", actor);
        stmt.putObject("verb").put("id", verb);
        ObjectNode obj = stmt.putObject("object");
        obj.put("id", itemId == null ? "" : itemId);
        obj.put("objectType", "Activity");
        if (rationale != null) {
            stmt.putObject("result").putObject("extensions")
                    .put("https://jneopallium.rakov.org/xapi/extensions/rationale", rationale)
                    .put("https://jneopallium.rakov.org/xapi/extensions/zpd", zpd);
        }
        stmt.put("timestamp", Instant.now().toString());
        return stmt;
    }

    private ObjectNode experiencedStatement(String actor, String itemId, String detail) {
        ObjectNode stmt = mapper.createObjectNode();
        ObjectNode a = stmt.putObject("actor");
        a.put("name", actor);
        a.putObject("account").put("homePage", "https://jneopallium.rakov.org")
                .put("name", actor);
        stmt.putObject("verb").put("id", XapiStatementMapper.VERB_EXPERIENCED);
        ObjectNode obj = stmt.putObject("object");
        obj.put("id", itemId == null ? "" : itemId);
        obj.put("objectType", "Activity");
        if (detail != null) {
            stmt.putObject("result").putObject("extensions")
                    .put("https://jneopallium.rakov.org/xapi/extensions/detail", detail);
        }
        stmt.put("timestamp", Instant.now().toString());
        return stmt;
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
     * stable so downstream tooling (instructor dashboard, LMS side-panel,
     * SIS audit) can deserialise the same schema regardless of verdict.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyOrder({
            "ts", "run", "verdict", "code", "detail", "scoreOrZpd",
            "learnerPseudonym", "severity", "gradingProgress", "evidenceNeuron"
    })
    public record AdvisoryRecord(
            long ts,
            long run,
            String verdict,
            String code,
            String detail,
            Double scoreOrZpd,
            String learnerPseudonym,
            String severity,
            String gradingProgress,
            String evidenceNeuron
    ) {}
}
