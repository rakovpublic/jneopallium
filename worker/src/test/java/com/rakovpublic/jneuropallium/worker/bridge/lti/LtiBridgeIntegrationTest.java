/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.lti;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeSafetyMode;
import com.rakovpublic.jneuropallium.worker.net.layers.IResult;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.tutoring.ContentRecommendationSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.tutoring.EngagementSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.tutoring.MasteryUpdateSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.tutoring.ResponseSignal;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end LTI / xAPI bridge tests (14-LTI-XAPI.md §9 acceptance
 * scenarios S7, S8, S9, S10, S11, S12).
 */
class LtiBridgeIntegrationTest {

    private final ObjectMapper json = new ObjectMapper();

    private static final class FakeResult implements IResult<IResultSignal> {
        private final IResultSignal signal;
        FakeResult(IResultSignal s) { this.signal = s; }
        @Override public IResultSignal getResult() { return signal; }
        @Override public Long getNeuronId() { return 42L; }
    }

    private LtiBridgeConfig baseConfig(Path auditFile, Path advisoryFile) {
        return new LtiBridgeConfig(
                new LtiBridgeConfig.LtiSection(
                        "jneopallium-tutor",
                        List.of(new LtiBridgeConfig.PlatformConfig(
                                "https://canvas.example.edu",
                                "10000000000123",
                                "https://canvas.example.edu/api/lti/authorize_redirect",
                                "https://canvas.example.edu/login/oauth2/token",
                                "https://canvas.example.edu/api/lti/security/jwks",
                                List.of("1:abcdef"))),
                        null, null),
                new LtiBridgeConfig.XapiSection(
                        LtiBridgeConfig.XapiMode.PULL,
                        new LtiBridgeConfig.LrsConfig(
                                "https://lrs.example.edu/xapi",
                                new LtiBridgeConfig.AuthConfig(
                                        LtiBridgeConfig.AuthType.NONE, null, null, null),
                                60L)),
                new LtiBridgeConfig.CohortConfig(List.of("course-101")),
                List.of(
                        new LtiBridgeConfig.ReadBindingConfig(
                                "ATTEMPTS",
                                XapiStatementMapper.VERB_ATTEMPTED,
                                LtiBridgeConfig.TargetSignal.RESPONSE,
                                "TUTOR.ATTEMPT"),
                        new LtiBridgeConfig.ReadBindingConfig(
                                "ENGAGEMENT",
                                XapiStatementMapper.VERB_INTERACTED,
                                LtiBridgeConfig.TargetSignal.ENGAGEMENT,
                                "TUTOR.ENGAGE")),
                List.of(
                        new LtiBridgeConfig.WriteBindingConfig(
                                "RECOMMENDATIONS",
                                "http://activitystrea.ms/recommend",
                                "Jneopallium-Tutor",
                                false, null, "TUTOR.RECOMMEND"),
                        new LtiBridgeConfig.WriteBindingConfig(
                                "PROPOSED-SCORES",
                                null, null, true,
                                LtiBridgeConfig.GradingProgress.PENDING_MANUAL,
                                "TUTOR.SCORE.PROPOSAL")),
                new LtiBridgeConfig.PrivacyConfig(true, null, true),
                new LtiBridgeConfig.AuditConfig(auditFile.toString(), advisoryFile.toString()),
                Map.of("RECOMMENDATIONS", BridgeSafetyMode.ADVISORY),
                null);
    }

    /** §9 S7 — pull statements from an LRS and emit ResponseSignals + EngagementSignals. */
    @Test
    void s7_lrsPullEmitsResponseAndEngagementSignals(@TempDir Path tmp) throws Exception {
        Path auditFile = tmp.resolve("audit.jsonl");
        LtiBridgeConfig cfg = baseConfig(auditFile, tmp.resolve("advisory.jsonl"));
        InMemoryXapiTransport transport = new InMemoryXapiTransport();
        transport.putStatements(
                "statements?verb=" + java.net.URLEncoder.encode(
                        XapiStatementMapper.VERB_ATTEMPTED, java.nio.charset.StandardCharsets.UTF_8),
                json.readTree("""
                        {
                          "actor": { "account": { "name": "alice@example.edu" } },
                          "verb":  { "id": "http://adlnet.gov/expapi/verbs/attempted" },
                          "object":{ "id": "item-42" },
                          "result":{ "success": true, "duration": "PT12S" }
                        }
                        """));
        transport.putStatements(
                "statements?verb=" + java.net.URLEncoder.encode(
                        XapiStatementMapper.VERB_INTERACTED, java.nio.charset.StandardCharsets.UTF_8),
                json.readTree("""
                        {
                          "actor": { "account": { "name": "alice@example.edu" } },
                          "verb":  { "id": "http://adlnet.gov/expapi/verbs/interacted" },
                          "object":{ "id": "module-1" },
                          "result":{ "duration": "PT5M" }
                        }
                        """));
        try (LtiAuditOutput audit = new LtiAuditOutput(auditFile);
             XapiClientService svc = new XapiClientService(cfg, transport, audit)) {
            svc.start();
            svc.poll();
            XapiResponseInput rIn = new XapiResponseInput("attempts", svc, List.of("ATTEMPTS"));
            XapiEngagementInput eIn = new XapiEngagementInput("engage", svc, List.of("ENGAGEMENT"));
            List<IInputSignal> rSigs = rIn.readSignals();
            List<IInputSignal> eSigs = eIn.readSignals();
            assertEquals(1, rSigs.size());
            assertEquals(1, eSigs.size());
            assertTrue(rSigs.get(0) instanceof ResponseSignal);
            assertTrue(eSigs.get(0) instanceof EngagementSignal);
            ResponseSignal r = (ResponseSignal) rSigs.get(0);
            assertEquals("item-42", r.getItemId());
            assertTrue(r.isCorrect());
        }
    }

    /** §9 S8 — successful LTI 1.3 launch decodes course context. */
    @Test
    void s8_ltiLaunchDecodesContext(@TempDir Path tmp) throws Exception {
        Path auditFile = tmp.resolve("audit.jsonl");
        LtiBridgeConfig cfg = baseConfig(auditFile, tmp.resolve("advisory.jsonl"));
        LtiClientService lti = new LtiClientService(cfg);
        ObjectNode header = json.createObjectNode();
        header.put("alg", "RS256");
        header.put("kid", "test-key-1");
        ObjectNode payload = json.createObjectNode();
        payload.put("iss", "https://canvas.example.edu");
        payload.put("aud", "10000000000123");
        payload.put("sub", "alice@example.edu");
        payload.put("nonce", "n-1");
        payload.put("exp", (System.currentTimeMillis() / 1000L) + 3600L);
        payload.put(LtiClientService.CLAIM_DEPLOYMENT_ID, "1:abcdef");
        ObjectNode ctx = payload.putObject(LtiClientService.CLAIM_CONTEXT);
        ctx.put("id", "course-101");
        ctx.put("title", "Introduction to Adaptive Tutoring");
        payload.putArray(LtiClientService.CLAIM_ROLES)
                .add(LtiClientService.ROLE_LEARNER);
        payload.putObject(LtiClientService.CLAIM_AGS)
                .put("lineitem", "https://canvas.example.edu/api/lti/courses/1/line_items/7")
                .put("lineitems", "https://canvas.example.edu/api/lti/courses/1/line_items");
        String idToken = LtiClientService.buildPermissiveIdToken(header, payload);
        LtiClientService.LaunchContext launch = lti.verifyLaunch(idToken);
        assertNotNull(launch);
        assertEquals("https://canvas.example.edu", launch.issuer());
        assertEquals("course-101", launch.contextId());
        assertTrue(launch.isLearner());
        assertFalse(launch.isInstructor());
        assertNotNull(launch.agsLineItemUrl());
    }

    /** §9 S8 — wrong issuer / wrong audience are rejected. */
    @Test
    void s8_ltiLaunchWithWrongIssuerIsRejected(@TempDir Path tmp) throws Exception {
        Path auditFile = tmp.resolve("audit.jsonl");
        LtiBridgeConfig cfg = baseConfig(auditFile, tmp.resolve("advisory.jsonl"));
        LtiClientService lti = new LtiClientService(cfg);
        ObjectNode payload = json.createObjectNode();
        payload.put("iss", "https://OTHER-platform.example.edu");
        payload.put("aud", "10000000000123");
        payload.put("sub", "bob");
        payload.put("exp", (System.currentTimeMillis() / 1000L) + 3600L);
        payload.put(LtiClientService.CLAIM_DEPLOYMENT_ID, "1:abcdef");
        String idToken = LtiClientService.buildPermissiveIdToken(
                json.createObjectNode().put("alg", "none"), payload);
        assertThrows(LtiClientService.LaunchVerificationException.class,
                () -> lti.verifyLaunch(idToken));
    }

    /** §9 S9 — pipeline emits a MasteryUpdate; an AGS Score is posted with PendingManual. */
    @Test
    void s9_masteryProposalIsPostedAsPendingManual(@TempDir Path tmp) throws Exception {
        Path auditFile = tmp.resolve("audit.jsonl");
        Path advisoryFile = tmp.resolve("advisory.jsonl");
        LtiBridgeConfig cfg = baseConfig(auditFile, advisoryFile);
        InMemoryXapiTransport transport = new InMemoryXapiTransport();
        try (LtiAuditOutput audit = new LtiAuditOutput(auditFile);
             XapiClientService svc = new XapiClientService(cfg, transport, audit);
             LtiAdvisoryOutputAggregator agg = new LtiAdvisoryOutputAggregator(svc, audit)) {
            svc.start();
            agg.setActiveLaunch(new LtiClientService.LaunchContext(
                    LtiPlatformBinding.fromConfig(cfg.lti().platforms().get(0)),
                    "https://canvas.example.edu", "10000000000123",
                    "alice@example.edu", "n-1",
                    "course-101", "Course 101",
                    List.of(LtiClientService.ROLE_LEARNER),
                    "1:abcdef",
                    "https://canvas.example.edu/api/lti/courses/1/line_items/7",
                    "https://canvas.example.edu/api/lti/courses/1/line_items",
                    null));
            MasteryUpdateSignal mu = new MasteryUpdateSignal("competency-7", 0.92);
            agg.save(List.<IResult>of(new FakeResult(mu)), 1_700_000_000L, 7L, null);
        }
        // AGS post must have happened with gradingProgress=PendingManual.
        List<InMemoryXapiTransport.PostedScore> posts = transport.postedScores();
        assertEquals(1, posts.size());
        JsonNode score = posts.get(0).body();
        assertEquals("PendingManual", score.get("gradingProgress").asText());
        assertEquals(1.0, score.get("scoreMaximum").asDouble(), 1e-9);
        String advisory = Files.readString(advisoryFile);
        assertTrue(advisory.contains("SCORE_PROPOSAL"));
        assertTrue(advisory.contains("PendingManual"));
    }

    /** §9 S10 — FullyGraded gradingProgress is rejected at config load. */
    @Test
    void s10_fullyGradedConfigIsRejected() {
        String yaml = """
                xapi:
                  mode: PULL
                  lrs:
                    endpoint: "https://lrs.example.edu/xapi"
                    pollIntervalSeconds: 60
                audit:
                  localAuditFile: /tmp/x.jsonl
                writes:
                  - bindingId: AGS-SCORE
                    ltiAgs: true
                    gradingProgress: "FullyGraded"
                """;
        Throwable t = assertThrows(Throwable.class, () -> LtiBridgeConfigLoader.load(yaml));
        // Walk to the deepest cause.
        while (t.getCause() != null && t.getCause() != t) t = t.getCause();
        String msg = t.getMessage() == null ? "" : t.getMessage();
        assertTrue(msg.contains("FullyGraded") || msg.contains("PendingManual"),
                "S10 — loader must reject FullyGraded with a clear message, got: " + msg);
    }

    /** §9 S11 — actor.account.name pseudonymised; raw email never appears in audit JSONL. */
    @Test
    void s11_pseudonymisationKeepsRawEmailOutOfAudit(@TempDir Path tmp) throws Exception {
        Path auditFile = tmp.resolve("audit.jsonl");
        Path advisoryFile = tmp.resolve("advisory.jsonl");
        LtiBridgeConfig cfg = baseConfig(auditFile, advisoryFile);
        InMemoryXapiTransport transport = new InMemoryXapiTransport();
        transport.putStatements(
                "statements?verb=" + java.net.URLEncoder.encode(
                        XapiStatementMapper.VERB_ATTEMPTED, java.nio.charset.StandardCharsets.UTF_8),
                json.readTree("""
                        {
                          "actor": { "account": { "name": "alice@example.edu" } },
                          "verb":  { "id": "http://adlnet.gov/expapi/verbs/attempted" },
                          "object":{ "id": "item-42" },
                          "result":{ "success": true }
                        }
                        """));
        try (LtiAuditOutput audit = new LtiAuditOutput(auditFile);
             XapiClientService svc = new XapiClientService(cfg, transport, audit);
             LtiAdvisoryOutputAggregator agg = new LtiAdvisoryOutputAggregator(svc, audit)) {
            svc.start();
            svc.poll();
            // emit a recommendation referencing the same learner to exercise advisory egress
            agg.setActiveLaunch(new LtiClientService.LaunchContext(
                    LtiPlatformBinding.fromConfig(cfg.lti().platforms().get(0)),
                    "https://canvas.example.edu", "10000000000123",
                    "alice@example.edu", "n-1",
                    "course-101", null,
                    List.of(LtiClientService.ROLE_LEARNER),
                    "1:abcdef", null, null, null));
            ContentRecommendationSignal rec =
                    new ContentRecommendationSignal("next-item", "ZPD fit", 0.72);
            agg.save(List.<IResult>of(new FakeResult(rec)), 1_700_000_000L, 7L, null);
        }
        String auditContent = Files.exists(auditFile) ? Files.readString(auditFile) : "";
        String advisoryContent = Files.exists(advisoryFile) ? Files.readString(advisoryFile) : "";
        assertFalse(auditContent.contains("alice@example.edu"),
                "S11 — raw email must not appear in audit JSONL");
        assertFalse(advisoryContent.contains("alice@example.edu"),
                "S11 — raw email must not appear in advisory JSONL");
    }

    /** §9 S12 — result.response free-text is redacted; a count is recorded in audit. */
    @Test
    void s12_freeTextRedactionIsRecordedAsCountOnly(@TempDir Path tmp) throws Exception {
        Path auditFile = tmp.resolve("audit.jsonl");
        LtiBridgeConfig cfg = baseConfig(auditFile, tmp.resolve("advisory.jsonl"));
        InMemoryXapiTransport transport = new InMemoryXapiTransport();
        transport.putStatements(
                "statements?verb=" + java.net.URLEncoder.encode(
                        XapiStatementMapper.VERB_ATTEMPTED, java.nio.charset.StandardCharsets.UTF_8),
                json.readTree("""
                        {
                          "actor": { "mbox": "mailto:alice@example.edu" },
                          "verb":  { "id": "http://adlnet.gov/expapi/verbs/attempted" },
                          "object":{ "id": "item-42" },
                          "result":{ "success": false,
                                     "response": "I think the answer is X because reasons" }
                        }
                        """));
        try (LtiAuditOutput audit = new LtiAuditOutput(auditFile);
             XapiClientService svc = new XapiClientService(cfg, transport, audit)) {
            svc.start();
            svc.poll();
            XapiResponseInput in = new XapiResponseInput("attempts", svc, List.of("ATTEMPTS"));
            List<IInputSignal> sigs = in.readSignals();
            assertEquals(1, sigs.size());
            ResponseSignal r = (ResponseSignal) sigs.get(0);
            assertEquals(null, r.getResponsePayload(),
                    "S12 — response text must be redacted before reaching the pipeline");
        }
        String content = Files.exists(auditFile) ? Files.readString(auditFile) : "";
        assertFalse(content.contains("I think the answer"),
                "S12 — sensitive response text must not appear in audit JSONL");
        assertTrue(content.contains("FREE_TEXT_REDACTED"),
                "S12 — audit must record a count of redactions, got: " + content);
    }

    /** PUSH mode — statements arrive via {@link XapiClientService#acceptStatement}. */
    @Test
    void pushModeRoutesAcceptedStatementsToBindings(@TempDir Path tmp) throws Exception {
        Path auditFile = tmp.resolve("audit.jsonl");
        LtiBridgeConfig cfg = new LtiBridgeConfig(
                null,
                new LtiBridgeConfig.XapiSection(LtiBridgeConfig.XapiMode.PUSH,
                        new LtiBridgeConfig.LrsConfig(
                                "https://lrs.example.edu/xapi", null, 60L)),
                null,
                List.of(new LtiBridgeConfig.ReadBindingConfig(
                        "ATTEMPTS",
                        XapiStatementMapper.VERB_ATTEMPTED,
                        LtiBridgeConfig.TargetSignal.RESPONSE,
                        "TUTOR.ATTEMPT")),
                List.of(),
                null,
                new LtiBridgeConfig.AuditConfig(auditFile.toString(), null),
                Map.of(),
                null);
        InMemoryXapiTransport transport = new InMemoryXapiTransport();
        try (LtiAuditOutput audit = new LtiAuditOutput(auditFile);
             XapiClientService svc = new XapiClientService(cfg, transport, audit)) {
            svc.start();
            // PULL poll should be a no-op in PUSH mode.
            svc.poll();
            svc.acceptStatement(json.readTree("""
                    {
                      "actor": { "name": "alice" },
                      "verb":  { "id": "http://adlnet.gov/expapi/verbs/attempted" },
                      "object":{ "id": "item-99" },
                      "result":{ "success": true }
                    }
                    """));
            XapiResponseInput in = new XapiResponseInput("attempts", svc, List.of("ATTEMPTS"));
            List<IInputSignal> sigs = in.readSignals();
            assertEquals(1, sigs.size());
            ResponseSignal r = (ResponseSignal) sigs.get(0);
            assertEquals("item-99", r.getItemId());
        }
    }

    /** Transport not ready → PULL poll is a no-op. */
    @Test
    void transportNotReadyIsGraceful(@TempDir Path tmp) throws Exception {
        Path auditFile = tmp.resolve("audit.jsonl");
        LtiBridgeConfig cfg = baseConfig(auditFile, tmp.resolve("advisory.jsonl"));
        InMemoryXapiTransport transport = new InMemoryXapiTransport().setReady(false);
        try (LtiAuditOutput audit = new LtiAuditOutput(auditFile);
             XapiClientService svc = new XapiClientService(cfg, transport, audit)) {
            svc.start();
            svc.poll(); // must not throw
            XapiResponseInput in = new XapiResponseInput("attempts", svc, List.of("ATTEMPTS"));
            assertTrue(in.readSignals().isEmpty());
        }
    }

    /** Recommendation egress posts an xAPI 'recommended' statement actor=tool, not learner. */
    @Test
    void recommendationActorIsTheToolNotTheLearner(@TempDir Path tmp) throws IOException {
        Path auditFile = tmp.resolve("audit.jsonl");
        Path advisoryFile = tmp.resolve("advisory.jsonl");
        LtiBridgeConfig cfg = baseConfig(auditFile, advisoryFile);
        InMemoryXapiTransport transport = new InMemoryXapiTransport();
        try (LtiAuditOutput audit = new LtiAuditOutput(auditFile);
             XapiClientService svc = new XapiClientService(cfg, transport, audit);
             LtiAdvisoryOutputAggregator agg = new LtiAdvisoryOutputAggregator(svc, audit)) {
            svc.start();
            agg.setActiveLaunch(new LtiClientService.LaunchContext(
                    LtiPlatformBinding.fromConfig(cfg.lti().platforms().get(0)),
                    "https://canvas.example.edu", "10000000000123",
                    "alice@example.edu", "n-1",
                    "course-101", null,
                    List.of(LtiClientService.ROLE_LEARNER),
                    "1:abcdef", null, null, null));
            ContentRecommendationSignal rec =
                    new ContentRecommendationSignal("next-item", "ZPD fit", 0.72);
            agg.save(List.<IResult>of(new FakeResult(rec)), 1_700_000_000L, 7L, null);
        }
        List<JsonNode> posted = transport.postedStatements();
        assertEquals(1, posted.size());
        JsonNode stmt = posted.get(0);
        // Actor must be the tool, not the learner.
        String actorName = stmt.get("actor").get("name").asText();
        assertEquals(LtiAdvisoryOutputAggregator.DEFAULT_TOOL_ACTOR, actorName);
        // Verb comes from the RECOMMENDATIONS write binding.
        assertEquals("http://activitystrea.ms/recommend", stmt.get("verb").get("id").asText());
    }
}
