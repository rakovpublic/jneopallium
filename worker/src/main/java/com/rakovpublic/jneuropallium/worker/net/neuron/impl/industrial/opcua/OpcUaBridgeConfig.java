/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.opcua;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.SafetyMode;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Top-level configuration for an OPC UA bridge instance.
 *
 * <p>Loaded from YAML at startup; immutable. Hot-reloading is deliberately
 * unsupported — config changes require a controlled restart of the bridge,
 * which is the expected industrial workflow (Management of Change).
 */
public record OpcUaBridgeConfig(
        ConnectionConfig connection,
        SecurityConfig security,
        List<NodeBindingConfig> reads,
        List<NodeBindingConfig> writes,
        List<NodeBindingConfig> alarms,
        AuditConfig audit,
        Map<String, SafetyMode> perLoopSafetyMode,
        Duration tickInterval
) {
    public OpcUaBridgeConfig {
        Objects.requireNonNull(connection, "connection");
        Objects.requireNonNull(security, "security");
        reads = reads == null ? List.of() : List.copyOf(reads);
        writes = writes == null ? List.of() : List.copyOf(writes);
        alarms = alarms == null ? List.of() : List.copyOf(alarms);
        perLoopSafetyMode = perLoopSafetyMode == null ? Map.of() : Map.copyOf(perLoopSafetyMode);
        tickInterval = tickInterval == null ? Duration.ofMillis(250) : tickInterval;
    }

    public record ConnectionConfig(
            String endpointUrl,
            String applicationName,
            String applicationUri,
            Duration requestTimeout,
            Duration sessionTimeout,
            int keepAliveFailuresAllowed
    ) {
        public ConnectionConfig {
            Objects.requireNonNull(endpointUrl, "endpointUrl");
            applicationName = applicationName == null ? "Jneopallium-OPCUA-Bridge" : applicationName;
            applicationUri = applicationUri == null ? "urn:rakovpublic:jneopallium:bridge" : applicationUri;
            requestTimeout = requestTimeout == null ? Duration.ofSeconds(5) : requestTimeout;
            sessionTimeout = sessionTimeout == null ? Duration.ofMinutes(2) : sessionTimeout;
            if (keepAliveFailuresAllowed < 0) keepAliveFailuresAllowed = 3;
        }
    }

    public record SecurityConfig(
            SecurityPolicy policy,
            MessageSecurityMode mode,
            String pkiDir,
            String certAlias,
            String certPassword,
            Authentication auth
    ) {
        public SecurityConfig {
            policy = policy == null ? SecurityPolicy.NONE : policy;
            mode = mode == null ? MessageSecurityMode.NONE : mode;
            auth = auth == null ? new Anonymous() : auth;
        }

        public enum SecurityPolicy { NONE, BASIC256SHA256, AES128_SHA256_RSAOAEP, AES256_SHA256_RSAPSS }
        public enum MessageSecurityMode { NONE, SIGN, SIGN_AND_ENCRYPT }

        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
        @JsonSubTypes({
                @JsonSubTypes.Type(value = Anonymous.class, name = "Anonymous"),
                @JsonSubTypes.Type(value = UsernamePassword.class, name = "UsernamePassword"),
                @JsonSubTypes.Type(value = X509.class, name = "X509")
        })
        public sealed interface Authentication permits Anonymous, UsernamePassword, X509 {}

        public record Anonymous() implements Authentication {}
        public record UsernamePassword(String username, String passwordEnv) implements Authentication {}
        public record X509(String certAlias) implements Authentication {}
    }

    public record NodeBindingConfig(
            String loopId,
            String nodeId,
            String signalTag,
            Direction direction,
            Double failSafeValue,
            Double rampRateMaxPerSec,
            Double minClampValue,
            Double maxClampValue
    ) {
        public NodeBindingConfig {
            Objects.requireNonNull(loopId, "loopId");
            Objects.requireNonNull(nodeId, "nodeId");
            Objects.requireNonNull(signalTag, "signalTag");
            direction = direction == null ? Direction.READ : direction;
        }

        public enum Direction { READ, WRITE, BOTH }
    }

    public record AuditConfig(
            String localAuditFile,
            String opcUaAuditNodeId,
            boolean writeRejectedToAudit
    ) {
        public AuditConfig {
            Objects.requireNonNull(localAuditFile, "localAuditFile");
        }
    }
}
