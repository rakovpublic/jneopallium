/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.canopen;

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
 * Top-level CANopen bridge configuration (13-CANOPEN.md §6).
 *
 * <p>Loaded from YAML via {@link CanopenBridgeConfigLoader}. Immutable; the
 * loader uses {@code FAIL_ON_UNKNOWN_PROPERTIES = true} per 00-FRAMEWORK §3
 * — typos in a config that addresses motors, brakes, and BMS packs are not
 * caught at runtime, they're caught at load.
 *
 * <p>The bridge's structural ceiling is {@code ADVISORY} (13-CANOPEN.md §3,
 * §11.13). The {@code controlword} index ({@code 0x6040}) is on the
 * {@link #FORBIDDEN_WRITE_OD_INDICES} list and is rejected at config load
 * unless explicitly listed in {@link #writeIndexAllowList} per node id.
 * Even if a write binding is present and clamped, the per-(nodeId, odIndex)
 * pair must appear in {@link #writeIndexAllowList} for it to be loaded —
 * this is the "structural defence" of §10 R3.
 */
public record CanopenBridgeConfig(
        CanBusConfig canBus,
        List<NodeConfig> nodes,
        List<ReadBindingConfig> reads,
        List<EventBindingConfig> events,
        List<WriteBindingConfig> writes,
        Map<Integer, List<Integer>> writeIndexAllowList,
        AuditConfig audit,
        Map<String, BridgeSafetyMode> perTagSafetyMode,
        Duration tickInterval
) {

    /**
     * Hard-coded safety-critical OD indices that the bridge MUST NOT write
     * unless they're individually re-allowed in
     * {@link #writeIndexAllowList} per node (13-CANOPEN.md §3, §5 egress
     * table). The {@code controlword} (0x6040) tops the list — autonomous
     * cyclic writes to it would arm the drive without external supervision.
     */
    public static final Set<Integer> FORBIDDEN_WRITE_OD_INDICES = Set.of(
            0x6040  // CiA-402 controlword
    );

    public CanopenBridgeConfig {
        Objects.requireNonNull(canBus, "canBus");
        nodes = nodes == null ? List.of() : List.copyOf(nodes);
        reads = reads == null ? List.of() : List.copyOf(reads);
        events = events == null ? List.of() : List.copyOf(events);
        writes = writes == null ? List.of() : List.copyOf(writes);
        tickInterval = tickInterval == null ? Duration.ofMillis(250) : tickInterval;

        if (writeIndexAllowList == null) {
            writeIndexAllowList = Map.of();
        } else {
            Map<Integer, List<Integer>> normalised = new LinkedHashMap<>();
            for (Map.Entry<Integer, List<Integer>> e : writeIndexAllowList.entrySet()) {
                normalised.put(e.getKey(),
                        e.getValue() == null ? List.of() : List.copyOf(e.getValue()));
            }
            writeIndexAllowList = Map.copyOf(normalised);
        }

        // Node ids must be unique.
        Set<Integer> nodeIds = new LinkedHashSet<>();
        for (NodeConfig n : nodes) {
            if (!nodeIds.add(n.id())) {
                throw new IllegalArgumentException(
                        "CANopen bridge: duplicate node id 0x" + Integer.toHexString(n.id()));
            }
        }

        // Binding ids must be globally unique across reads / events / writes.
        Set<String> seen = new LinkedHashSet<>();
        for (ReadBindingConfig r : reads) {
            if (!seen.add(r.bindingId())) {
                throw new IllegalArgumentException("Duplicate bindingId: " + r.bindingId());
            }
            if (!nodeIds.isEmpty() && !nodeIds.contains(r.nodeId())) {
                throw new IllegalArgumentException(
                        "Read binding '" + r.bindingId() + "' references unknown nodeId 0x"
                                + Integer.toHexString(r.nodeId()));
            }
        }
        for (EventBindingConfig ev : events) {
            if (!seen.add(ev.bindingId())) {
                throw new IllegalArgumentException("Duplicate bindingId: " + ev.bindingId());
            }
            if (!nodeIds.isEmpty() && !nodeIds.contains(ev.nodeId())) {
                throw new IllegalArgumentException(
                        "Event binding '" + ev.bindingId() + "' references unknown nodeId 0x"
                                + Integer.toHexString(ev.nodeId()));
            }
        }
        for (WriteBindingConfig w : writes) {
            if (!seen.add(w.bindingId())) {
                throw new IllegalArgumentException("Duplicate bindingId: " + w.bindingId());
            }
            if (!nodeIds.isEmpty() && !nodeIds.contains(w.nodeId())) {
                throw new IllegalArgumentException(
                        "Write binding '" + w.bindingId() + "' references unknown nodeId 0x"
                                + Integer.toHexString(w.nodeId()));
            }
            // §3, §6, §10 R3 — every write must clear two structural gates:
            // 1. The hard-coded forbidden-index list rejects controlword unconditionally.
            // 2. The per-node allow list must include the index.
            if (FORBIDDEN_WRITE_OD_INDICES.contains(w.odIndex())) {
                throw new IllegalArgumentException(
                        "CANopen bridge: write binding '" + w.bindingId()
                                + "' targets forbidden OD index 0x"
                                + Integer.toHexString(w.odIndex())
                                + " (13-CANOPEN.md §3, §5 egress table)");
            }
            List<Integer> allowed = writeIndexAllowList.get(w.nodeId());
            if (allowed == null || !allowed.contains(w.odIndex())) {
                throw new IllegalArgumentException(
                        "CANopen bridge: write binding '" + w.bindingId()
                                + "' targets (nodeId=0x" + Integer.toHexString(w.nodeId())
                                + ", odIndex=0x" + Integer.toHexString(w.odIndex())
                                + ") which is not on writeIndexAllowList (13-CANOPEN.md §6)");
            }
        }

        perTagSafetyMode = perTagSafetyMode == null
                ? Map.of()
                : Map.copyOf(perTagSafetyMode);
    }

    @JsonCreator
    public static CanopenBridgeConfig create(
            @JsonProperty("canBus") CanBusConfig canBus,
            @JsonProperty("nodes") List<NodeConfig> nodes,
            @JsonProperty("reads") List<ReadBindingConfig> reads,
            @JsonProperty("events") List<EventBindingConfig> events,
            @JsonProperty("writes") List<WriteBindingConfig> writes,
            @JsonProperty("writeIndexAllowList") Map<Integer, List<Integer>> writeIndexAllowList,
            @JsonProperty("audit") AuditConfig audit,
            @JsonProperty("perTagSafetyMode") Map<String, BridgeSafetyMode> perTagSafetyMode,
            @JsonProperty("tickInterval") Duration tickInterval) {
        return new CanopenBridgeConfig(
                canBus, nodes, reads, events, writes,
                writeIndexAllowList, audit, perTagSafetyMode, tickInterval);
    }

    /** Transports supported in §6 — the JNA-SocketCAN escape hatch and the cross-platform USB-CAN dongle path. */
    public enum BusType { SOCKETCAN, USB_CAN }

    /** What signal class a {@link ReadBindingConfig} produces (§5). */
    public enum ReadSignalKind {
        /** Joint state / encoder position / velocity from a CiA-402 drive. */
        PROPRIOCEPTIVE,
        /** Generic measurement — CiA-401 I/O, sensor PDO. */
        MEASUREMENT,
        /** Battery / pack metric — CiA-418 BMS. */
        EFFICIENCY,
        /** Drive state-machine snapshot — statusword decoded into BatchStateSignal. */
        BATCH_STATE,
        /** Bridge-level liveness or fault alarm. */
        ALARM
    }

    /** Source COB-ID class an OD index is mapped through (§5). */
    public enum PdoSource {
        TPDO1, TPDO2, TPDO3, TPDO4,
        /** SDO read (acyclic). Not used for cyclic mapping. */
        SDO
    }

    /** OD value type used by the decoder/encoder (CiA-301 §7.4). */
    public enum OdType {
        UINT8, UINT16, UINT32,
        INT8, INT16, INT32,
        REAL32
    }

    /** §6 {@code canBus:} block. */
    public record CanBusConfig(
            BusType type,
            String device,
            int bitrate,
            Double samplePoint
    ) {
        public CanBusConfig {
            Objects.requireNonNull(type, "type");
            Objects.requireNonNull(device, "device");
            if (bitrate <= 0) {
                throw new IllegalArgumentException("bitrate must be positive");
            }
        }

        @JsonCreator
        public static CanBusConfig of(
                @JsonProperty("type") BusType type,
                @JsonProperty("device") String device,
                @JsonProperty("bitrate") Integer bitrate,
                @JsonProperty("samplePoint") Double samplePoint) {
            return new CanBusConfig(type, device,
                    bitrate == null ? 500_000 : bitrate, samplePoint);
        }
    }

    /** §6 {@code nodes:} entry. */
    public record NodeConfig(
            int id,
            String type,
            String profileEdsFile
    ) {
        public NodeConfig {
            if (id < 1 || id > 127) {
                throw new IllegalArgumentException(
                        "CANopen node id must be in 1..127, got 0x" + Integer.toHexString(id));
            }
        }

        @JsonCreator
        public static NodeConfig of(
                @JsonProperty("id") Integer id,
                @JsonProperty("type") String type,
                @JsonProperty("profileEdsFile") String profileEdsFile) {
            Objects.requireNonNull(id, "node id");
            return new NodeConfig(id, type, profileEdsFile);
        }
    }

    /** §6 {@code reads:} entry — one PDO/SDO read binding. */
    public record ReadBindingConfig(
            String bindingId,
            int nodeId,
            int odIndex,
            int subIndex,
            PdoSource pdoSource,
            OdType odType,
            ReadSignalKind targetSignal,
            String signalTag,
            double scale,
            double offset,
            int decimateBy
    ) {
        public ReadBindingConfig {
            Objects.requireNonNull(bindingId, "bindingId");
            if (nodeId < 1 || nodeId > 127) {
                throw new IllegalArgumentException(
                        "Read binding '" + bindingId + "' nodeId must be 1..127");
            }
            if (pdoSource == null) pdoSource = PdoSource.TPDO1;
            if (odType == null) odType = OdType.UINT16;
            if (targetSignal == null) targetSignal = ReadSignalKind.MEASUREMENT;
            if (decimateBy <= 0) decimateBy = 1;
        }

        @JsonCreator
        public static ReadBindingConfig of(
                @JsonProperty("bindingId") String bindingId,
                @JsonProperty("nodeId") Integer nodeId,
                @JsonProperty("odIndex") Integer odIndex,
                @JsonProperty("subIndex") Integer subIndex,
                @JsonProperty("pdoSource") PdoSource pdoSource,
                @JsonProperty("odType") OdType odType,
                @JsonProperty("targetSignal") ReadSignalKind targetSignal,
                @JsonProperty("signalTag") String signalTag,
                @JsonProperty("scale") Double scale,
                @JsonProperty("offset") Double offset,
                @JsonProperty("decimateBy") Integer decimateBy) {
            return new ReadBindingConfig(
                    bindingId,
                    nodeId == null ? 1 : nodeId,
                    odIndex == null ? 0 : odIndex,
                    subIndex == null ? 0 : subIndex,
                    pdoSource, odType, targetSignal, signalTag,
                    scale == null ? 1.0 : scale,
                    offset == null ? 0.0 : offset,
                    decimateBy == null ? 1 : decimateBy);
        }
    }

    /** §6 {@code events:} entry — EMCY / heartbeat-loss subscription. */
    public record EventBindingConfig(
            String bindingId,
            int nodeId,
            EventSource source,
            ReadSignalKind targetSignal,
            String signalTagPrefix
    ) {
        public EventBindingConfig {
            Objects.requireNonNull(bindingId, "bindingId");
            if (source == null) source = EventSource.EMCY;
            if (targetSignal == null) targetSignal = ReadSignalKind.ALARM;
        }

        @JsonCreator
        public static EventBindingConfig of(
                @JsonProperty("bindingId") String bindingId,
                @JsonProperty("nodeId") Integer nodeId,
                @JsonProperty("source") EventSource source,
                @JsonProperty("targetSignal") ReadSignalKind targetSignal,
                @JsonProperty("signalTagPrefix") String signalTagPrefix) {
            Objects.requireNonNull(nodeId, "nodeId");
            return new EventBindingConfig(bindingId, nodeId, source, targetSignal, signalTagPrefix);
        }
    }

    /** Event-class taxonomy. */
    public enum EventSource {
        /** CiA-301 emergency frame (COB-ID = 0x080 + node). */
        EMCY,
        /** Heartbeat-timeout watchdog (synthesised by the bridge). */
        HEARTBEAT_LOSS
    }

    /** §6 {@code writes:} entry — one advisory egress binding. */
    public record WriteBindingConfig(
            String bindingId,
            int nodeId,
            int odIndex,
            int subIndex,
            OdType odType,
            WriteVia via,
            String signalTag,
            Double minClampValue,
            Double maxClampValue,
            Double rampRateMaxPerSec,
            Double failSafeValue
    ) {
        public WriteBindingConfig {
            Objects.requireNonNull(bindingId, "bindingId");
            Objects.requireNonNull(signalTag, "signalTag");
            if (nodeId < 1 || nodeId > 127) {
                throw new IllegalArgumentException(
                        "Write binding '" + bindingId + "' nodeId must be 1..127");
            }
            if (odType == null) odType = OdType.UINT32;
            if (via == null) via = WriteVia.SDO;
        }

        @JsonCreator
        public static WriteBindingConfig of(
                @JsonProperty("bindingId") String bindingId,
                @JsonProperty("nodeId") Integer nodeId,
                @JsonProperty("odIndex") Integer odIndex,
                @JsonProperty("subIndex") Integer subIndex,
                @JsonProperty("odType") OdType odType,
                @JsonProperty("via") WriteVia via,
                @JsonProperty("signalTag") String signalTag,
                @JsonProperty("minClampValue") Double minClampValue,
                @JsonProperty("maxClampValue") Double maxClampValue,
                @JsonProperty("rampRateMaxPerSec") Double rampRateMaxPerSec,
                @JsonProperty("failSafeValue") Double failSafeValue) {
            Objects.requireNonNull(nodeId, "nodeId");
            Objects.requireNonNull(odIndex, "odIndex");
            return new WriteBindingConfig(
                    bindingId, nodeId, odIndex,
                    subIndex == null ? 0 : subIndex,
                    odType, via, signalTag,
                    minClampValue, maxClampValue,
                    rampRateMaxPerSec, failSafeValue);
        }
    }

    /** Egress channel — SDO is acyclic and audited per write; RPDO is cyclic and per-tick. */
    public enum WriteVia { SDO, RPDO }

    /** Audit channel (00-FRAMEWORK §3, §4; 13-CANOPEN.md §6). */
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
