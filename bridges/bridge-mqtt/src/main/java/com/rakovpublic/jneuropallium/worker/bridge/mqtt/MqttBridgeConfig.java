/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.mqtt;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeSafetyMode;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Top-level configuration for the MQTT + Sparkplug B bridge
 * (02-MQTT-SPARKPLUG.md §6). Loaded from YAML via
 * {@link MqttBridgeConfigLoader}. Immutable.
 *
 * <p>Per 00-FRAMEWORK §3 unknown fields fail loading. Per 02-MQTT-SPARKPLUG.md
 * §3 &amp; §6 the structural ceiling is {@code ADVISORY}; the compact
 * constructor rejects any per-tag {@code AUTONOMOUS} promotion.
 */
public record MqttBridgeConfig(
        ConnectionConfig connection,
        SecurityConfig security,
        SparkplugConfig sparkplug,
        List<ReadBindingConfig> reads,
        List<WriteBindingConfig> writes,
        AuditConfig audit,
        Map<String, BridgeSafetyMode> perTagSafetyMode,
        Map<String, AlarmPriorityName> severityMap,
        Duration tickInterval
) {
    public MqttBridgeConfig {
        Objects.requireNonNull(connection, "connection");
        reads = reads == null ? List.of() : List.copyOf(reads);
        writes = writes == null ? List.of() : List.copyOf(writes);
        if (perTagSafetyMode == null) {
            perTagSafetyMode = Map.of();
        } else {
            Map<String, BridgeSafetyMode> linked = new LinkedHashMap<>(perTagSafetyMode);
            for (Map.Entry<String, BridgeSafetyMode> e : linked.entrySet()) {
                if (e.getValue() == BridgeSafetyMode.AUTONOMOUS) {
                    throw new IllegalArgumentException(
                            "MQTT/Sparkplug bridge ceiling is ADVISORY (02-MQTT-SPARKPLUG.md §3): "
                                    + "tag '" + e.getKey() + "' was promoted to AUTONOMOUS — refusing to load");
                }
            }
            perTagSafetyMode = Map.copyOf(linked);
        }
        severityMap = severityMap == null ? Map.of() : Map.copyOf(severityMap);
    }

    /** Connection-level settings (broker URL, client identity, keep-alive). */
    public record ConnectionConfig(
            String brokerUrl,
            String clientId,
            boolean cleanSession,
            Duration keepAlive,
            int advisoryQueueSize
    ) {
        public ConnectionConfig {
            Objects.requireNonNull(brokerUrl, "brokerUrl");
            Objects.requireNonNull(clientId, "clientId");
            keepAlive = keepAlive == null ? Duration.ofSeconds(30) : keepAlive;
            if (advisoryQueueSize <= 0) advisoryQueueSize = 10_000;
        }

        @JsonCreator
        public static ConnectionConfig of(
                @JsonProperty("brokerUrl") String brokerUrl,
                @JsonProperty("clientId") String clientId,
                @JsonProperty("cleanSession") Boolean cleanSession,
                @JsonProperty("keepAlive") Duration keepAlive,
                @JsonProperty("advisoryQueueSize") Integer advisoryQueueSize) {
            return new ConnectionConfig(
                    brokerUrl, clientId,
                    cleanSession != null && cleanSession,
                    keepAlive,
                    advisoryQueueSize == null ? 10_000 : advisoryQueueSize);
        }
    }

    /**
     * Auth + transport security. {@code passwordEnv} names an environment
     * variable rather than embedding a secret; the config loader does not
     * read it (the connection layer does at start-up).
     */
    public record SecurityConfig(
            SecurityType type,
            String username,
            String passwordEnv,
            String trustStore,
            String clientCertificate,
            String clientKeyEnv
    ) {
        @JsonCreator
        public static SecurityConfig of(
                @JsonProperty("type") SecurityType type,
                @JsonProperty("username") String username,
                @JsonProperty("passwordEnv") String passwordEnv,
                @JsonProperty("trustStore") String trustStore,
                @JsonProperty("clientCertificate") String clientCertificate,
                @JsonProperty("clientKeyEnv") String clientKeyEnv) {
            return new SecurityConfig(type, username, passwordEnv,
                    trustStore, clientCertificate, clientKeyEnv);
        }
    }

    public enum SecurityType { None, UsernamePassword, ClientCertificate }

    /** Sparkplug-specific edge-node identity (02-MQTT-SPARKPLUG.md §6). */
    public record SparkplugConfig(
            boolean enabled,
            String groupId,
            String edgeNodeId,
            String advisoryNamespace
    ) {
        @JsonCreator
        public static SparkplugConfig of(
                @JsonProperty("enabled") Boolean enabled,
                @JsonProperty("groupId") String groupId,
                @JsonProperty("edgeNodeId") String edgeNodeId,
                @JsonProperty("advisoryNamespace") String advisoryNamespace) {
            return new SparkplugConfig(
                    enabled != null && enabled, groupId, edgeNodeId,
                    advisoryNamespace == null || advisoryNamespace.isBlank()
                            ? "advisory" : advisoryNamespace);
        }
    }

    /**
     * One ingress binding — either a Sparkplug metric address or a plain MQTT
     * topic. Exactly one of {@code sparkplugMetric} / {@code plainMqttTopic}
     * must be set; the loader enforces the rule.
     */
    public record ReadBindingConfig(
            String bindingId,
            String sparkplugMetric,
            String plainMqttTopic,
            String jsonPath,
            String signalTag,
            ReadSignalKind signalKind
    ) {
        public ReadBindingConfig {
            Objects.requireNonNull(bindingId, "bindingId");
            boolean hasSp = sparkplugMetric != null && !sparkplugMetric.isBlank();
            boolean hasMq = plainMqttTopic != null && !plainMqttTopic.isBlank();
            if (hasSp == hasMq) {
                throw new IllegalArgumentException(
                        "ReadBinding '" + bindingId + "' must set exactly one of "
                                + "sparkplugMetric or plainMqttTopic");
            }
            signalKind = signalKind == null ? ReadSignalKind.MEASUREMENT : signalKind;
            if (hasMq && (jsonPath == null || jsonPath.isBlank()) && signalKind == ReadSignalKind.MEASUREMENT) {
                throw new IllegalArgumentException(
                        "ReadBinding '" + bindingId + "' uses plainMqttTopic but no jsonPath");
            }
        }

        @JsonCreator
        public static ReadBindingConfig of(
                @JsonProperty("bindingId") String bindingId,
                @JsonProperty("sparkplugMetric") String sparkplugMetric,
                @JsonProperty("plainMqttTopic") String plainMqttTopic,
                @JsonProperty("jsonPath") String jsonPath,
                @JsonProperty("signalTag") String signalTag,
                @JsonProperty("signalKind") ReadSignalKind signalKind) {
            return new ReadBindingConfig(bindingId, sparkplugMetric, plainMqttTopic,
                    jsonPath, signalTag, signalKind);
        }
    }

    /** What signal class a {@link ReadBindingConfig} produces. */
    public enum ReadSignalKind { MEASUREMENT, ALARM }

    /**
     * One advisory egress binding. The bridge clamps to
     * {@code [minClampValue, maxClampValue]} before publishing per §6.
     */
    public record WriteBindingConfig(
            String bindingId,
            String advisoryTopic,
            String signalTag,
            String sparkplugMetric,
            Double minClampValue,
            Double maxClampValue,
            int qos
    ) {
        public WriteBindingConfig {
            Objects.requireNonNull(bindingId, "bindingId");
            Objects.requireNonNull(advisoryTopic, "advisoryTopic");
            Objects.requireNonNull(signalTag, "signalTag");
            if (qos < 0 || qos > 2) qos = 1;
        }

        @JsonCreator
        public static WriteBindingConfig of(
                @JsonProperty("bindingId") String bindingId,
                @JsonProperty("advisoryTopic") String advisoryTopic,
                @JsonProperty("signalTag") String signalTag,
                @JsonProperty("sparkplugMetric") String sparkplugMetric,
                @JsonProperty("minClampValue") Double minClampValue,
                @JsonProperty("maxClampValue") Double maxClampValue,
                @JsonProperty("qos") Integer qos) {
            return new WriteBindingConfig(bindingId, advisoryTopic, signalTag,
                    sparkplugMetric, minClampValue, maxClampValue,
                    qos == null ? 1 : qos);
        }
    }

    /** Audit channel configuration (00-FRAMEWORK §3, §4). */
    public record AuditConfig(
            String localAuditFile,
            String mqttAuditTopic,
            int mqttAuditQos
    ) {
        public AuditConfig {
            Objects.requireNonNull(localAuditFile, "localAuditFile");
            if (mqttAuditQos < 0 || mqttAuditQos > 2) mqttAuditQos = 1;
        }

        @JsonCreator
        public static AuditConfig of(
                @JsonProperty("localAuditFile") String localAuditFile,
                @JsonProperty("mqttAuditTopic") String mqttAuditTopic,
                @JsonProperty("mqttAuditQos") Integer mqttAuditQos) {
            return new AuditConfig(localAuditFile, mqttAuditTopic,
                    mqttAuditQos == null ? 1 : mqttAuditQos);
        }
    }

    /** Stable name for the AlarmPriority enum so it can appear in YAML config. */
    public enum AlarmPriorityName { JOURNAL, LOW, HIGH, URGENT }
}
