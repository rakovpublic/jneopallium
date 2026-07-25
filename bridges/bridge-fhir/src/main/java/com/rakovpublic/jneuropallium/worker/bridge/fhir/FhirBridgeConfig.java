/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.fhir;

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
 * Top-level HL7 FHIR bridge configuration (06-FHIR.md §6).
 *
 * <p>The bridge ceiling is structurally <b>ADVISORY — permanently</b>
 * (06-FHIR.md §0, §3, §11). Three structural defences:
 *
 * <ol>
 *   <li>Bridge has no FHIR write API. The {@link FhirTransport} seam
 *       exposes only read/search; no {@code create()}/{@code update()}/
 *       {@code delete()} method exists in the bridge surface — a unit test
 *       (S10) verifies the absence by interface inspection.</li>
 *   <li>{@link FhirAdvisoryOutputAggregator} writes
 *       {@code TreatmentProposalSignal}s only to local JSONL audit and the
 *       configured advisory file — never to a FHIR resource (§3 rule 2).</li>
 *   <li>{@link PseudonymService} strips real patient identifiers before
 *       persistence (§3 rule 3, §9 S9, §10 R1).</li>
 * </ol>
 *
 * <p>Per §6 ({@code # perTagSafetyMode is irrelevant — bridge never writes
 * to FHIR. AUTONOMOUS is rejected at config-load.}) — this config has no
 * write bindings; an {@code AUTONOMOUS} entry in {@link #perTagSafetyMode()}
 * is rejected unconditionally because the FHIR ceiling does not admit
 * autonomous behaviour.
 */
public record FhirBridgeConfig(
        FhirEndpointConfig fhir,
        SecurityConfig security,
        CohortConfig cohort,
        List<ReadBindingConfig> reads,
        PrivacyConfig privacy,
        AuditConfig audit,
        Map<String, BridgeSafetyMode> perTagSafetyMode,
        Duration tickInterval
) {

    /** Minimum allowed poll interval — protects EHR servers (§10 R2). */
    public static final long MIN_POLL_INTERVAL_SECONDS = 15L;

    public FhirBridgeConfig {
        Objects.requireNonNull(fhir, "fhir");
        Objects.requireNonNull(audit, "audit");
        reads = reads == null ? List.of() : List.copyOf(reads);
        tickInterval = tickInterval == null ? Duration.ofMillis(500) : tickInterval;
        perTagSafetyMode = perTagSafetyMode == null ? Map.of() : Map.copyOf(perTagSafetyMode);
        privacy = privacy == null ? PrivacyConfig.defaults() : privacy;
        cohort = cohort == null ? new CohortConfig(List.of(), null) : cohort;

        Set<String> seen = new LinkedHashSet<>();
        for (ReadBindingConfig r : reads) {
            if (!seen.add(r.bindingId())) {
                throw new IllegalArgumentException("FHIR bridge: duplicate bindingId: " + r.bindingId());
            }
        }
        if (fhir.pollIntervalSeconds() < MIN_POLL_INTERVAL_SECONDS) {
            throw new IllegalArgumentException(
                    "FHIR bridge: pollIntervalSeconds=" + fhir.pollIntervalSeconds()
                            + " is below the minimum " + MIN_POLL_INTERVAL_SECONDS
                            + "s mandated by 06-FHIR.md §10 R2.");
        }
        for (Map.Entry<String, BridgeSafetyMode> e : perTagSafetyMode.entrySet()) {
            if (e.getValue() == BridgeSafetyMode.AUTONOMOUS) {
                throw new IllegalArgumentException(
                        "FHIR bridge: perTagSafetyMode entry '" + e.getKey()
                                + "' declares AUTONOMOUS, which is rejected by the FHIR ceiling "
                                + "(06-FHIR.md §3, §6 — bridge never writes to FHIR).");
            }
        }
    }

    @JsonCreator
    public static FhirBridgeConfig create(
            @JsonProperty("fhir") FhirEndpointConfig fhir,
            @JsonProperty("security") SecurityConfig security,
            @JsonProperty("cohort") CohortConfig cohort,
            @JsonProperty("reads") List<ReadBindingConfig> reads,
            @JsonProperty("privacy") PrivacyConfig privacy,
            @JsonProperty("audit") AuditConfig audit,
            @JsonProperty("perTagSafetyMode") Map<String, BridgeSafetyMode> perTagSafetyMode,
            @JsonProperty("tickInterval") Duration tickInterval) {
        return new FhirBridgeConfig(fhir, security, cohort, reads, privacy, audit,
                perTagSafetyMode, tickInterval);
    }

    /** Supported FHIR versions (06-FHIR.md §6 {@code fhirVersion}). R3 is out of scope (§10 R3). */
    public enum FhirVersion { R4, R5 }

    /** Supported authentication modes (06-FHIR.md §6 {@code security.type}). */
    public enum SecurityType {
        OAUTH2_BEARER_TOKEN,
        BASIC_AUTH,
        MUTUAL_TLS,
        /** No authentication — only valid against open test servers (e.g. {@code https://hapi.fhir.org/baseR4}). */
        NONE
    }

    /** Target Jneopallium signal kinds (06-FHIR.md §5 mapping table). */
    public enum TargetSignal {
        VITAL,
        LAB_RESULT,
        WAVEFORM,
        DEMOGRAPHIC,
        DIAGNOSIS,
        MED_ADMIN,
        ADVERSE_EVENT
    }

    /** §6 {@code fhir:} block. */
    public record FhirEndpointConfig(
            String baseUrl,
            FhirVersion fhirVersion,
            long pollIntervalSeconds
    ) {
        public FhirEndpointConfig {
            Objects.requireNonNull(baseUrl, "baseUrl");
            if (fhirVersion == null) fhirVersion = FhirVersion.R4;
            if (pollIntervalSeconds <= 0) pollIntervalSeconds = 60L;
        }

        @JsonCreator
        public static FhirEndpointConfig of(
                @JsonProperty("baseUrl") String baseUrl,
                @JsonProperty("fhirVersion") FhirVersion fhirVersion,
                @JsonProperty("pollIntervalSeconds") Long pollIntervalSeconds) {
            return new FhirEndpointConfig(baseUrl,
                    fhirVersion == null ? FhirVersion.R4 : fhirVersion,
                    pollIntervalSeconds == null ? 60L : pollIntervalSeconds);
        }
    }

    /** §6 {@code security:} block. */
    public record SecurityConfig(
            SecurityType type,
            String tokenEndpoint,
            String clientId,
            String clientSecretEnv,
            String scope,
            String basicAuthUserEnv,
            String basicAuthPassEnv,
            String trustStorePath,
            String trustStorePassEnv
    ) {
        @JsonCreator
        public static SecurityConfig of(
                @JsonProperty("type") SecurityType type,
                @JsonProperty("tokenEndpoint") String tokenEndpoint,
                @JsonProperty("clientId") String clientId,
                @JsonProperty("clientSecretEnv") String clientSecretEnv,
                @JsonProperty("scope") String scope,
                @JsonProperty("basicAuthUserEnv") String basicAuthUserEnv,
                @JsonProperty("basicAuthPassEnv") String basicAuthPassEnv,
                @JsonProperty("trustStorePath") String trustStorePath,
                @JsonProperty("trustStorePassEnv") String trustStorePassEnv) {
            return new SecurityConfig(
                    type == null ? SecurityType.NONE : type,
                    tokenEndpoint, clientId, clientSecretEnv, scope,
                    basicAuthUserEnv, basicAuthPassEnv,
                    trustStorePath, trustStorePassEnv);
        }
    }

    /** §6 {@code cohort:} block — list of patient ids and/or a search expression. */
    public record CohortConfig(
            List<String> patientIds,
            String cohortQuery
    ) {
        public CohortConfig {
            patientIds = patientIds == null ? List.of() : List.copyOf(patientIds);
        }

        @JsonCreator
        public static CohortConfig of(
                @JsonProperty("patientIds") List<String> patientIds,
                @JsonProperty("cohortQuery") String cohortQuery) {
            return new CohortConfig(patientIds, cohortQuery);
        }
    }

    /** §6 {@code reads:} entry. */
    public record ReadBindingConfig(
            String bindingId,
            String fhirSearch,
            TargetSignal targetSignal,
            String signalTag
    ) {
        public ReadBindingConfig {
            Objects.requireNonNull(bindingId, "bindingId");
            Objects.requireNonNull(fhirSearch, "fhirSearch");
            Objects.requireNonNull(targetSignal, "targetSignal");
        }

        @JsonCreator
        public static ReadBindingConfig of(
                @JsonProperty("bindingId") String bindingId,
                @JsonProperty("fhirSearch") String fhirSearch,
                @JsonProperty("targetSignal") TargetSignal targetSignal,
                @JsonProperty("signalTag") String signalTag) {
            return new ReadBindingConfig(bindingId, fhirSearch, targetSignal, signalTag);
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
