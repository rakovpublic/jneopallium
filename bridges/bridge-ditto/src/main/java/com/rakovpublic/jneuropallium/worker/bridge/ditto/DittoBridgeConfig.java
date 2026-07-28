/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.ditto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeSafetyMode;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Top-level configuration for the Eclipse Ditto digital-twin bridge
 * (10-DITTO.md §5). Loaded from YAML via {@link DittoBridgeConfigLoader};
 * immutable.
 *
 * <p>Per 00-FRAMEWORK §3 unknown YAML keys fail loading. Per 10-DITTO §1 the
 * structural ceiling is {@code ADVISORY}; the compact constructor rejects
 * per-tag {@code AUTONOMOUS} promotion. The egress feature-prefix rule
 * ({@code recommended_*} / {@code advisory_*} only) is enforced both here
 * (per binding) and in {@link DittoAdvisoryOutputAggregator} at runtime
 * (defence-in-depth, §4).
 */
public record DittoBridgeConfig(
        ConnectionConfig connection,
        AuthConfig authentication,
        List<String> things,
        List<ReadBindingConfig> reads,
        List<WriteBindingConfig> writes,
        AuditConfig audit,
        Map<String, BridgeSafetyMode> perTagSafetyMode,
        Map<String, AlarmPriorityName> severityMap,
        Duration tickInterval
) {
    /** Allowed prefixes for any write binding's {@code feature} (10-DITTO §4). */
    public static final String ADVISORY_PREFIX = "advisory_";
    public static final String RECOMMENDED_PREFIX = "recommended_";

    public DittoBridgeConfig {
        Objects.requireNonNull(connection, "connection");
        things = things == null ? List.of() : List.copyOf(things);
        reads = reads == null ? List.of() : List.copyOf(reads);
        writes = writes == null ? List.of() : List.copyOf(writes);
        if (perTagSafetyMode == null) {
            perTagSafetyMode = Map.of();
        } else {
            Map<String, BridgeSafetyMode> linked = new LinkedHashMap<>(perTagSafetyMode);
            for (Map.Entry<String, BridgeSafetyMode> e : linked.entrySet()) {
                if (e.getValue() == BridgeSafetyMode.AUTONOMOUS) {
                    throw new IllegalArgumentException(
                            "Eclipse Ditto bridge ceiling is ADVISORY (10-DITTO.md §1): "
                                    + "tag '" + e.getKey() + "' was promoted to AUTONOMOUS — refusing to load");
                }
            }
            perTagSafetyMode = Map.copyOf(linked);
        }
        severityMap = severityMap == null ? Map.of() : Map.copyOf(severityMap);
        for (WriteBindingConfig w : writes) {
            requireAdvisoryPrefix(w.feature(), w.bindingId());
        }
    }

    /** Throw when {@code feature} is not an advisory/recommended feature (10-DITTO §4). */
    public static void requireAdvisoryPrefix(String feature, String bindingId) {
        if (feature == null || !(feature.startsWith(ADVISORY_PREFIX)
                || feature.startsWith(RECOMMENDED_PREFIX))) {
            throw new IllegalArgumentException(
                    "Ditto write binding '" + bindingId
                            + "' targets feature '" + feature + "' which is not an advisory feature: "
                            + "only feature names starting with '" + RECOMMENDED_PREFIX
                            + "' or '" + ADVISORY_PREFIX + "' may receive advisory writes (10-DITTO §4)");
        }
    }

    /** Connection-level settings. */
    public record ConnectionConfig(
            String baseUrl,
            String webSocketPath,
            String httpPath,
            Duration requestTimeout,
            int advisoryQueueSize
    ) {
        public ConnectionConfig {
            Objects.requireNonNull(baseUrl, "baseUrl");
            webSocketPath = webSocketPath == null || webSocketPath.isBlank()
                    ? "/ws/2" : webSocketPath;
            httpPath = httpPath == null || httpPath.isBlank()
                    ? "/api/2" : httpPath;
            requestTimeout = requestTimeout == null ? Duration.ofSeconds(10) : requestTimeout;
            if (advisoryQueueSize <= 0) advisoryQueueSize = 10_000;
        }

        @JsonCreator
        public static ConnectionConfig of(
                @JsonProperty("baseUrl") String baseUrl,
                @JsonProperty("webSocketPath") String webSocketPath,
                @JsonProperty("httpPath") String httpPath,
                @JsonProperty("requestTimeout") Duration requestTimeout,
                @JsonProperty("advisoryQueueSize") Integer advisoryQueueSize) {
            return new ConnectionConfig(baseUrl, webSocketPath, httpPath, requestTimeout,
                    advisoryQueueSize == null ? 10_000 : advisoryQueueSize);
        }
    }

    /**
     * Authentication settings (10-DITTO §5). Secret material is never
     * embedded — env-var names are stored, the connection layer reads them
     * at start-up.
     */
    public record AuthConfig(
            AuthType type,
            String username,
            String passwordEnv,
            String tokenEndpoint,
            String clientId,
            String clientSecretEnv
    ) {
        @JsonCreator
        public static AuthConfig of(
                @JsonProperty("type") AuthType type,
                @JsonProperty("username") String username,
                @JsonProperty("passwordEnv") String passwordEnv,
                @JsonProperty("tokenEndpoint") String tokenEndpoint,
                @JsonProperty("clientId") String clientId,
                @JsonProperty("clientSecretEnv") String clientSecretEnv) {
            return new AuthConfig(type, username, passwordEnv,
                    tokenEndpoint, clientId, clientSecretEnv);
        }
    }

    public enum AuthType { None, BasicAuth, OAuth2BearerToken }

    /**
     * One ingress binding from a Ditto thing/feature/property to a
     * Jneopallium signal (10-DITTO §4 ingress table).
     */
    public record ReadBindingConfig(
            String bindingId,
            String thingId,
            String feature,
            String property,
            String signalTag,
            ReadSignalKind signalKind
    ) {
        public ReadBindingConfig {
            Objects.requireNonNull(bindingId, "bindingId");
            Objects.requireNonNull(thingId, "thingId");
            Objects.requireNonNull(feature, "feature");
            Objects.requireNonNull(property, "property");
            signalKind = signalKind == null ? ReadSignalKind.MEASUREMENT : signalKind;
        }

        @JsonCreator
        public static ReadBindingConfig of(
                @JsonProperty("bindingId") String bindingId,
                @JsonProperty("thingId") String thingId,
                @JsonProperty("feature") String feature,
                @JsonProperty("property") String property,
                @JsonProperty("signalTag") String signalTag,
                @JsonProperty("signalKind") ReadSignalKind signalKind) {
            return new ReadBindingConfig(bindingId, thingId, feature, property,
                    signalTag, signalKind);
        }
    }

    /** What signal class a {@link ReadBindingConfig} produces. */
    public enum ReadSignalKind { MEASUREMENT, ALARM }

    /**
     * One advisory egress binding (10-DITTO §4 egress table). The
     * compact constructor rejects any non-advisory {@code feature}.
     */
    public record WriteBindingConfig(
            String bindingId,
            String thingId,
            String feature,
            String property,
            String signalTag,
            Double minClampValue,
            Double maxClampValue
    ) {
        public WriteBindingConfig {
            Objects.requireNonNull(bindingId, "bindingId");
            Objects.requireNonNull(thingId, "thingId");
            Objects.requireNonNull(feature, "feature");
            Objects.requireNonNull(property, "property");
            requireAdvisoryPrefix(feature, bindingId);
        }

        @JsonCreator
        public static WriteBindingConfig of(
                @JsonProperty("bindingId") String bindingId,
                @JsonProperty("thingId") String thingId,
                @JsonProperty("feature") String feature,
                @JsonProperty("property") String property,
                @JsonProperty("signalTag") String signalTag,
                @JsonProperty("minClampValue") Double minClampValue,
                @JsonProperty("maxClampValue") Double maxClampValue) {
            return new WriteBindingConfig(bindingId, thingId, feature, property,
                    signalTag, minClampValue, maxClampValue);
        }
    }

    /** Audit channel configuration (00-FRAMEWORK §3, §4). */
    public record AuditConfig(
            String localAuditFile
    ) {
        public AuditConfig {
            Objects.requireNonNull(localAuditFile, "localAuditFile");
        }

        @JsonCreator
        public static AuditConfig of(
                @JsonProperty("localAuditFile") String localAuditFile) {
            return new AuditConfig(localAuditFile);
        }
    }

    /** Stable name for the AlarmPriority enum so it can appear in YAML config. */
    public enum AlarmPriorityName { JOURNAL, LOW, HIGH, URGENT }
}
