/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.mavlink;

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
 * Top-level configuration for the MAVLink bridge (12-MAVLINK.md §6).
 * Loaded from YAML via {@link MavlinkBridgeConfigLoader}. Immutable.
 *
 * <p>Per 12-MAVLINK.md §3 the structural ceiling is {@code SIM-ONLY}: writes
 * to the actuator-class MAVLink message types in
 * {@link #FORBIDDEN_WRITE_MESSAGE_TYPES} are rejected at config load unless
 * {@link #simulatorOnly()} is {@code true}, and {@code AUTONOMOUS} per-tag
 * promotion is rejected for the same reason. Bindings whose {@code systemId}
 * does not appear in their connection's {@code expectedSystems} whitelist
 * are also rejected (§6, §11 R1) — this catches typos that would silently
 * consume cross-talk on a shared UDP segment.
 */
public record MavlinkBridgeConfig(
        List<ConnectionConfig> connections,
        boolean simulatorOnly,
        List<ReadBindingConfig> reads,
        List<EventBindingConfig> events,
        List<WriteBindingConfig> writes,
        AuditConfig audit,
        Map<String, BridgeSafetyMode> perTagSafetyMode,
        Duration tickInterval
) {

    /**
     * MAVLink message types whose write bindings are rejected unless
     * {@link #simulatorOnly()} is {@code true}. These name commands that
     * the bridge MUST NOT emit toward a physical autopilot
     * (12-MAVLINK.md §3, §6).
     */
    public static final Set<String> FORBIDDEN_WRITE_MESSAGE_TYPES = Set.of(
            "COMMAND_LONG",
            "COMMAND_INT",
            "SET_MODE",
            "MANUAL_CONTROL",
            "RC_CHANNELS_OVERRIDE",
            "MISSION_ITEM",
            "MISSION_ITEM_INT",
            "MISSION_COUNT",
            "MISSION_CLEAR_ALL",
            "MISSION_SET_CURRENT",
            "MISSION_WRITE_PARTIAL_LIST"
    );

    public MavlinkBridgeConfig {
        connections = connections == null ? List.of() : List.copyOf(connections);
        if (connections.isEmpty()) {
            throw new IllegalArgumentException(
                    "MAVLink bridge: at least one connection must be configured (12-MAVLINK.md §6)");
        }
        reads = reads == null ? List.of() : List.copyOf(reads);
        events = events == null ? List.of() : List.copyOf(events);
        writes = writes == null ? List.of() : List.copyOf(writes);
        tickInterval = tickInterval == null ? Duration.ofMillis(250) : tickInterval;

        // Connection ids must be unique.
        Set<String> connIds = new LinkedHashSet<>();
        for (ConnectionConfig c : connections) {
            if (!connIds.add(c.id())) {
                throw new IllegalArgumentException("Duplicate connection id: " + c.id());
            }
        }
        Map<String, ConnectionConfig> byId = new LinkedHashMap<>();
        for (ConnectionConfig c : connections) byId.put(c.id(), c);

        // Per-bindingId uniqueness across reads/events/writes (the runtime
        // maps key on bindingId).
        Set<String> seen = new LinkedHashSet<>();
        for (ReadBindingConfig r : reads) {
            if (!seen.add(r.bindingId())) {
                throw new IllegalArgumentException("Duplicate bindingId: " + r.bindingId());
            }
            requireConnection(byId, r.connectionId(), r.bindingId());
            requireExpectedSystem(byId.get(r.connectionId()), r.systemId(), r.bindingId());
        }
        for (EventBindingConfig e : events) {
            if (!seen.add(e.bindingId())) {
                throw new IllegalArgumentException("Duplicate bindingId: " + e.bindingId());
            }
            requireConnection(byId, e.connectionId(), e.bindingId());
        }
        for (WriteBindingConfig w : writes) {
            if (!seen.add(w.bindingId())) {
                throw new IllegalArgumentException("Duplicate bindingId: " + w.bindingId());
            }
            requireConnection(byId, w.connectionId(), w.bindingId());
        }

        // §3, §6: forbidden-message-type rule — sim-only required.
        if (!simulatorOnly) {
            for (WriteBindingConfig w : writes) {
                if (w.messageType() != null
                        && FORBIDDEN_WRITE_MESSAGE_TYPES.contains(w.messageType())) {
                    throw new IllegalArgumentException(
                            "MAVLink bridge: write binding '" + w.bindingId()
                                    + "' targets actuating message type '" + w.messageType()
                                    + "' but simulatorOnly=false (12-MAVLINK.md §3, §6)");
                }
            }
        }

        // §3: AUTONOMOUS per-tag promotion only allowed in simulatorOnly mode.
        if (perTagSafetyMode == null) {
            perTagSafetyMode = Map.of();
        } else {
            Map<String, BridgeSafetyMode> linked = new LinkedHashMap<>(perTagSafetyMode);
            if (!simulatorOnly) {
                for (Map.Entry<String, BridgeSafetyMode> ent : linked.entrySet()) {
                    if (ent.getValue() == BridgeSafetyMode.AUTONOMOUS) {
                        throw new IllegalArgumentException(
                                "MAVLink bridge ceiling is SIM-ONLY (12-MAVLINK.md §3): "
                                        + "binding '" + ent.getKey()
                                        + "' was promoted to AUTONOMOUS but simulatorOnly=false");
                    }
                }
            }
            perTagSafetyMode = Map.copyOf(linked);
        }
    }

    @JsonCreator
    public static MavlinkBridgeConfig create(
            @JsonProperty("connections") List<ConnectionConfig> connections,
            @JsonProperty("simulatorOnly") Boolean simulatorOnly,
            @JsonProperty("reads") List<ReadBindingConfig> reads,
            @JsonProperty("events") List<EventBindingConfig> events,
            @JsonProperty("writes") List<WriteBindingConfig> writes,
            @JsonProperty("audit") AuditConfig audit,
            @JsonProperty("perTagSafetyMode") Map<String, BridgeSafetyMode> perTagSafetyMode,
            @JsonProperty("tickInterval") Duration tickInterval) {
        return new MavlinkBridgeConfig(
                connections,
                simulatorOnly == null || simulatorOnly,
                reads, events, writes, audit, perTagSafetyMode, tickInterval);
    }

    private static void requireConnection(Map<String, ConnectionConfig> byId, String id, String bindingId) {
        if (id == null || !byId.containsKey(id)) {
            throw new IllegalArgumentException(
                    "Binding '" + bindingId + "' references unknown connectionId '" + id + "'");
        }
    }

    private static void requireExpectedSystem(ConnectionConfig c, int systemId, String bindingId) {
        if (c.expectedSystems() == null || c.expectedSystems().isEmpty()) return;
        for (int s : c.expectedSystems()) if (s == systemId) return;
        throw new IllegalArgumentException(
                "Binding '" + bindingId + "' systemId=" + systemId
                        + " not in connection '" + c.id() + "' expectedSystems"
                        + c.expectedSystems()
                        + " (12-MAVLINK.md §6, §11 R1)");
    }

    /** Transports supported in §4 / §6 (UDP and TCP only — serial is hardware-only). */
    public enum Transport { UDP, TCP }

    /** What signal class a {@link ReadBindingConfig} produces (§5). */
    public enum ReadSignalKind {
        /** Own-vehicle pose / motion / energy. */
        PROPRIOCEPTIVE,
        /** Battery / link efficiency. */
        EFFICIENCY,
        /** Observed pose of a peer MAV system. */
        PEER_OBSERVATION,
        /** Bridge-level liveness or sensor-health alarm. */
        ALARM,
        /** Anomaly score (RSSI/noise thresholds). */
        ANOMALY
    }

    /** One MAVLink connection (§4). */
    public record ConnectionConfig(
            String id,
            Transport transport,
            String bindAddress,
            Integer bindPort,
            String host,
            Integer port,
            List<Integer> expectedSystems
    ) {
        public ConnectionConfig {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(transport, "transport");
            expectedSystems = expectedSystems == null ? List.of() : List.copyOf(expectedSystems);
            if (transport == Transport.UDP && bindPort == null) {
                throw new IllegalArgumentException(
                        "UDP connection '" + id + "' requires bindPort (12-MAVLINK.md §6)");
            }
            if (transport == Transport.TCP && (host == null || port == null)) {
                throw new IllegalArgumentException(
                        "TCP connection '" + id + "' requires host and port (12-MAVLINK.md §6)");
            }
        }

        @JsonCreator
        public static ConnectionConfig of(
                @JsonProperty("id") String id,
                @JsonProperty("transport") Transport transport,
                @JsonProperty("bindAddress") String bindAddress,
                @JsonProperty("bindPort") Integer bindPort,
                @JsonProperty("host") String host,
                @JsonProperty("port") Integer port,
                @JsonProperty("expectedSystems") List<Integer> expectedSystems) {
            return new ConnectionConfig(id, transport, bindAddress, bindPort, host, port, expectedSystems);
        }
    }

    /** One per-system telemetry read binding (§5, §6). */
    public record ReadBindingConfig(
            String bindingId,
            String connectionId,
            int systemId,
            Integer componentId,
            String messageType,
            ReadSignalKind targetSignal,
            String signalTag,
            String peerId,
            int decimateBy
    ) {
        public ReadBindingConfig {
            Objects.requireNonNull(bindingId, "bindingId");
            Objects.requireNonNull(connectionId, "connectionId");
            Objects.requireNonNull(messageType, "messageType");
            if (decimateBy <= 0) decimateBy = 1;
            if (targetSignal == null) {
                targetSignal = inferKind(messageType);
            }
            if (targetSignal == ReadSignalKind.PEER_OBSERVATION
                    && !"GLOBAL_POSITION_INT".equals(messageType)) {
                throw new IllegalArgumentException(
                        "ReadBinding '" + bindingId
                                + "' PEER_OBSERVATION requires messageType=GLOBAL_POSITION_INT (12-MAVLINK.md §5)");
            }
        }

        @JsonCreator
        public static ReadBindingConfig of(
                @JsonProperty("bindingId") String bindingId,
                @JsonProperty("connectionId") String connectionId,
                @JsonProperty("systemId") Integer systemId,
                @JsonProperty("componentId") Integer componentId,
                @JsonProperty("messageType") String messageType,
                @JsonProperty("targetSignal") ReadSignalKind targetSignal,
                @JsonProperty("signalTag") String signalTag,
                @JsonProperty("peerId") String peerId,
                @JsonProperty("decimateBy") Integer decimateBy) {
            return new ReadBindingConfig(
                    bindingId, connectionId,
                    systemId == null ? 1 : systemId,
                    componentId,
                    messageType, targetSignal, signalTag, peerId,
                    decimateBy == null ? 1 : decimateBy);
        }

        private static ReadSignalKind inferKind(String messageType) {
            return switch (messageType) {
                case "GLOBAL_POSITION_INT", "ATTITUDE", "GPS_RAW_INT" -> ReadSignalKind.PROPRIOCEPTIVE;
                case "BATTERY_STATUS" -> ReadSignalKind.EFFICIENCY;
                case "SYS_STATUS", "STATUSTEXT" -> ReadSignalKind.ALARM;
                case "RADIO_STATUS" -> ReadSignalKind.ANOMALY;
                default -> ReadSignalKind.ALARM;
            };
        }
    }

    /** Multi-system event-class subscription (§6). */
    public record EventBindingConfig(
            String bindingId,
            String connectionId,
            String messageType,
            ReadSignalKind targetSignal,
            String signalTagPrefix
    ) {
        public EventBindingConfig {
            Objects.requireNonNull(bindingId, "bindingId");
            Objects.requireNonNull(connectionId, "connectionId");
            Objects.requireNonNull(messageType, "messageType");
            if (targetSignal == null) targetSignal = ReadSignalKind.ALARM;
        }

        @JsonCreator
        public static EventBindingConfig of(
                @JsonProperty("bindingId") String bindingId,
                @JsonProperty("connectionId") String connectionId,
                @JsonProperty("messageType") String messageType,
                @JsonProperty("targetSignal") ReadSignalKind targetSignal,
                @JsonProperty("signalTagPrefix") String signalTagPrefix) {
            return new EventBindingConfig(bindingId, connectionId, messageType,
                    targetSignal, signalTagPrefix);
        }
    }

    /**
     * One advisory egress binding. Egress to the message types in
     * {@link MavlinkBridgeConfig#FORBIDDEN_WRITE_MESSAGE_TYPES} is rejected
     * at config load unless {@code simulatorOnly == true} (§3, §6).
     */
    public record WriteBindingConfig(
            String bindingId,
            String connectionId,
            int targetSystemId,
            Integer targetComponentId,
            String messageType,
            String signalTag,
            Double minClampValue,
            Double maxClampValue,
            Double rampRateMaxPerSec,
            Double failSafeValue
    ) {
        public WriteBindingConfig {
            Objects.requireNonNull(bindingId, "bindingId");
            Objects.requireNonNull(connectionId, "connectionId");
            Objects.requireNonNull(messageType, "messageType");
            Objects.requireNonNull(signalTag, "signalTag");
        }

        @JsonCreator
        public static WriteBindingConfig of(
                @JsonProperty("bindingId") String bindingId,
                @JsonProperty("connectionId") String connectionId,
                @JsonProperty("targetSystemId") Integer targetSystemId,
                @JsonProperty("targetComponentId") Integer targetComponentId,
                @JsonProperty("messageType") String messageType,
                @JsonProperty("signalTag") String signalTag,
                @JsonProperty("minClampValue") Double minClampValue,
                @JsonProperty("maxClampValue") Double maxClampValue,
                @JsonProperty("rampRateMaxPerSec") Double rampRateMaxPerSec,
                @JsonProperty("failSafeValue") Double failSafeValue) {
            return new WriteBindingConfig(
                    bindingId, connectionId,
                    targetSystemId == null ? 0 : targetSystemId,
                    targetComponentId,
                    messageType, signalTag,
                    minClampValue, maxClampValue, rampRateMaxPerSec, failSafeValue);
        }
    }

    /** Audit channel (00-FRAMEWORK §3, §4; 12-MAVLINK.md §6). */
    public record AuditConfig(
            String localAuditFile
    ) {
        public AuditConfig {
            Objects.requireNonNull(localAuditFile, "localAuditFile");
        }

        @JsonCreator
        public static AuditConfig of(@JsonProperty("localAuditFile") String localAuditFile) {
            return new AuditConfig(localAuditFile);
        }
    }
}
