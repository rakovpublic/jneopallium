/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.ros2;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeSafetyMode;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Top-level configuration for the ROS 2 / DDS bridge (04-ROS2-DDS.md §7).
 * Loaded from YAML via {@link Ros2BridgeConfigLoader}. Immutable.
 *
 * <p>Per 04-ROS2-DDS.md §3 the structural ceiling is {@code ADVISORY}. The
 * compact constructor enforces it: any per-tag promotion to
 * {@code AUTONOMOUS} fails the load <i>unless</i> {@link #simulatorOnly()} is
 * {@code true}, which models the simulator-with-watchdog escape.
 *
 * <p>The validator forbids any write binding whose topic matches one of the
 * actuating topics in {@link #FORBIDDEN_WRITE_TOPICS} unless
 * {@code simulatorOnly} is set; see §3 and §7.
 */
public record Ros2BridgeConfig(
        TransportMode mode,
        String rosbridgeUrl,
        Integer domainId,
        QosProfile qosProfile,
        boolean simulatorOnly,
        List<ReadBindingConfig> reads,
        List<WriteBindingConfig> writes,
        AuditConfig audit,
        Map<String, BridgeSafetyMode> perTagSafetyMode,
        Duration tickInterval
) {

    /**
     * Topics whose write bindings are rejected unless {@link #simulatorOnly()}
     * is {@code true}. These name actuators that the bridge MUST NOT drive
     * directly in production (04-ROS2-DDS.md §3, §7).
     */
    public static final Set<String> FORBIDDEN_WRITE_TOPICS =
            Set.of("/cmd_vel", "/joint_trajectory", "/joint_command");

    public Ros2BridgeConfig {
        Objects.requireNonNull(mode, "mode");
        if (mode == TransportMode.ROSBRIDGE) {
            Objects.requireNonNull(rosbridgeUrl, "rosbridgeUrl");
        }
        qosProfile = qosProfile == null ? QosProfile.SENSOR_DATA : qosProfile;
        reads = reads == null ? List.of() : List.copyOf(reads);
        writes = writes == null ? List.of() : List.copyOf(writes);
        tickInterval = tickInterval == null ? Duration.ofMillis(250) : tickInterval;

        // Forbidden-topic rule (§3, §7): /cmd_vel etc. require simulatorOnly.
        if (!simulatorOnly) {
            for (WriteBindingConfig w : writes) {
                if (w.topic() != null && FORBIDDEN_WRITE_TOPICS.contains(w.topic())) {
                    throw new IllegalArgumentException(
                            "ROS 2 bridge: write binding '" + w.bindingId()
                                    + "' targets actuating topic '" + w.topic()
                                    + "' but simulatorOnly=false (04-ROS2-DDS.md §3, §7)");
                }
            }
        }

        // Detect duplicate bindingIds early — the runtime maps key on it.
        Set<String> seen = new LinkedHashSet<>();
        for (ReadBindingConfig r : reads) {
            if (!seen.add(r.bindingId())) {
                throw new IllegalArgumentException("Duplicate bindingId: " + r.bindingId());
            }
        }
        for (WriteBindingConfig w : writes) {
            if (!seen.add(w.bindingId())) {
                throw new IllegalArgumentException("Duplicate bindingId: " + w.bindingId());
            }
        }

        // Per-tag AUTONOMOUS only allowed in simulatorOnly mode (04-ROS2-DDS §3).
        if (perTagSafetyMode == null) {
            perTagSafetyMode = Map.of();
        } else {
            Map<String, BridgeSafetyMode> linked = new LinkedHashMap<>(perTagSafetyMode);
            if (!simulatorOnly) {
                for (Map.Entry<String, BridgeSafetyMode> e : linked.entrySet()) {
                    if (e.getValue() == BridgeSafetyMode.AUTONOMOUS) {
                        throw new IllegalArgumentException(
                                "ROS 2 bridge ceiling is ADVISORY (04-ROS2-DDS.md §3): "
                                        + "binding '" + e.getKey() + "' was promoted to AUTONOMOUS "
                                        + "but simulatorOnly=false");
                    }
                }
            }
            perTagSafetyMode = Map.copyOf(linked);
        }
    }

    @JsonCreator
    public static Ros2BridgeConfig create(
            @JsonProperty("mode") TransportMode mode,
            @JsonProperty("rosbridgeUrl") String rosbridgeUrl,
            @JsonProperty("domainId") Integer domainId,
            @JsonProperty("qosProfile") QosProfile qosProfile,
            @JsonProperty("simulatorOnly") Boolean simulatorOnly,
            @JsonProperty("reads") List<ReadBindingConfig> reads,
            @JsonProperty("writes") List<WriteBindingConfig> writes,
            @JsonProperty("audit") AuditConfig audit,
            @JsonProperty("perTagSafetyMode") Map<String, BridgeSafetyMode> perTagSafetyMode,
            @JsonProperty("tickInterval") Duration tickInterval) {
        return new Ros2BridgeConfig(
                mode == null ? TransportMode.ROSBRIDGE : mode,
                rosbridgeUrl, domainId, qosProfile,
                simulatorOnly != null && simulatorOnly,
                reads, writes, audit, perTagSafetyMode, tickInterval);
    }

    /** Strategy A or Strategy B (04-ROS2-DDS.md §1). */
    public enum TransportMode {
        /** Strategy B — rosbridge_suite WebSocket JSON. Default. */
        ROSBRIDGE,
        /** Strategy A — embedded rcljava client. Behind a feature flag. */
        RCLJAVA
    }

    /** ROS 2 QoS preset names (04-ROS2-DDS.md §7). */
    public enum QosProfile { SENSOR_DATA, RELIABLE, PARAMETERS }

    /**
     * One subscribe-side binding. The bridge produces the signal class
     * indicated by {@link ReadSignalKind} (or, for swarm peers, by
     * {@code asPeerObservation: true}).
     */
    public record ReadBindingConfig(
            String bindingId,
            String topic,
            String msgType,
            String signalTag,
            ReadSignalKind signalKind,
            boolean asPeerObservation,
            String peerId,
            int decimateBy,
            int maxRangeBins,
            int maxPayloadBytes
    ) {
        public ReadBindingConfig {
            Objects.requireNonNull(bindingId, "bindingId");
            Objects.requireNonNull(topic, "topic");
            Objects.requireNonNull(msgType, "msgType");
            if (decimateBy <= 0) decimateBy = 1;
            if (maxRangeBins <= 0) maxRangeBins = 720;
            if (maxPayloadBytes <= 0) maxPayloadBytes = 1_048_576;
            if (signalKind == null) {
                signalKind = inferKind(msgType);
            }
            if (asPeerObservation && !"nav_msgs/msg/Odometry".equals(msgType)) {
                throw new IllegalArgumentException(
                        "ReadBinding '" + bindingId
                                + "' asPeerObservation=true requires msgType=nav_msgs/msg/Odometry");
            }
        }

        @JsonCreator
        public static ReadBindingConfig of(
                @JsonProperty("bindingId") String bindingId,
                @JsonProperty("topic") String topic,
                @JsonProperty("msgType") String msgType,
                @JsonProperty("signalTag") String signalTag,
                @JsonProperty("signalKind") ReadSignalKind signalKind,
                @JsonProperty("asPeerObservation") Boolean asPeerObservation,
                @JsonProperty("peerId") String peerId,
                @JsonProperty("decimateBy") Integer decimateBy,
                @JsonProperty("maxRangeBins") Integer maxRangeBins,
                @JsonProperty("maxPayloadBytes") Integer maxPayloadBytes) {
            return new ReadBindingConfig(
                    bindingId, topic, msgType, signalTag, signalKind,
                    asPeerObservation != null && asPeerObservation,
                    peerId,
                    decimateBy == null ? 1 : decimateBy,
                    maxRangeBins == null ? 720 : maxRangeBins,
                    maxPayloadBytes == null ? 1_048_576 : maxPayloadBytes);
        }

        private static ReadSignalKind inferKind(String msgType) {
            return switch (msgType) {
                case "sensor_msgs/msg/Image",
                     "sensor_msgs/msg/CompressedImage",
                     "sensor_msgs/msg/LaserScan",
                     "sensor_msgs/msg/PointCloud2" -> ReadSignalKind.SENSORY;
                case "sensor_msgs/msg/JointState",
                     "nav_msgs/msg/Odometry" -> ReadSignalKind.PROPRIOCEPTIVE;
                case "sensor_msgs/msg/BatteryState" -> ReadSignalKind.ENERGY;
                default -> ReadSignalKind.SENSORY;
            };
        }
    }

    /** What signal class a {@link ReadBindingConfig} produces. */
    public enum ReadSignalKind { SENSORY, PROPRIOCEPTIVE, ENERGY }

    /**
     * One advisory egress binding. The bridge publishes the encoded payload
     * onto the configured topic. Egress to the topics in
     * {@link Ros2BridgeConfig#FORBIDDEN_WRITE_TOPICS} is rejected at config
     * load unless {@code simulatorOnly == true}.
     */
    public record WriteBindingConfig(
            String bindingId,
            String topic,
            String msgType,
            String signalTag,
            Double minClampValue,
            Double maxClampValue,
            Double rampRateMaxPerSec,
            Double failSafeValue
    ) {
        public WriteBindingConfig {
            Objects.requireNonNull(bindingId, "bindingId");
            Objects.requireNonNull(topic, "topic");
            Objects.requireNonNull(msgType, "msgType");
            Objects.requireNonNull(signalTag, "signalTag");
        }

        @JsonCreator
        public static WriteBindingConfig of(
                @JsonProperty("bindingId") String bindingId,
                @JsonProperty("topic") String topic,
                @JsonProperty("msgType") String msgType,
                @JsonProperty("signalTag") String signalTag,
                @JsonProperty("minClampValue") Double minClampValue,
                @JsonProperty("maxClampValue") Double maxClampValue,
                @JsonProperty("rampRateMaxPerSec") Double rampRateMaxPerSec,
                @JsonProperty("failSafeValue") Double failSafeValue) {
            return new WriteBindingConfig(bindingId, topic, msgType, signalTag,
                    minClampValue, maxClampValue, rampRateMaxPerSec, failSafeValue);
        }
    }

    /** Audit channel configuration (00-FRAMEWORK §3, §4). */
    public record AuditConfig(
            String localAuditFile,
            String advisoryAuditTopic
    ) {
        public AuditConfig {
            Objects.requireNonNull(localAuditFile, "localAuditFile");
        }

        @JsonCreator
        public static AuditConfig of(
                @JsonProperty("localAuditFile") String localAuditFile,
                @JsonProperty("advisoryAuditTopic") String advisoryAuditTopic) {
            return new AuditConfig(localAuditFile, advisoryAuditTopic);
        }
    }
}
