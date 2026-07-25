/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.dicom;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Top-level DICOM bridge configuration (07-DICOM.md §6).
 *
 * <p>The bridge ceiling is structurally <b>READ-ONLY — permanently</b>
 * (07-DICOM.md §0, §3, §4 diagram, §8 phase 3). The structural defences
 * are:
 *
 * <ol>
 *   <li>The bridge package contains no aggregator or output class
 *       (07-DICOM.md §7 "No aggregator class — there is no write path"),
 *       so there is no surface a higher layer could call to push data
 *       back to the PACS.</li>
 *   <li>The {@link DicomwebTransport} seam exposes only {@code qido()} and
 *       {@code wadoMetadata()} — both HTTP {@code GET}. No method accepts
 *       an HTTP verb argument or a request body, so a write code path
 *       cannot be expressed within the bridge.</li>
 *   <li>{@code writes:} blocks are rejected at config-load with a clear
 *       message (§6 "{@code writes:} block is <b>rejected at config-load</b>
 *       with a clear message — there is no write surface").</li>
 * </ol>
 */
public record DicomBridgeConfig(
        Mode mode,
        DimseConfig dimse,
        DicomwebConfig dicomweb,
        SecurityConfig security,
        List<StudyReadConfig> reads,
        PrivacyConfig privacy,
        AuditConfig audit,
        Duration tickInterval
) {

    /** Minimum allowed poll interval (protects PACS / DIMSE servers). */
    public static final long MIN_POLL_INTERVAL_SECONDS = 30L;

    public DicomBridgeConfig {
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(audit, "audit");
        reads = reads == null ? List.of() : List.copyOf(reads);
        tickInterval = tickInterval == null ? Duration.ofMillis(1000) : tickInterval;
        privacy = privacy == null ? PrivacyConfig.defaults() : privacy;

        if (mode == Mode.DICOMWEB && dicomweb == null) {
            throw new IllegalArgumentException(
                    "DICOM bridge: mode=DICOMWEB requires a 'dicomweb:' block.");
        }
        if (mode == Mode.DIMSE && dimse == null) {
            throw new IllegalArgumentException(
                    "DICOM bridge: mode=DIMSE requires a 'dimse:' block.");
        }

        Set<String> seen = new LinkedHashSet<>();
        for (StudyReadConfig r : reads) {
            if (!seen.add(r.bindingId())) {
                throw new IllegalArgumentException(
                        "DICOM bridge: duplicate bindingId: " + r.bindingId());
            }
        }
    }

    @JsonCreator
    public static DicomBridgeConfig create(
            @JsonProperty("mode") Mode mode,
            @JsonProperty("dimse") DimseConfig dimse,
            @JsonProperty("dicomweb") DicomwebConfig dicomweb,
            @JsonProperty("security") SecurityConfig security,
            @JsonProperty("reads") List<StudyReadConfig> reads,
            @JsonProperty("privacy") PrivacyConfig privacy,
            @JsonProperty("audit") AuditConfig audit,
            @JsonProperty("tickInterval") Duration tickInterval,
            // §6 — reject a writes: block at load time. Declared so Jackson surfaces it
            // with FAIL_ON_UNKNOWN_PROPERTIES=true and rejects with a clear message.
            @JsonProperty("writes") Object writes) {
        if (writes != null) {
            throw new IllegalArgumentException(
                    "DICOM bridge: 'writes:' block is not permitted — the bridge ceiling "
                            + "is READ-ONLY (07-DICOM.md §0, §3, §6). Remove the writes section.");
        }
        return new DicomBridgeConfig(mode, dimse, dicomweb, security, reads,
                privacy, audit, tickInterval);
    }

    /** Ingress mode (§4 architecture). */
    public enum Mode {
        /** DICOMweb REST (QIDO-RS / WADO-RS). */
        DICOMWEB,
        /** Classic DIMSE network protocol (C-FIND / C-MOVE / C-GET). */
        DIMSE
    }

    /** Supported authentication modes (§6 {@code security.type}). */
    public enum SecurityType {
        OAUTH2_BEARER_TOKEN,
        BASIC_AUTH,
        MUTUAL_TLS,
        /** No authentication — only valid against open test instances (e.g. Orthanc demo). */
        NONE
    }

    /** Target Jneopallium signal kind. Currently only one is supported (§5). */
    public enum TargetSignal {
        IMAGING_FINDING
    }

    /** §6 {@code dimse:} block. Only used when {@link Mode#DIMSE}. */
    public record DimseConfig(
            String callingAet,
            String calledAet,
            String host,
            int port,
            boolean requireTls
    ) {
        public DimseConfig {
            Objects.requireNonNull(callingAet, "callingAet");
            Objects.requireNonNull(calledAet, "calledAet");
            Objects.requireNonNull(host, "host");
            if (port <= 0 || port > 65535) {
                throw new IllegalArgumentException("DICOM bridge: dimse.port out of range: " + port);
            }
        }

        @JsonCreator
        public static DimseConfig of(
                @JsonProperty("callingAet") String callingAet,
                @JsonProperty("calledAet") String calledAet,
                @JsonProperty("host") String host,
                @JsonProperty("port") int port,
                @JsonProperty("requireTls") Boolean requireTls) {
            return new DimseConfig(callingAet, calledAet, host, port,
                    requireTls != null && requireTls);
        }
    }

    /** §6 {@code dicomweb:} block. Only used when {@link Mode#DICOMWEB}. */
    public record DicomwebConfig(
            String baseUrl,
            String qidoEndpoint,
            String wadoEndpoint,
            long pollIntervalSeconds
    ) {
        public DicomwebConfig {
            Objects.requireNonNull(baseUrl, "baseUrl");
            if (qidoEndpoint == null) qidoEndpoint = "/studies";
            if (wadoEndpoint == null) wadoEndpoint =
                    "/studies/{study}/series/{series}/instances/{instance}/metadata";
            if (pollIntervalSeconds <= 0) pollIntervalSeconds = 60L;
            if (pollIntervalSeconds < MIN_POLL_INTERVAL_SECONDS) {
                throw new IllegalArgumentException(
                        "DICOM bridge: pollIntervalSeconds=" + pollIntervalSeconds
                                + " is below the minimum " + MIN_POLL_INTERVAL_SECONDS
                                + "s mandated to protect the PACS (07-DICOM.md §10 R2).");
            }
        }

        @JsonCreator
        public static DicomwebConfig of(
                @JsonProperty("baseUrl") String baseUrl,
                @JsonProperty("qidoEndpoint") String qidoEndpoint,
                @JsonProperty("wadoEndpoint") String wadoEndpoint,
                @JsonProperty("pollIntervalSeconds") Long pollIntervalSeconds) {
            return new DicomwebConfig(baseUrl, qidoEndpoint, wadoEndpoint,
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

    /** §6 {@code reads[].studyFilter:} block. */
    public record StudyFilterConfig(
            String modality,
            String accessionPattern,
            Integer windowHours
    ) {
        @JsonCreator
        public static StudyFilterConfig of(
                @JsonProperty("modality") String modality,
                @JsonProperty("accessionPattern") String accessionPattern,
                @JsonProperty("windowHours") Integer windowHours) {
            return new StudyFilterConfig(modality, accessionPattern, windowHours);
        }
    }

    /** §6 {@code reads:} entry. */
    public record StudyReadConfig(
            String bindingId,
            StudyFilterConfig studyFilter,
            TargetSignal targetSignal,
            String signalTagPrefix
    ) {
        public StudyReadConfig {
            Objects.requireNonNull(bindingId, "bindingId");
            if (targetSignal == null) targetSignal = TargetSignal.IMAGING_FINDING;
        }

        @JsonCreator
        public static StudyReadConfig of(
                @JsonProperty("bindingId") String bindingId,
                @JsonProperty("studyFilter") StudyFilterConfig studyFilter,
                @JsonProperty("targetSignal") TargetSignal targetSignal,
                @JsonProperty("signalTagPrefix") String signalTagPrefix) {
            return new StudyReadConfig(bindingId, studyFilter, targetSignal, signalTagPrefix);
        }
    }

    /** §6 {@code privacy:} block. */
    public record PrivacyConfig(
            boolean pseudonymise,
            String saltEnv,
            boolean redactPatientName,
            boolean redactInstitution
    ) {
        public static PrivacyConfig defaults() {
            return new PrivacyConfig(true, null, true, false);
        }

        @JsonCreator
        public static PrivacyConfig of(
                @JsonProperty("pseudonymise") Boolean pseudonymise,
                @JsonProperty("saltEnv") String saltEnv,
                @JsonProperty("redactPatientName") Boolean redactPatientName,
                @JsonProperty("redactInstitution") Boolean redactInstitution) {
            return new PrivacyConfig(
                    pseudonymise == null || pseudonymise,
                    saltEnv,
                    redactPatientName == null || redactPatientName,
                    redactInstitution != null && redactInstitution);
        }
    }

    /** §6 {@code audit:} block. */
    public record AuditConfig(String localAuditFile) {
        public AuditConfig {
            Objects.requireNonNull(localAuditFile, "localAuditFile");
        }

        @JsonCreator
        public static AuditConfig of(@JsonProperty("localAuditFile") String localAuditFile) {
            return new AuditConfig(localAuditFile);
        }
    }
}
