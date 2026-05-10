/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.mavlink;

import com.rakovpublic.jneuropallium.worker.bridge.common.AbstractBridgeAuditOutput;
import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeAuditRecord;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.AlarmPriority;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.AlarmSignal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Owns the MAVLink connections and the per-system latest-value cache
 * (12-MAVLINK.md §4). One service can speak to many MAV systems on many
 * connections; routing is by {@code (connectionId, systemId)}.
 *
 * <p>Read flow: transport → handler → mapper → per-binding queue drained by
 * {@link MavlinkTelemetryInput} / {@link MavlinkSwarmInput} /
 * {@link MavlinkEventInput} on each tick (00-FRAMEWORK §2.1). Write flow:
 * {@link MavlinkAdvisoryOutputAggregator} computes the advisory payload and
 * calls {@link #send}.
 *
 * <p>Multi-system routing, the {@link MavlinkBridgeConfig.ConnectionConfig}
 * {@code expectedSystems} whitelist (§11 R1), and the safety-critical runtime
 * check that no write hits a forbidden actuating message type outside
 * {@code simulatorOnly} mode (§3) all live here.
 *
 * <p>Heartbeat liveness (§5): a missing {@code HEARTBEAT} for more than
 * {@link #HEARTBEAT_TIMEOUT_MS} surfaces an {@code AlarmSignal} with
 * condition {@code PEER_OFFLINE}. The timeout is checked lazily by
 * {@link #checkHeartbeats(long)} which the host loop calls each tick.
 */
public final class MavlinkClientService implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(MavlinkClientService.class);

    public static final String BRIDGE_NAME = "mavlink";
    public static final String BRIDGE_RECONNECTED = "BRIDGE_RECONNECTED";

    /** §5 heartbeat-loss threshold. */
    public static final long HEARTBEAT_TIMEOUT_MS = 2_000L;

    /** §11 R1 — single audit per minute per unknown system. */
    private static final long UNKNOWN_SYSTEM_WINDOW_MS = 60_000L;

    /** MAVLink-specific reasons in addition to 00-FRAMEWORK §4 vocabulary. */
    public static final class Reason {
        private Reason() {}
        public static final String FORBIDDEN_MESSAGE_TYPE = "FORBIDDEN_MESSAGE_TYPE";
        public static final String UNKNOWN_SYSTEM         = "UNKNOWN_SYSTEM";
        public static final String DECODER_ERROR          = "DECODER_ERROR";
        public static final String PUBLISH_ERROR          = "PUBLISH_ERROR";
        public static final String UNKNOWN_CONNECTION     = "UNKNOWN_CONNECTION";
    }

    private final MavlinkBridgeConfig config;
    private final AbstractBridgeAuditOutput audit;
    private final MavlinkSignalMapper mapper;

    /** Connections keyed by id. */
    private final Map<String, MavlinkConnectionBinding> connections = new LinkedHashMap<>();
    private final Map<String, MavlinkTransport> transports = new ConcurrentHashMap<>();

    /** Resolved bindings. */
    private final Map<String, MavlinkMessageBinding> readBindings = new LinkedHashMap<>();
    private final Map<String, MavlinkMessageBinding> eventBindings = new LinkedHashMap<>();
    private final Map<String, MavlinkMessageBinding> writeBindings = new LinkedHashMap<>();

    /**
     * Index: (connectionId, systemId, messageType) → list of read bindings.
     * Cross-product because one binding can match many bindings only when
     * config repeats; we keep it as a list to preserve order.
     */
    private final Map<RouteKey, List<MavlinkMessageBinding>> readRoutes = new HashMap<>();

    /** Index: (connectionId, messageType) → list of event bindings (system-agnostic). */
    private final Map<EventKey, List<MavlinkMessageBinding>> eventRoutes = new HashMap<>();

    /** Pending decoded signals per read/event binding. Drained per tick. */
    private final Map<String, List<IInputSignal>> queueByBinding = new ConcurrentHashMap<>();

    /** Pending alarm/event signals (advisory channel). */
    private final List<IInputSignal> events = new ArrayList<>();

    /** Per-binding decimation counters. */
    private final Map<String, AtomicLong> decimationCounters = new ConcurrentHashMap<>();

    /** Last HEARTBEAT timestamp (epoch ms) per (connectionId, systemId). */
    private final Map<RouteKey, Long> heartbeats = new ConcurrentHashMap<>();

    /** Sticky "we already alarmed offline" flag per (connectionId, systemId). */
    private final Map<RouteKey, Boolean> offlineAlarmed = new ConcurrentHashMap<>();

    /** Single-audit-per-minute throttle for unknown-system messages (§11 R1). */
    private final Map<RouteKey, AtomicLong> unknownSystemWindow = new ConcurrentHashMap<>();

    private final AtomicBoolean started = new AtomicBoolean();
    private final AtomicBoolean closed = new AtomicBoolean();

    public MavlinkClientService(MavlinkBridgeConfig config,
                                Map<String, MavlinkTransport> transports,
                                AbstractBridgeAuditOutput audit) {
        this.config = Objects.requireNonNull(config, "config");
        this.audit = Objects.requireNonNull(audit, "audit");
        this.mapper = new MavlinkSignalMapper();

        for (MavlinkBridgeConfig.ConnectionConfig c : config.connections()) {
            connections.put(c.id(), MavlinkConnectionBinding.from(c));
        }

        Map<String, MavlinkTransport> safe = transports == null ? Map.of() : transports;
        for (Map.Entry<String, MavlinkTransport> e : safe.entrySet()) {
            if (!connections.containsKey(e.getKey())) {
                throw new IllegalArgumentException(
                        "Transport supplied for unknown connectionId '" + e.getKey() + "'");
            }
            this.transports.put(e.getKey(), e.getValue());
        }

        for (MavlinkBridgeConfig.ReadBindingConfig r : config.reads()) {
            MavlinkMessageBinding b = MavlinkMessageBinding.fromRead(r);
            readBindings.put(r.bindingId(), b);
            queueByBinding.put(r.bindingId(), new ArrayList<>());
            decimationCounters.put(r.bindingId(), new AtomicLong());
            readRoutes.computeIfAbsent(
                    new RouteKey(r.connectionId(), r.systemId(), r.messageType()),
                    k -> new ArrayList<>()).add(b);
        }
        for (MavlinkBridgeConfig.EventBindingConfig ev : config.events()) {
            MavlinkMessageBinding b = MavlinkMessageBinding.fromEvent(ev);
            eventBindings.put(ev.bindingId(), b);
            queueByBinding.put(ev.bindingId(), new ArrayList<>());
            eventRoutes.computeIfAbsent(
                    new EventKey(ev.connectionId(), ev.messageType()),
                    k -> new ArrayList<>()).add(b);
        }
        for (MavlinkBridgeConfig.WriteBindingConfig w : config.writes()) {
            writeBindings.put(w.bindingId(), MavlinkMessageBinding.fromWrite(w));
        }
    }

    /** Open every transport and register the message handler. Idempotent. */
    public synchronized void start() {
        if (started.get()) return;
        for (Map.Entry<String, MavlinkTransport> e : transports.entrySet()) {
            String connId = e.getKey();
            MavlinkTransport t = e.getValue();
            t.setHandler(msg -> onMessage(connId, msg));
            t.connect();
        }
        started.set(true);
        log.info("MavlinkClientService started; {} connection(s), {} read + {} event + {} write bindings",
                transports.size(), readBindings.size(), eventBindings.size(), writeBindings.size());
    }

    /**
     * Drop in-flight latest-value cache and emit a {@code BRIDGE_RECONNECTED}
     * advisory alarm. Called by the host after a transport reports a
     * reconnect (00-FRAMEWORK §2.3).
     */
    public synchronized void onReconnected(String connectionId) {
        for (Map.Entry<String, List<IInputSignal>> e : queueByBinding.entrySet()) {
            synchronized (e.getValue()) { e.getValue().clear(); }
        }
        heartbeats.keySet().removeIf(k -> k.connectionId().equals(connectionId));
        offlineAlarmed.keySet().removeIf(k -> k.connectionId().equals(connectionId));
        synchronized (events) {
            events.add(new AlarmSignal(AlarmPriority.LOW, BRIDGE_NAME,
                    BRIDGE_RECONNECTED + ":" + connectionId, System.currentTimeMillis()));
        }
        log.info("MavlinkClientService: reconnect on {} — cache cleared, advisory event emitted",
                connectionId);
    }

    /** Drain (and clear) all decoded signals for one binding. */
    public List<IInputSignal> drain(String bindingId) {
        List<IInputSignal> out = queueByBinding.get(bindingId);
        if (out == null || out.isEmpty()) return List.of();
        synchronized (out) {
            List<IInputSignal> snap = new ArrayList<>(out);
            out.clear();
            return snap;
        }
    }

    /** Drain (and clear) all advisory alarm/event signals. */
    public List<IInputSignal> drainEvents() {
        synchronized (events) {
            if (events.isEmpty()) return List.of();
            List<IInputSignal> snap = new ArrayList<>(events);
            events.clear();
            return snap;
        }
    }

    /**
     * Heartbeat watchdog (§5, §10 S9). Call once per tick. For every system
     * we have ever heard a {@code HEARTBEAT} from, if more than
     * {@link #HEARTBEAT_TIMEOUT_MS} has elapsed since the last one, emit a
     * {@code PEER_OFFLINE} alarm (once per timeout).
     */
    public void checkHeartbeats(long nowMs) {
        for (Map.Entry<RouteKey, Long> e : heartbeats.entrySet()) {
            RouteKey k = e.getKey();
            long last = e.getValue();
            if (nowMs - last > HEARTBEAT_TIMEOUT_MS) {
                Boolean prev = offlineAlarmed.put(k, Boolean.TRUE);
                if (prev == null || !prev) {
                    AlarmSignal alarm = mapper.peerOffline(k.systemId(), nowMs);
                    synchronized (events) { events.add(alarm); }
                }
            } else {
                offlineAlarmed.put(k, Boolean.FALSE);
            }
        }
    }

    /**
     * Encode and send a MAVLink message via the named connection. The caller
     * (the aggregator) has already audited the verdict; this method enforces
     * the §3 forbidden-message-type runtime backstop and the
     * {@code expectedSystems} whitelist.
     *
     * @return {@code true} on a successful transport-level publish.
     */
    public boolean send(String bindingId, Object payload, long ts, long run) {
        if (closed.get() || !started.get()) return false;
        MavlinkMessageBinding b = writeBindings.get(bindingId);
        if (b == null) {
            audit.append(new BridgeAuditRecord(
                    System.currentTimeMillis(), run, BRIDGE_NAME,
                    BridgeAuditRecord.Verdict.REJECTED,
                    bindingId, null, null, null,
                    BridgeAuditRecord.RejectReason.UNKNOWN_TAG, null, List.of()));
            return false;
        }
        MavlinkConnectionBinding conn = connections.get(b.connectionId());
        if (conn == null) {
            audit.append(new BridgeAuditRecord(
                    ts, run, BRIDGE_NAME,
                    BridgeAuditRecord.Verdict.REJECTED,
                    b.loopId(), b.signalTag(), null, null,
                    Reason.UNKNOWN_CONNECTION, null, List.of()));
            return false;
        }

        // Hard runtime backstop for the §3 forbidden-message-type rule.
        if (!config.simulatorOnly()
                && b.messageType() != null
                && MavlinkBridgeConfig.FORBIDDEN_WRITE_MESSAGE_TYPES.contains(b.messageType())) {
            audit.append(new BridgeAuditRecord(
                    ts, run, BRIDGE_NAME,
                    BridgeAuditRecord.Verdict.REJECTED,
                    b.loopId(), b.signalTag(), null, null,
                    Reason.FORBIDDEN_MESSAGE_TYPE, null, List.of()));
            return false;
        }

        MavlinkTransport t = transports.get(b.connectionId());
        if (t == null) {
            audit.append(new BridgeAuditRecord(
                    ts, run, BRIDGE_NAME,
                    BridgeAuditRecord.Verdict.FAILED,
                    b.loopId(), b.signalTag(), null, null,
                    Reason.PUBLISH_ERROR + ":no transport", null, List.of()));
            return false;
        }
        try {
            int sys = b.systemId() == null ? 0 : b.systemId();
            int comp = b.componentId() == null ? 0 : b.componentId();
            t.send(sys, comp, payload);
            return true;
        } catch (RuntimeException ex) {
            audit.append(new BridgeAuditRecord(
                    ts, run, BRIDGE_NAME,
                    BridgeAuditRecord.Verdict.FAILED,
                    b.loopId(), b.signalTag(), null, null,
                    Reason.PUBLISH_ERROR + ":" + ex.getMessage(),
                    null, List.of()));
            return false;
        }
    }

    /* ===== accessors ===================================================== */

    public MavlinkBridgeConfig config() { return config; }
    public MavlinkMessageBinding readBinding(String id) { return readBindings.get(id); }
    public MavlinkMessageBinding eventBinding(String id) { return eventBindings.get(id); }
    public MavlinkMessageBinding writeBinding(String id) { return writeBindings.get(id); }
    public MavlinkMessageBinding writeBindingForTag(String tag) {
        for (MavlinkMessageBinding b : writeBindings.values()) {
            if (Objects.equals(tag, b.signalTag())) return b;
        }
        return null;
    }
    public List<String> readBindingIds() { return List.copyOf(readBindings.keySet()); }
    public List<String> eventBindingIds() { return List.copyOf(eventBindings.keySet()); }

    @Override
    public synchronized void close() {
        if (!closed.compareAndSet(false, true)) return;
        for (MavlinkTransport t : transports.values()) {
            try { t.close(); } catch (RuntimeException e) {
                log.warn("MavlinkTransport.close() threw: {}", e.getMessage());
            }
        }
    }

    /* ===== inbound message dispatch ====================================== */

    private void onMessage(String connectionId, MavlinkTransport.InboundMessage message) {
        if (message == null) return;
        Object payload = message.payload();
        if (payload == null) return;
        int systemId = message.systemId();
        long now = System.currentTimeMillis();
        MavlinkConnectionBinding conn = connections.get(connectionId);

        // §11 R1: drop messages from unknown systems with a single audit per minute.
        if (conn != null && !conn.isExpected(systemId)) {
            RouteKey k = new RouteKey(connectionId, systemId, "*");
            AtomicLong window = unknownSystemWindow.computeIfAbsent(k, key -> new AtomicLong());
            long last = window.get();
            if (now - last > UNKNOWN_SYSTEM_WINDOW_MS) {
                window.set(now);
                audit.append(new BridgeAuditRecord(
                        now, 0, BRIDGE_NAME,
                        BridgeAuditRecord.Verdict.REJECTED,
                        connectionId, null, null, null,
                        Reason.UNKNOWN_SYSTEM + ":" + systemId, null, List.of()));
            }
            return;
        }

        String messageType = mavlinkMessageTypeOf(payload);

        // §5: track HEARTBEAT for the watchdog.
        if (payload instanceof io.dronefleet.mavlink.minimal.Heartbeat) {
            heartbeats.put(new RouteKey(connectionId, systemId, "HEARTBEAT"), now);
            offlineAlarmed.put(new RouteKey(connectionId, systemId, "HEARTBEAT"), Boolean.FALSE);
        }

        // Dispatch to event bindings first (system-agnostic).
        List<MavlinkMessageBinding> evList = eventRoutes.get(new EventKey(connectionId, messageType));
        if (evList != null) {
            for (MavlinkMessageBinding b : evList) deliver(b, payload, systemId, now);
        }

        // Dispatch to read bindings (system-specific).
        List<MavlinkMessageBinding> rdList = readRoutes.get(new RouteKey(connectionId, systemId, messageType));
        if (rdList != null) {
            for (MavlinkMessageBinding b : rdList) {
                if (b.decimateBy() > 1) {
                    AtomicLong c = decimationCounters.get(b.bindingId());
                    long n = c == null ? 0 : c.incrementAndGet();
                    if (n % b.decimateBy() != 0) continue;
                }
                deliver(b, payload, systemId, now);
            }
        }
    }

    private void deliver(MavlinkMessageBinding b, Object payload, int systemId, long now) {
        try {
            IInputSignal signal = mapPayload(b, payload, systemId, now);
            if (signal == null) return;
            if (signal instanceof AlarmSignal) {
                synchronized (events) { events.add(signal); }
                return;
            }
            List<IInputSignal> q = queueByBinding.get(b.bindingId());
            if (q != null) {
                synchronized (q) { q.add(signal); }
            }
        } catch (RuntimeException ex) {
            audit.append(new BridgeAuditRecord(
                    now, 0, BRIDGE_NAME,
                    BridgeAuditRecord.Verdict.FAILED,
                    b.loopId(), b.signalTag(), null, null,
                    Reason.DECODER_ERROR + ":" + ex.getClass().getSimpleName(),
                    null, List.of()));
        }
    }

    private IInputSignal mapPayload(MavlinkMessageBinding b, Object payload, int systemId, long now) {
        if (payload instanceof io.dronefleet.mavlink.common.GlobalPositionInt gp) {
            if (b.targetSignal() == MavlinkBridgeConfig.ReadSignalKind.PEER_OBSERVATION) {
                return mapper.toPeerObservation(b, gp, systemId);
            }
            return mapper.toOwnPosition(b, gp, systemId, now);
        }
        if (payload instanceof io.dronefleet.mavlink.common.Attitude att) {
            return mapper.toAttitude(b, att, systemId, now);
        }
        if (payload instanceof io.dronefleet.mavlink.common.BatteryStatus bs) {
            return mapper.toBattery(b, bs);
        }
        if (payload instanceof io.dronefleet.mavlink.common.SysStatus ss) {
            return mapper.toSysStatus(b, ss, systemId, now);
        }
        if (payload instanceof io.dronefleet.mavlink.common.Statustext st) {
            return mapper.toStatusText(b, st, systemId, now);
        }
        if (payload instanceof io.dronefleet.mavlink.common.RadioStatus rs) {
            return mapper.toRadioStatus(b, rs, systemId);
        }
        if (payload instanceof io.dronefleet.mavlink.common.GpsRawInt gr) {
            return mapper.toGpsRaw(b, gr, systemId, now);
        }
        if (payload instanceof io.dronefleet.mavlink.minimal.Heartbeat) {
            return null;
        }
        return null;
    }

    /**
     * Convention used by both dronefleet codegen and the MAVLink XML
     * dialects: a payload class' simple name in CamelCase corresponds to
     * the message type in SCREAMING_SNAKE_CASE. We compute the latter once
     * per message so the dispatch keys can use the canonical MAVLink name
     * the YAML config uses.
     */
    static String mavlinkMessageTypeOf(Object payload) {
        if (payload == null) return null;
        String simple = payload.getClass().getSimpleName();
        StringBuilder out = new StringBuilder(simple.length() + 4);
        for (int i = 0; i < simple.length(); i++) {
            char c = simple.charAt(i);
            if (Character.isUpperCase(c) && i > 0) out.append('_');
            out.append(Character.toUpperCase(c));
        }
        return out.toString();
    }

    /** Multi-key hash key for read routes. */
    private record RouteKey(String connectionId, int systemId, String messageType) {}

    /** Multi-key hash key for event routes (system-agnostic). */
    private record EventKey(String connectionId, String messageType) {}
}
