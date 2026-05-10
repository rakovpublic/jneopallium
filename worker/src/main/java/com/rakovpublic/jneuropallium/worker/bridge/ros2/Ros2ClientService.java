/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.ros2;

import com.rakovpublic.jneuropallium.worker.bridge.common.AbstractBridgeAuditOutput;
import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeAuditRecord;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.AlarmPriority;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.AlarmSignal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Owns the ROS 2 connection (rosbridge or rcljava) and the latest-value
 * cache (04-ROS2-DDS.md §4). Read flow: ROS 2 message → mapper → per-binding
 * queue drained by {@link Ros2SensoryInput} / {@link Ros2StateInput} on each
 * tick (00-FRAMEWORK §2.1). Write flow: {@link Ros2AdvisoryOutputAggregator}
 * computes the advisory payload and calls {@link #publish}.
 *
 * <p>Decimation, payload-size capping (§7, §10 R3), and the safety-critical
 * runtime check that no write hits a forbidden actuating topic outside
 * {@code simulatorOnly} mode (§3) all live here.
 *
 * <p>Reconnect (00-FRAMEWORK §2.3) is handled by the host loop calling
 * {@link #onReconnected()} after the transport reports a fresh connect.
 */
public final class Ros2ClientService implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(Ros2ClientService.class);

    public static final String BRIDGE_NAME = "ros2";
    public static final String BRIDGE_RECONNECTED = "BRIDGE_RECONNECTED";

    /** ROS 2 specific reasons in addition to 00-FRAMEWORK §4 vocabulary. */
    public static final class Reason {
        private Reason() {}
        public static final String FORBIDDEN_TOPIC   = "FORBIDDEN_TOPIC";
        public static final String DECODER_ERROR     = "DECODER_ERROR";
        public static final String PUBLISH_ERROR     = "PUBLISH_ERROR";
        public static final String OVERSIZED_PAYLOAD = "OVERSIZED_PAYLOAD";
    }

    private final Ros2BridgeConfig config;
    private final Ros2Transport transport;
    private final Ros2MessageMapper mapper;
    private final AbstractBridgeAuditOutput audit;

    /** Pending decoded signals per read binding. Drained per tick. */
    private final Map<String, List<IInputSignal>> queueByBinding = new ConcurrentHashMap<>();

    /** Pending alarm/event signals (advisory channel). */
    private final List<IInputSignal> events = new ArrayList<>();

    /** Resolved bindings keyed by bindingId. */
    private final Map<String, Ros2TopicBinding> readBindings = new HashMap<>();
    private final Map<String, Ros2TopicBinding> writeBindings = new HashMap<>();

    /** Topic → read binding lookup (rosbridge subscribes are topic-keyed). */
    private final Map<String, Ros2TopicBinding> readByTopic = new HashMap<>();

    /** Per-binding decimation counter. */
    private final Map<String, AtomicLong> decimationCounters = new ConcurrentHashMap<>();

    /** Per-binding minute-windowed warning suppressor for oversized payloads (§10 R3). */
    private final Map<String, AtomicLong> oversizeWindowMs = new ConcurrentHashMap<>();

    private final AtomicBoolean started = new AtomicBoolean();
    private final AtomicBoolean closed = new AtomicBoolean();

    public Ros2ClientService(Ros2BridgeConfig config,
                             Ros2Transport transport,
                             Ros2MessageMapper mapper,
                             AbstractBridgeAuditOutput audit) {
        this.config = Objects.requireNonNull(config, "config");
        this.transport = Objects.requireNonNull(transport, "transport");
        this.mapper = Objects.requireNonNull(mapper, "mapper");
        this.audit = Objects.requireNonNull(audit, "audit");
        for (Ros2BridgeConfig.ReadBindingConfig r : config.reads()) {
            Ros2TopicBinding b = Ros2TopicBinding.fromRead(r);
            readBindings.put(r.bindingId(), b);
            readByTopic.put(r.topic(), b);
            queueByBinding.put(r.bindingId(), new ArrayList<>());
            decimationCounters.put(r.bindingId(), new AtomicLong());
        }
        for (Ros2BridgeConfig.WriteBindingConfig w : config.writes()) {
            writeBindings.put(w.bindingId(), Ros2TopicBinding.fromWrite(w));
        }
    }

    /** Open the ROS 2 connection, register the message handler, subscribe + advertise. Idempotent. */
    public synchronized void start() {
        if (started.get()) return;
        transport.setHandler(this::onMessage);
        transport.connect();
        for (Ros2TopicBinding r : readBindings.values()) {
            transport.subscribe(r.topic(), r.msgType());
        }
        for (Ros2TopicBinding w : writeBindings.values()) {
            transport.advertise(w.topic(), w.msgType());
        }
        started.set(true);
        log.info("Ros2ClientService started; {} read + {} write bindings",
                readBindings.size(), writeBindings.size());
    }

    /**
     * Drop in-flight latest-value cache and emit a {@code BRIDGE_RECONNECTED}
     * alarm. Called by the host after the transport reports a reconnect
     * (00-FRAMEWORK §2.3).
     */
    public synchronized void onReconnected() {
        for (List<IInputSignal> q : queueByBinding.values()) {
            synchronized (q) { q.clear(); }
        }
        synchronized (events) {
            events.add(new AlarmSignal(AlarmPriority.LOW, BRIDGE_NAME,
                    BRIDGE_RECONNECTED, System.currentTimeMillis()));
        }
        log.info("Ros2ClientService: reconnect — cache cleared, advisory event emitted");
    }

    /** Drain (and clear) all decoded signals for one read binding. */
    public List<IInputSignal> drain(String bindingId) {
        List<IInputSignal> out = queueByBinding.get(bindingId);
        if (out == null || out.isEmpty()) return List.of();
        synchronized (out) {
            List<IInputSignal> snap = new ArrayList<>(out);
            out.clear();
            return snap;
        }
    }

    /** Drain (and clear) all decoded alarm/event signals. */
    public List<IInputSignal> drainEvents() {
        synchronized (events) {
            if (events.isEmpty()) return List.of();
            List<IInputSignal> snap = new ArrayList<>(events);
            events.clear();
            return snap;
        }
    }

    /**
     * Publish a JSON-encoded ROS 2 message to the configured topic. The
     * caller (the aggregator) has already clamped, rate-limited, and
     * audited the proposed value. Returns {@code true} on a successful
     * transport-level publish; on failure an audit record is appended and
     * {@code false} is returned. Per §3 and §7, a write to a forbidden
     * topic is rejected here as the runtime backstop.
     */
    public boolean publish(String bindingId, String json, long ts, long run, String signalTag) {
        if (closed.get() || !started.get()) return false;
        Ros2TopicBinding b = writeBindings.get(bindingId);
        if (b == null) {
            audit.append(new BridgeAuditRecord(
                    System.currentTimeMillis(), run, BRIDGE_NAME,
                    BridgeAuditRecord.Verdict.REJECTED,
                    bindingId, signalTag, null, null,
                    BridgeAuditRecord.RejectReason.UNKNOWN_TAG, null, List.of()));
            return false;
        }
        // Hard runtime backstop for the §3 forbidden-topic rule.
        if (!config.simulatorOnly()
                && Ros2BridgeConfig.FORBIDDEN_WRITE_TOPICS.contains(b.topic())) {
            audit.append(new BridgeAuditRecord(
                    ts, run, BRIDGE_NAME,
                    BridgeAuditRecord.Verdict.REJECTED,
                    b.loopId(), signalTag, null, null,
                    Reason.FORBIDDEN_TOPIC, null, List.of()));
            return false;
        }
        try {
            transport.publish(b.topic(), json);
            return true;
        } catch (RuntimeException ex) {
            audit.append(new BridgeAuditRecord(
                    ts, run, BRIDGE_NAME,
                    BridgeAuditRecord.Verdict.FAILED,
                    b.loopId(), signalTag, null, null,
                    Reason.PUBLISH_ERROR + ":" + ex.getMessage(),
                    null, List.of()));
            return false;
        }
    }

    /** Publish a raw JSON string — used by the audit-mirror channel. */
    public boolean publishRaw(String topic, String json) {
        if (closed.get() || !started.get()) return false;
        try {
            transport.publish(topic, json);
            return true;
        } catch (RuntimeException ex) {
            log.warn("publishRaw to {} failed: {}", topic, ex.getMessage());
            return false;
        }
    }

    public Ros2BridgeConfig config() { return config; }
    public Ros2TopicBinding readBinding(String bindingId) { return readBindings.get(bindingId); }
    public Ros2TopicBinding writeBinding(String bindingId) { return writeBindings.get(bindingId); }
    public Ros2TopicBinding writeBindingForTag(String tag) {
        for (Ros2TopicBinding b : writeBindings.values()) {
            if (Objects.equals(tag, b.signalTag())) return b;
        }
        return null;
    }

    @Override
    public synchronized void close() {
        if (!closed.compareAndSet(false, true)) return;
        try { transport.close(); } catch (RuntimeException e) {
            log.warn("Ros2Transport.close() threw: {}", e.getMessage());
        }
    }

    /* ===== inbound message dispatch ============================================ */

    private void onMessage(Ros2Transport.InboundMessage msg) {
        Ros2TopicBinding b = readByTopic.get(msg.topic());
        if (b == null) return;
        try {
            // §10 R3: drop oversized payloads with a single audit per minute per binding.
            int len = msg.json() == null ? 0 : msg.json().length();
            if (b.maxPayloadBytes() > 0 && len > b.maxPayloadBytes()) {
                long now = System.currentTimeMillis();
                AtomicLong window = oversizeWindowMs.computeIfAbsent(
                        b.bindingId(), k -> new AtomicLong());
                long last = window.get();
                if (now - last > 60_000L) {
                    window.set(now);
                    audit.append(new BridgeAuditRecord(
                            now, 0, BRIDGE_NAME,
                            BridgeAuditRecord.Verdict.REJECTED,
                            b.loopId(), b.signalTag(), null, null,
                            Reason.OVERSIZED_PAYLOAD + ":" + len, null, List.of()));
                }
                return;
            }

            // §7 / §10 S10: decimate.
            if (b.decimateBy() > 1) {
                AtomicLong c = decimationCounters.get(b.bindingId());
                long n = c == null ? 0 : c.incrementAndGet();
                if (n % b.decimateBy() != 0) return;
            }

            IInputSignal signal = mapper.fromRosbridgeEnvelope(b, msg.json());
            if (signal == null) return;
            if (signal instanceof AlarmSignal) {
                synchronized (events) { events.add(signal); }
            } else {
                List<IInputSignal> q = queueByBinding.get(b.bindingId());
                if (q != null) synchronized (q) { q.add(signal); }
            }
        } catch (Ros2MessageMapper.SignalMapperException ex) {
            audit.append(new BridgeAuditRecord(
                    System.currentTimeMillis(), 0, BRIDGE_NAME,
                    BridgeAuditRecord.Verdict.FAILED,
                    b.loopId(), b.signalTag(), null, null,
                    Reason.DECODER_ERROR + ":" + ex.getMessage(),
                    null, List.of()));
        }
    }
}
