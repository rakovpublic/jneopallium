/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.mqtt;

import com.rakovpublic.jneuropallium.worker.bridge.common.AbstractBridgeAuditOutput;
import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeAuditRecord;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.AlarmPriority;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.AlarmSignal;
import org.eclipse.tahu.message.model.Metric;
import org.eclipse.tahu.message.model.SparkplugBPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Owns the MQTT connection, the Sparkplug session-state cache, and the
 * advisory publish queue (02-MQTT-SPARKPLUG.md §4 &amp; §10 R3, R4).
 *
 * <p>Read flow: MQTT message → {@link MqttSignalMapper} → per-binding queue
 * drained by {@link MqttMetricInput} / {@link MqttEventInput} on each tick
 * (00-FRAMEWORK §2.1).
 *
 * <p>Write flow: {@link MqttAdvisoryOutputAggregator} computes the advisory
 * payload, calls {@link #publish}; this method enforces the bounded
 * advisory queue size (02-MQTT-SPARKPLUG.md §10 R4) and audits the result.
 *
 * <p>The HiveMQ client is owned by the {@link MqttTransport} layer; this
 * service is therefore broker-agnostic and tested with an in-memory
 * transport.
 */
public final class MqttClientService implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(MqttClientService.class);

    public static final String BRIDGE_NAME = "mqtt";
    public static final String DEVICE_OFFLINE = "DEVICE_OFFLINE";
    public static final String BRIDGE_RECONNECTED = "BRIDGE_RECONNECTED";

    /** MQTT-bridge specific reasons (in addition to 00-FRAMEWORK §4 vocabulary). */
    public static final class Reason {
        private Reason() {}
        public static final String ADVISORY_QUEUE_FULL = "ADVISORY_QUEUE_FULL";
        public static final String DECODER_ERROR       = "DECODER_ERROR";
        public static final String PUBLISH_ERROR       = "PUBLISH_ERROR";
        public static final String CLAMPED_HIGH        = BridgeAuditRecord.ModifyReason.CLAMPED_HIGH;
        public static final String CLAMPED_LOW         = BridgeAuditRecord.ModifyReason.CLAMPED_LOW;
    }

    private final MqttBridgeConfig config;
    private final MqttTransport transport;
    private final MqttSignalMapper mapper;
    private final SparkplugMetricResolver resolver;
    private final AbstractBridgeAuditOutput audit;

    /** Pending decoded signals per measurement-binding. Drained per tick. */
    private final Map<String, List<IInputSignal>> measurementsByBinding = new ConcurrentHashMap<>();

    /** Pending alarm/event signals (a single dedicated queue: alarms aren't tag-bound). */
    private final List<IInputSignal> events = new ArrayList<>();

    /** Resolved bindings keyed by bindingId. */
    private final Map<String, MqttTopicBinding> readBindings = new HashMap<>();
    private final Map<String, MqttTopicBinding> writeBindings = new HashMap<>();
    private final Map<String, MqttBridgeConfig.WriteBindingConfig> writeConfigs = new HashMap<>();

    /** Plain-MQTT topic → binding lookup. */
    private final Map<String, MqttBridgeConfig.ReadBindingConfig> plainTopicIndex = new HashMap<>();

    /**
     * Sparkplug "world": which devices are currently alive. Key is
     * {@code group/edge[/device]}; presence means we've seen its BIRTH and no
     * subsequent DEATH.
     */
    private final Set<String> aliveDevices = ConcurrentHashMap.newKeySet();

    /** Bounded advisory queue depth. */
    private final AtomicLong advisoryQueueDepth = new AtomicLong();

    private final AtomicBoolean started = new AtomicBoolean();
    private final AtomicBoolean closed = new AtomicBoolean();

    public MqttClientService(MqttBridgeConfig config,
                             MqttTransport transport,
                             MqttSignalMapper mapper,
                             AbstractBridgeAuditOutput audit) {
        this.config = Objects.requireNonNull(config, "config");
        this.transport = Objects.requireNonNull(transport, "transport");
        this.mapper = Objects.requireNonNull(mapper, "mapper");
        this.audit = Objects.requireNonNull(audit, "audit");
        this.resolver = new SparkplugMetricResolver(config.reads());
        for (MqttBridgeConfig.ReadBindingConfig r : config.reads()) {
            readBindings.put(r.bindingId(), MqttTopicBinding.fromRead(r));
            measurementsByBinding.put(r.bindingId(), new ArrayList<>());
            if (r.plainMqttTopic() != null) plainTopicIndex.put(r.plainMqttTopic(), r);
        }
        for (MqttBridgeConfig.WriteBindingConfig w : config.writes()) {
            writeBindings.put(w.bindingId(), MqttTopicBinding.fromWrite(w));
            writeConfigs.put(w.bindingId(), w);
        }
    }

    /** Open the MQTT connection, register the message handler, subscribe. Idempotent. */
    public synchronized void start() {
        if (started.get()) return;
        transport.setHandler(this::onMessage);
        transport.connect();
        for (String filter : SparkplugMetricResolver.subscriptionTopicsFor(config)) {
            transport.subscribe(filter, 1);
        }
        started.set(true);
        log.info("MqttClientService started; {} read + {} write bindings, {} subscriptions",
                config.reads().size(), config.writes().size(),
                SparkplugMetricResolver.subscriptionTopicsFor(config).size());
    }

    /**
     * Drop in-flight latest-value cache and emit a {@code BRIDGE_RECONNECTED}
     * alarm. Called by the host after the transport reports a reconnect
     * (00-FRAMEWORK §2.3).
     */
    public synchronized void onReconnected() {
        aliveDevices.clear();
        synchronized (events) {
            events.add(new AlarmSignal(AlarmPriority.LOW, BRIDGE_NAME,
                    BRIDGE_RECONNECTED, System.currentTimeMillis()));
        }
        log.info("MqttClientService: reconnect — cache cleared, advisory event emitted");
    }

    /** Drain (and clear) all decoded measurement signals for one read binding. */
    public List<IInputSignal> drainMeasurements(String bindingId) {
        List<IInputSignal> out = measurementsByBinding.get(bindingId);
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
     * Publish an advisory record to the configured advisory topic. The
     * payload is encoded according to the binding ({@code sparkplugMetric}
     * configured ⇒ Sparkplug B; otherwise a small JSON document). Returns
     * {@code true} on a successful enqueue.
     */
    public boolean publish(String bindingId, double value, long ts, long run, String signalTag) {
        if (closed.get() || !started.get()) return false;
        MqttBridgeConfig.WriteBindingConfig wc = writeConfigs.get(bindingId);
        MqttTopicBinding b = writeBindings.get(bindingId);
        if (wc == null || b == null) {
            audit.append(new BridgeAuditRecord(
                    System.currentTimeMillis(), run, BRIDGE_NAME,
                    BridgeAuditRecord.Verdict.REJECTED,
                    bindingId, signalTag, value, null,
                    BridgeAuditRecord.RejectReason.UNKNOWN_TAG, null, List.of()));
            return false;
        }

        long current = advisoryQueueDepth.incrementAndGet();
        if (current > config.connection().advisoryQueueSize()) {
            advisoryQueueDepth.decrementAndGet();
            audit.append(new BridgeAuditRecord(
                    System.currentTimeMillis(), run, BRIDGE_NAME,
                    BridgeAuditRecord.Verdict.FAILED,
                    bindingId, signalTag, value, null,
                    Reason.ADVISORY_QUEUE_FULL, null, List.of()));
            return false;
        }
        try {
            byte[] payload;
            if (wc.sparkplugMetric() != null) {
                payload = mapper.encodeSparkplugDouble(wc.sparkplugMetric(), value, ts);
            } else {
                payload = simpleJsonAdvisory(signalTag, value, ts);
            }
            transport.publish(wc.advisoryTopic(), payload, wc.qos(), false);
            return true;
        } catch (RuntimeException ex) {
            audit.append(new BridgeAuditRecord(
                    System.currentTimeMillis(), run, BRIDGE_NAME,
                    BridgeAuditRecord.Verdict.FAILED,
                    bindingId, signalTag, value, null,
                    Reason.PUBLISH_ERROR + ":" + ex.getMessage(), null, List.of()));
            return false;
        } finally {
            advisoryQueueDepth.decrementAndGet();
        }
    }

    /** Publish a raw advisory payload to a topic — used by the audit-mirror channel. */
    public boolean publishRaw(String topic, byte[] payload, int qos) {
        if (closed.get() || !started.get()) return false;
        try {
            transport.publish(topic, payload, qos, false);
            return true;
        } catch (RuntimeException ex) {
            log.warn("publishRaw to {} failed: {}", topic, ex.getMessage());
            return false;
        }
    }

    public MqttBridgeConfig config() { return config; }
    public MqttTopicBinding readBinding(String bindingId) { return readBindings.get(bindingId); }
    public MqttTopicBinding writeBinding(String bindingId) { return writeBindings.get(bindingId); }
    public MqttBridgeConfig.WriteBindingConfig writeConfig(String bindingId) { return writeConfigs.get(bindingId); }
    public MqttTopicBinding writeBindingForTag(String tag) {
        for (MqttTopicBinding b : writeBindings.values()) {
            if (Objects.equals(tag, b.signalTag())) return b;
        }
        return null;
    }

    @Override
    public synchronized void close() {
        if (!closed.compareAndSet(false, true)) return;
        try { transport.close(); } catch (RuntimeException e) {
            log.warn("MqttTransport.close() threw: {}", e.getMessage());
        }
    }

    /* ===== inbound message dispatch ============================================ */

    private void onMessage(MqttTransport.InboundMessage msg) {
        try {
            String topic = msg.topic();
            if (isSparkplug(topic)) {
                handleSparkplug(topic, msg.payload());
            } else {
                handlePlain(topic, msg.payload());
            }
        } catch (MqttSignalMapper.SignalMapperException ex) {
            audit.append(new BridgeAuditRecord(
                    System.currentTimeMillis(), 0, BRIDGE_NAME,
                    BridgeAuditRecord.Verdict.FAILED,
                    null, msg.topic(), null, null,
                    Reason.DECODER_ERROR + ":" + ex.getMessage(), null, List.of()));
        }
    }

    private static boolean isSparkplug(String topic) { return topic.startsWith("spBv1.0/"); }

    /** Sparkplug topic shape: {@code spBv1.0/<group>/<type>/<edge>[/<device>]}. */
    private record SparkplugTopic(String group, String messageType, String edge, String device) {
        static SparkplugTopic parse(String topic) {
            String[] p = topic.split("/");
            if (p.length < 4) return null;
            return new SparkplugTopic(p[1], p[2], p[3], p.length > 4 ? p[4] : null);
        }
    }

    private void handleSparkplug(String topic, byte[] payload) {
        SparkplugTopic st = SparkplugTopic.parse(topic);
        if (st == null) return;
        String type = st.messageType();
        // Retain only the message types the bridge is expected to react to.
        SparkplugBPayload sp;
        switch (type) {
            case "NBIRTH":
            case "DBIRTH":
                sp = mapper.decodeSparkplug(payload);
                handleBirth(st, sp);
                return;
            case "DDATA":
            case "NDATA":
                sp = mapper.decodeSparkplug(payload);
                handleData(st, sp);
                return;
            case "DDEATH":
            case "NDEATH":
                handleDeath(st);
                return;
            default:
                // NCMD/DCMD/STATE/RECORD: this is a subscriber bridge, ignore.
        }
    }

    private void handleBirth(SparkplugTopic st, SparkplugBPayload payload) {
        aliveDevices.add(deviceKey(st));
        // First DDATA after BIRTH is processed normally; BIRTH itself does not produce signals.
        if (payload != null) {
            // BIRTHs may carry initial values; emit them too so the cache reflects current state.
            handleData(st, payload);
        }
    }

    private void handleData(SparkplugTopic st, SparkplugBPayload payload) {
        if (payload == null || payload.getMetrics() == null) return;
        long fallbackTs = payload.getTimestamp() != null
                ? payload.getTimestamp().getTime() : System.currentTimeMillis();
        for (Metric m : payload.getMetrics()) {
            if (m == null || m.getName() == null) continue;
            MqttBridgeConfig.ReadBindingConfig r = resolver.resolve(
                    st.group(), st.edge(), st.device() == null ? "" : st.device(), m.getName());
            if (r == null) continue;
            IInputSignal signal = mapper.toSignal(r, m, fallbackTs);
            if (signal == null) continue;
            if (signal instanceof AlarmSignal) {
                synchronized (events) { events.add(signal); }
            } else {
                List<IInputSignal> q = measurementsByBinding.get(r.bindingId());
                if (q != null) synchronized (q) { q.add(signal); }
            }
        }
    }

    private void handleDeath(SparkplugTopic st) {
        boolean wasAlive = aliveDevices.remove(deviceKey(st));
        if (!wasAlive) return;
        long ts = System.currentTimeMillis();
        synchronized (events) {
            events.add(new AlarmSignal(AlarmPriority.LOW,
                    deviceKey(st), DEVICE_OFFLINE, ts));
        }
    }

    private static String deviceKey(SparkplugTopic st) {
        return st.device() == null
                ? st.group() + "/" + st.edge()
                : st.group() + "/" + st.edge() + "/" + st.device();
    }

    private void handlePlain(String topic, byte[] payload) {
        MqttBridgeConfig.ReadBindingConfig r = plainTopicIndex.get(topic);
        if (r == null) return;
        IInputSignal sig = mapper.fromPlainJson(r, payload, System.currentTimeMillis());
        if (sig == null) return;
        if (sig instanceof AlarmSignal) {
            synchronized (events) { events.add(sig); }
        } else {
            List<IInputSignal> q = measurementsByBinding.get(r.bindingId());
            if (q != null) synchronized (q) { q.add(sig); }
        }
    }

    /* ===== helpers ============================================================ */

    private static byte[] simpleJsonAdvisory(String tag, double value, long ts) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("tag", tag);
        dto.put("target", value);
        dto.put("ts", ts);
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsBytes(dto);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            // Should never happen — DTO is a plain Map.
            throw new MqttTransport.MqttTransportException("json encode failed: " + e.getMessage(), e);
        }
    }

    /** Whether the bridge has seen a BIRTH for this device — used for tests. */
    boolean isDeviceAlive(String group, String edge, String device) {
        return aliveDevices.contains(group + "/" + edge + "/" + device);
    }
}
