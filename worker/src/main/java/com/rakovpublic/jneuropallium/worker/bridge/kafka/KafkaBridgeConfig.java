/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.kafka;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeSafetyMode;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Top-level configuration for the Apache Kafka bridge (08-KAFKA.md §5).
 *
 * <p>Loaded from YAML via {@link KafkaBridgeConfigLoader}. Immutable.
 *
 * <p>Per 00-FRAMEWORK §3, unknown fields fail loading rather than being
 * silently ignored — typos in a security-bridge config are dangerous.
 */
public record KafkaBridgeConfig(
        ClusterConfig cluster,
        SecurityConfig security,
        SchemaRegistryConfig schemaRegistry,
        List<ReadBindingConfig> reads,
        List<WriteBindingConfig> writes,
        AuditConfig audit,
        Map<String, BridgeSafetyMode> perTagSafetyMode
) {
    public KafkaBridgeConfig {
        Objects.requireNonNull(cluster, "cluster");
        reads = reads == null ? List.of() : List.copyOf(reads);
        writes = writes == null ? List.of() : List.copyOf(writes);
        if (perTagSafetyMode == null) {
            perTagSafetyMode = Map.of();
        } else {
            Map<String, BridgeSafetyMode> linked = new LinkedHashMap<>(perTagSafetyMode);
            for (Map.Entry<String, BridgeSafetyMode> e : linked.entrySet()) {
                if (e.getValue() == BridgeSafetyMode.AUTONOMOUS) {
                    throw new IllegalArgumentException(
                            "Kafka advisory bridge cannot run in AUTONOMOUS mode (tag="
                                    + e.getKey() + "); 08-KAFKA.md §1 ceiling is ADVISORY");
                }
            }
            perTagSafetyMode = Map.copyOf(linked);
        }
    }

    /** Bootstrap + consumer-group level settings. */
    public record ClusterConfig(
            String bootstrapServers,
            String consumerGroupId,
            boolean enableAutoCommit,
            int maxPollRecords,
            Duration pollTimeout,
            Duration maxPollInterval
    ) {
        public ClusterConfig {
            Objects.requireNonNull(bootstrapServers, "bootstrapServers");
            Objects.requireNonNull(consumerGroupId, "consumerGroupId");
            if (maxPollRecords <= 0) {
                throw new IllegalArgumentException("maxPollRecords must be > 0");
            }
            pollTimeout = pollTimeout == null ? Duration.ofMillis(500) : pollTimeout;
            maxPollInterval = maxPollInterval == null ? Duration.ofMinutes(5) : maxPollInterval;
        }

        @JsonCreator
        public static ClusterConfig of(
                @JsonProperty("bootstrapServers") String bootstrapServers,
                @JsonProperty("consumerGroupId") String consumerGroupId,
                @JsonProperty("enableAutoCommit") Boolean enableAutoCommit,
                @JsonProperty("maxPollRecords") Integer maxPollRecords,
                @JsonProperty("pollTimeout") Duration pollTimeout,
                @JsonProperty("maxPollInterval") Duration maxPollInterval) {
            return new ClusterConfig(
                    bootstrapServers,
                    consumerGroupId,
                    enableAutoCommit != null && enableAutoCommit,
                    maxPollRecords == null ? 500 : maxPollRecords,
                    pollTimeout,
                    maxPollInterval);
        }
    }

    /** TLS / SASL configuration (08-KAFKA.md §5). All fields optional. */
    public record SecurityConfig(
            String protocol,
            String saslMechanism,
            String truststore,
            String oauthTokenEndpoint,
            String clientIdEnv,
            String clientSecretEnv
    ) {
        @JsonCreator
        public static SecurityConfig of(
                @JsonProperty("protocol") String protocol,
                @JsonProperty("saslMechanism") String saslMechanism,
                @JsonProperty("truststore") String truststore,
                @JsonProperty("oauthTokenEndpoint") String oauthTokenEndpoint,
                @JsonProperty("clientIdEnv") String clientIdEnv,
                @JsonProperty("clientSecretEnv") String clientSecretEnv) {
            return new SecurityConfig(protocol, saslMechanism, truststore,
                    oauthTokenEndpoint, clientIdEnv, clientSecretEnv);
        }
    }

    /** Optional Confluent Schema Registry (08-KAFKA.md §5). */
    public record SchemaRegistryConfig(
            boolean enabled,
            String url,
            boolean mandatory
    ) {
        @JsonCreator
        public static SchemaRegistryConfig of(
                @JsonProperty("enabled") Boolean enabled,
                @JsonProperty("url") String url,
                @JsonProperty("mandatory") Boolean mandatory) {
            return new SchemaRegistryConfig(
                    enabled != null && enabled,
                    url,
                    mandatory != null && mandatory);
        }
    }

    /** §4 source binding — one Kafka topic feeding one signal class. */
    public record ReadBindingConfig(
            String bindingId,
            String topic,
            PayloadFormat payloadFormat,
            String decoder,
            TargetSignal targetSignal,
            String signalTagPrefix,
            FailurePolicy failurePolicy
    ) {
        public ReadBindingConfig {
            Objects.requireNonNull(bindingId, "bindingId");
            Objects.requireNonNull(topic, "topic");
            Objects.requireNonNull(targetSignal, "targetSignal");
            payloadFormat = payloadFormat == null ? PayloadFormat.JSON : payloadFormat;
            failurePolicy = failurePolicy == null
                    ? FailurePolicy.STOP_AT_FAILED_OFFSET : failurePolicy;
        }

        @JsonCreator
        public static ReadBindingConfig of(
                @JsonProperty("bindingId") String bindingId,
                @JsonProperty("topic") String topic,
                @JsonProperty("payloadFormat") PayloadFormat payloadFormat,
                @JsonProperty("decoder") String decoder,
                @JsonProperty("targetSignal") TargetSignal targetSignal,
                @JsonProperty("signalTagPrefix") String signalTagPrefix,
                @JsonProperty("failurePolicy") FailurePolicy failurePolicy) {
            return new ReadBindingConfig(bindingId, topic, payloadFormat, decoder,
                    targetSignal, signalTagPrefix, failurePolicy);
        }
    }

    /** §4 advisory egress — one signal class published to one topic. */
    public record WriteBindingConfig(
            String bindingId,
            String topic,
            PayloadFormat payloadFormat,
            String signalTag,
            int maxQueueSize
    ) {
        public WriteBindingConfig {
            Objects.requireNonNull(bindingId, "bindingId");
            Objects.requireNonNull(topic, "topic");
            Objects.requireNonNull(signalTag, "signalTag");
            payloadFormat = payloadFormat == null ? PayloadFormat.JSON : payloadFormat;
        }

        @JsonCreator
        public static WriteBindingConfig of(
                @JsonProperty("bindingId") String bindingId,
                @JsonProperty("topic") String topic,
                @JsonProperty("payloadFormat") PayloadFormat payloadFormat,
                @JsonProperty("signalTag") String signalTag,
                @JsonProperty("maxQueueSize") Integer maxQueueSize) {
            return new WriteBindingConfig(bindingId, topic, payloadFormat, signalTag,
                    maxQueueSize == null || maxQueueSize <= 0 ? 10_000 : maxQueueSize);
        }
    }

    public record AuditConfig(
            String localAuditFile
    ) {
        public AuditConfig {
            Objects.requireNonNull(localAuditFile, "localAuditFile");
        }
    }

    /** Source payload encoding. */
    public enum PayloadFormat { JSON, AVRO }

    /** Target signal class (08-KAFKA.md §4). */
    public enum TargetSignal {
        LOG_EVENT,
        PACKET,
        SIGNATURE_MATCH,
        SYSCALL,
        ANOMALY_SCORE
    }

    /** Per-binding bad-record handling (08-KAFKA.md §9 R1). */
    public enum FailurePolicy {
        /** Refuse to advance the consumer offset past a record that failed to decode. */
        STOP_AT_FAILED_OFFSET,
        /** Advance the offset, log a {@code FAILED} audit record, drop the message. */
        SKIP_AND_LOG
    }

}
