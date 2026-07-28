/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.lti;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeSafetyMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link LtiBridgeConfigLoader} tests (14-LTI-XAPI.md §6, §9 S10, §10).
 */
class LtiBridgeConfigLoaderTest {

    @Test
    void loadsMinimalConfig() throws Exception {
        String yaml = """
                lti:
                  toolName: "jneopallium-tutor"
                  platforms:
                    - issuer: "https://canvas.example.edu"
                      clientId: "10000000000123"
                      authLoginUrl: "https://canvas.example.edu/api/lti/authorize_redirect"
                      authTokenUrl: "https://canvas.example.edu/login/oauth2/token"
                      keysetUrl: "https://canvas.example.edu/api/lti/security/jwks"
                      deploymentIds: ["1:abcdef"]
                  toolPrivateKeyPath: "/etc/jneopallium/lti/tool-private.pem"
                  toolPublicJwksUrl: "https://jneo-tutor.example.edu/.well-known/jwks.json"
                xapi:
                  mode: PULL
                  lrs:
                    endpoint: "https://lrs.example.edu/xapi"
                    auth:
                      type: BASIC_AUTH
                      usernameEnv: XAPI_USER
                      passwordEnv: XAPI_PASSWORD
                    pollIntervalSeconds: 60
                cohort:
                  courseIds: ["course-101", "course-202"]
                reads:
                  - bindingId: ATTEMPTS
                    xapiVerb: "http://adlnet.gov/expapi/verbs/attempted"
                    targetSignal: RESPONSE
                    signalTagPrefix: "TUTOR.ATTEMPT"
                  - bindingId: ENGAGEMENT
                    xapiVerb: "http://adlnet.gov/expapi/verbs/interacted"
                    targetSignal: ENGAGEMENT
                    signalTagPrefix: "TUTOR.ENGAGE"
                writes:
                  - bindingId: RECOMMENDATIONS
                    xapiVerb: "http://activitystrea.ms/recommend"
                    targetActor: "Jneopallium-Tutor"
                    signalTag: "TUTOR.RECOMMEND"
                  - bindingId: PROPOSED-SCORES
                    ltiAgs: true
                    gradingProgress: "PendingManual"
                    signalTag: "TUTOR.SCORE.PROPOSAL"
                privacy:
                  pseudonymise: true
                  saltEnv: TUTOR_PSEUDO_SALT
                  redactFreeText: true
                audit:
                  localAuditFile: /var/log/jneopallium/lti-audit.jsonl
                perTagSafetyMode:
                  RECOMMENDATIONS: ADVISORY
                  PROPOSED-SCORES: ADVISORY
                """;
        LtiBridgeConfig cfg = LtiBridgeConfigLoader.load(yaml);
        assertEquals("jneopallium-tutor", cfg.lti().toolName());
        assertEquals(1, cfg.lti().platforms().size());
        assertEquals("https://canvas.example.edu", cfg.lti().platforms().get(0).issuer());
        assertEquals(LtiBridgeConfig.XapiMode.PULL, cfg.xapi().mode());
        assertEquals("https://lrs.example.edu/xapi", cfg.xapi().lrs().endpoint());
        assertEquals(2, cfg.reads().size());
        assertEquals(2, cfg.writes().size());
        assertEquals(LtiBridgeConfig.TargetSignal.RESPONSE,
                cfg.reads().get(0).targetSignal());
        assertEquals("PendingManual", cfg.writes().get(1).gradingProgress());
        assertTrue(cfg.privacy().pseudonymise());
        assertNotNull(cfg.audit().localAuditFile());
    }

    /** §9 S10 — FullyGraded must be rejected at config load. */
    @Test
    void fullyGradedIsRejected() {
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
        String msg = causeMessage(() -> LtiBridgeConfigLoader.load(yaml));
        assertTrue(msg.contains("PendingManual") || msg.contains("FullyGraded"),
                "loader must mention PendingManual / FullyGraded, got: " + msg);
    }

    /** §6 — AUTONOMOUS rejected by validator. */
    @Test
    void autonomousModeIsRejected() {
        String yaml = """
                xapi:
                  mode: PULL
                  lrs:
                    endpoint: "https://lrs.example.edu/xapi"
                    pollIntervalSeconds: 60
                audit:
                  localAuditFile: /tmp/x.jsonl
                perTagSafetyMode:
                  ANY: AUTONOMOUS
                """;
        String msg = causeMessage(() -> LtiBridgeConfigLoader.load(yaml));
        assertTrue(msg.contains("AUTONOMOUS"),
                "loader must mention AUTONOMOUS, got: " + msg);
    }

    /** 00-FRAMEWORK §3 — unknown YAML keys fail at load. */
    @Test
    void unknownKeyIsRejected() {
        String yaml = """
                xapi:
                  mode: PULL
                  lrs:
                    endpoint: "https://lrs.example.edu/xapi"
                    pollIntervalSeconds: 60
                audit:
                  localAuditFile: /tmp/x.jsonl
                bogusKey: 42
                """;
        assertThrows(UnrecognizedPropertyException.class,
                () -> LtiBridgeConfigLoader.load(yaml));
    }

    /** Poll interval must not run below the 15-second floor. */
    @Test
    void pollIntervalBelowFloorIsRejected() {
        String yaml = """
                xapi:
                  mode: PULL
                  lrs:
                    endpoint: "https://lrs.example.edu/xapi"
                    pollIntervalSeconds: 5
                audit:
                  localAuditFile: /tmp/x.jsonl
                """;
        String msg = causeMessage(() -> LtiBridgeConfigLoader.load(yaml));
        assertTrue(msg.contains("pollIntervalSeconds"),
                "loader must mention pollIntervalSeconds, got: " + msg);
    }

    /** Duplicate read or write binding ids are rejected. */
    @Test
    void duplicateBindingIdIsRejected() {
        String yaml = """
                xapi:
                  mode: PULL
                  lrs:
                    endpoint: "https://lrs.example.edu/xapi"
                    pollIntervalSeconds: 60
                audit:
                  localAuditFile: /tmp/x.jsonl
                reads:
                  - bindingId: DUP
                    xapiVerb: "http://adlnet.gov/expapi/verbs/attempted"
                    targetSignal: RESPONSE
                  - bindingId: DUP
                    xapiVerb: "http://adlnet.gov/expapi/verbs/interacted"
                    targetSignal: ENGAGEMENT
                """;
        String msg = causeMessage(() -> LtiBridgeConfigLoader.load(yaml));
        assertTrue(msg.contains("duplicate"),
                "loader must mention duplicate, got: " + msg);
    }

    @Test
    void shadowAndAdvisoryAccepted() throws Exception {
        String yaml = """
                xapi:
                  mode: PULL
                  lrs:
                    endpoint: "https://lrs.example.edu/xapi"
                    pollIntervalSeconds: 60
                audit:
                  localAuditFile: /tmp/x.jsonl
                perTagSafetyMode:
                  R: ADVISORY
                  S: SHADOW
                """;
        LtiBridgeConfig cfg = LtiBridgeConfigLoader.load(yaml);
        assertEquals(BridgeSafetyMode.ADVISORY, cfg.perTagSafetyMode().get("R"));
        assertEquals(BridgeSafetyMode.SHADOW, cfg.perTagSafetyMode().get("S"));
    }

    private static String causeMessage(org.junit.jupiter.api.function.Executable e) {
        Throwable t = assertThrows(Throwable.class, e);
        while (t.getCause() != null && t.getCause() != t) t = t.getCause();
        return t.getMessage() == null ? "" : t.getMessage();
    }
}
