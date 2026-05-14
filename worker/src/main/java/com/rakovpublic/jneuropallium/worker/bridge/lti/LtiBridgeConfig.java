/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.lti;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeSafetyMode;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Top-level LTI / xAPI bridge configuration (14-LTI-XAPI.md §6).
 *
 * <p>The bridge ceiling is structurally <b>ADVISORY</b> (14-LTI-XAPI.md §0,
 * §3, §11). Key structural defences:
 *
 * <ol>
 *   <li>Egress to an LMS never sets {@code gradingProgress=FullyGraded}.
 *       The loader rejects that value unconditionally (§9 S10).</li>
 *   <li>Egress to an LMS never declares {@code AUTONOMOUS} safety mode.
 *       The loader rejects {@code AUTONOMOUS} unconditionally.</li>
 *   <li>The bridge has no auto-enrolment / auto-roster / auto-grade API
 *       on its egress surface; only xAPI {@code recommended} statements
 *       and AGS {@code Score} proposals with {@code PendingManual}.</li>
 * </ol>
 */
public record LtiBridgeConfig(
        LtiSection lti,
        XapiSection xapi,
        CohortConfig cohort,
        List<ReadBindingConfig> reads,
        List<WriteBindingConfig> writes,
        PrivacyConfig privacy,
        AuditConfig audit,
        Map<String, BridgeSafetyMode> perTagSafetyMode,
        Duration tickInterval
) {

    /** Minimum poll interval — protects LRS endpoints (mirrors §10 R2 of the FHIR bridge). */
    public static final long MIN_POLL_INTERVAL_SECONDS = 15L;

    public LtiBridgeConfig {
        Objects.requireNonNull(audit, "audit");
        reads = reads == null ? List.of() : List.copyOf(reads);
        writes = writes == null ? List.of() : List.copyOf(writes);
        tickInterval = tickInterval == null ? Duration.ofMillis(500) : tickInterval;
        perTagSafetyMode = perTagSafetyMode == null ? Map.of() : Map.copyOf(perTagSafetyMode);
        privacy = privacy == null ? PrivacyConfig.defaults() : privacy;
        cohort = cohort == null ? new CohortConfig(List.of()) : cohort;

        Set<String> seenReads = new LinkedHashSet<>();
        for (ReadBindingConfig r : reads) {
            if (!seenReads.add(r.bindingId())) {
                throw new IllegalArgumentException(
                        "LTI bridge: duplicate read bindingId: " + r.bindingId());
            }
        }
        Set<String> seenWrites = new LinkedHashSet<>();
        for (WriteBindingConfig w : writes) {
            if (!seenWrites.add(w.bindingId())) {
                throw new IllegalArgumentException(
                        "LTI bridge: duplicate write bindingId: " + w.bindingId());
            }
            if (w.ltiAgs() && w.gradingProgress() != null
                    && !GradingProgress.PENDING_MANUAL.equals(w.gradingProgress())) {
                throw new IllegalArgumentException(
                        "LTI bridge: write bindingId '" + w.bindingId()
                                + "' declares gradingProgress=" + w.gradingProgress()
                                + " — 14-LTI-XAPI.md §9 S10 only permits PendingManual.");
            }
        }
        if (xapi != null && xapi.lrs() != null
                && xapi.lrs().pollIntervalSeconds() < MIN_POLL_INTERVAL_SECONDS) {
            throw new IllegalArgumentException(
                    "LTI bridge: xapi.lrs.pollIntervalSeconds="
                            + xapi.lrs().pollIntervalSeconds()
                            + " is below the minimum " + MIN_POLL_INTERVAL_SECONDS + "s.");
        }
        for (Map.Entry<String, BridgeSafetyMode> e : perTagSafetyMode.entrySet()) {
            if (e.getValue() == BridgeSafetyMode.AUTONOMOUS) {
                throw new IllegalArgumentException(
                        "LTI bridge: perTagSafetyMode entry '" + e.getKey()
                                + "' declares AUTONOMOUS — rejected by the LTI/xAPI "
                                + "advisory ceiling (14-LTI-XAPI.md §3, §6).");
            }
        }
    }

    @JsonCreator
    public static LtiBridgeConfig create(
            @JsonProperty("lti") LtiSection lti,
            @JsonProperty("xapi") XapiSection xapi,
            @JsonProperty("cohort") CohortConfig cohort,
            @JsonProperty("reads") List<ReadBindingConfig> reads,
            @JsonProperty("writes") List<WriteBindingConfig> writes,
            @JsonProperty("privacy") PrivacyConfig privacy,
            @JsonProperty("audit") AuditConfig audit,
            @JsonProperty("perTagSafetyMode") Map<String, BridgeSafetyMode> perTagSafetyMode,
            @JsonProperty("tickInterval") Duration tickInterval) {
        return new LtiBridgeConfig(lti, xapi, cohort, reads, writes,
                privacy, audit, perTagSafetyMode, tickInterval);
    }

    /** xAPI ingestion mode (§6 — bridge polls an LRS, or accepts statements as an endpoint). */
    public enum XapiMode { PULL, PUSH }

    /** AGS gradingProgress vocabulary surfaced in config. */
    public static final class GradingProgress {
        private GradingProgress() {}
        /** The only value the bridge will accept (§5 egress table, §9 S10). */
        public static final String PENDING_MANUAL = "PendingManual";
        public static final String FULLY_GRADED = "FullyGraded";
    }

    /** Target Jneopallium signal for an xAPI read binding (§5 signal mapping). */
    public enum TargetSignal {
        RESPONSE,
        MASTERY,
        ENGAGEMENT,
        AFFECT
    }

    /** §6 {@code lti:} block — tool registration metadata. */
    public record LtiSection(
            String toolName,
            List<PlatformConfig> platforms,
            String toolPrivateKeyPath,
            String toolPublicJwksUrl
    ) {
        public LtiSection {
            platforms = platforms == null ? List.of() : List.copyOf(platforms);
        }

        @JsonCreator
        public static LtiSection of(
                @JsonProperty("toolName") String toolName,
                @JsonProperty("platforms") List<PlatformConfig> platforms,
                @JsonProperty("toolPrivateKeyPath") String toolPrivateKeyPath,
                @JsonProperty("toolPublicJwksUrl") String toolPublicJwksUrl) {
            return new LtiSection(toolName, platforms, toolPrivateKeyPath, toolPublicJwksUrl);
        }
    }

    /** §6 — one LMS platform registered against the tool. */
    public record PlatformConfig(
            String issuer,
            String clientId,
            String authLoginUrl,
            String authTokenUrl,
            String keysetUrl,
            List<String> deploymentIds
    ) {
        public PlatformConfig {
            Objects.requireNonNull(issuer, "issuer");
            Objects.requireNonNull(clientId, "clientId");
            deploymentIds = deploymentIds == null ? List.of() : List.copyOf(deploymentIds);
        }

        @JsonCreator
        public static PlatformConfig of(
                @JsonProperty("issuer") String issuer,
                @JsonProperty("clientId") String clientId,
                @JsonProperty("authLoginUrl") String authLoginUrl,
                @JsonProperty("authTokenUrl") String authTokenUrl,
                @JsonProperty("keysetUrl") String keysetUrl,
                @JsonProperty("deploymentIds") List<String> deploymentIds) {
            return new PlatformConfig(issuer, clientId, authLoginUrl, authTokenUrl,
                    keysetUrl, deploymentIds);
        }
    }

    /** §6 {@code xapi:} block. */
    public record XapiSection(
            XapiMode mode,
            LrsConfig lrs
    ) {
        @JsonCreator
        public static XapiSection of(
                @JsonProperty("mode") XapiMode mode,
                @JsonProperty("lrs") LrsConfig lrs) {
            return new XapiSection(mode == null ? XapiMode.PULL : mode, lrs);
        }
    }

    /** §6 — LRS endpoint + auth. */
    public record LrsConfig(
            String endpoint,
            AuthConfig auth,
            long pollIntervalSeconds
    ) {
        public LrsConfig {
            Objects.requireNonNull(endpoint, "endpoint");
            if (pollIntervalSeconds <= 0) pollIntervalSeconds = 60L;
        }

        @JsonCreator
        public static LrsConfig of(
                @JsonProperty("endpoint") String endpoint,
                @JsonProperty("auth") AuthConfig auth,
                @JsonProperty("pollIntervalSeconds") Long pollIntervalSeconds) {
            return new LrsConfig(endpoint, auth,
                    pollIntervalSeconds == null ? 60L : pollIntervalSeconds);
        }
    }

    /** §6 — LRS auth (BasicAuth, OAuth2 bearer, or none). */
    public record AuthConfig(
            AuthType type,
            String usernameEnv,
            String passwordEnv,
            String tokenEnv
    ) {
        public AuthConfig {
            if (type == null) type = AuthType.NONE;
        }

        @JsonCreator
        public static AuthConfig of(
                @JsonProperty("type") AuthType type,
                @JsonProperty("usernameEnv") String usernameEnv,
                @JsonProperty("passwordEnv") String passwordEnv,
                @JsonProperty("tokenEnv") String tokenEnv) {
            return new AuthConfig(type == null ? AuthType.NONE : type,
                    usernameEnv, passwordEnv, tokenEnv);
        }
    }

    public enum AuthType { NONE, BASIC_AUTH, BEARER_TOKEN }

    /** §6 {@code cohort:} block. */
    public record CohortConfig(List<String> courseIds) {
        public CohortConfig {
            courseIds = courseIds == null ? List.of() : List.copyOf(courseIds);
        }

        @JsonCreator
        public static CohortConfig of(@JsonProperty("courseIds") List<String> courseIds) {
            return new CohortConfig(courseIds);
        }
    }

    /** §6 {@code reads:} entry. */
    public record ReadBindingConfig(
            String bindingId,
            String xapiVerb,
            TargetSignal targetSignal,
            String signalTagPrefix
    ) {
        public ReadBindingConfig {
            Objects.requireNonNull(bindingId, "bindingId");
            Objects.requireNonNull(xapiVerb, "xapiVerb");
            Objects.requireNonNull(targetSignal, "targetSignal");
        }

        @JsonCreator
        public static ReadBindingConfig of(
                @JsonProperty("bindingId") String bindingId,
                @JsonProperty("xapiVerb") String xapiVerb,
                @JsonProperty("targetSignal") TargetSignal targetSignal,
                @JsonProperty("signalTagPrefix") String signalTagPrefix) {
            return new ReadBindingConfig(bindingId, xapiVerb, targetSignal, signalTagPrefix);
        }
    }

    /** §6 {@code writes:} entry. */
    public record WriteBindingConfig(
            String bindingId,
            String xapiVerb,
            String targetActor,
            boolean ltiAgs,
            String gradingProgress,
            String signalTag
    ) {
        public WriteBindingConfig {
            Objects.requireNonNull(bindingId, "bindingId");
        }

        @JsonCreator
        public static WriteBindingConfig of(
                @JsonProperty("bindingId") String bindingId,
                @JsonProperty("xapiVerb") String xapiVerb,
                @JsonProperty("targetActor") String targetActor,
                @JsonProperty("ltiAgs") Boolean ltiAgs,
                @JsonProperty("gradingProgress") String gradingProgress,
                @JsonProperty("signalTag") String signalTag) {
            return new WriteBindingConfig(bindingId, xapiVerb, targetActor,
                    ltiAgs != null && ltiAgs, gradingProgress, signalTag);
        }
    }

    /** §6 {@code privacy:} block. */
    public record PrivacyConfig(
            boolean pseudonymise,
            String saltEnv,
            boolean redactFreeText
    ) {
        public static PrivacyConfig defaults() {
            return new PrivacyConfig(true, null, true);
        }

        @JsonCreator
        public static PrivacyConfig of(
                @JsonProperty("pseudonymise") Boolean pseudonymise,
                @JsonProperty("saltEnv") String saltEnv,
                @JsonProperty("redactFreeText") Boolean redactFreeText) {
            return new PrivacyConfig(
                    pseudonymise == null || pseudonymise,
                    saltEnv,
                    redactFreeText == null || redactFreeText);
        }
    }

    /** §6 {@code audit:} block. */
    public record AuditConfig(
            String localAuditFile,
            String advisoryFile
    ) {
        public AuditConfig {
            Objects.requireNonNull(localAuditFile, "localAuditFile");
        }

        @JsonCreator
        public static AuditConfig of(
                @JsonProperty("localAuditFile") String localAuditFile,
                @JsonProperty("advisoryFile") String advisoryFile) {
            return new AuditConfig(localAuditFile, advisoryFile);
        }
    }
}
