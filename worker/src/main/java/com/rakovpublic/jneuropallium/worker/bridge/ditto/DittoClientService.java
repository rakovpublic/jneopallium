/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.ditto;

import com.fasterxml.jackson.databind.JsonNode;
import com.rakovpublic.jneuropallium.worker.bridge.common.AbstractBridgeAuditOutput;
import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeAuditRecord;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.AlarmPriority;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.Quality;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.AlarmSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MeasurementSignal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Owns the Ditto WebSocket subscription, the per-thing online cache, and
 * the advisory write queue (10-DITTO.md §3 &amp; §9).
 *
 * <p>Read flow: a {@link DittoTransport.TwinEvent} is decoded by
 * {@link DittoSignalMapper} into a typed signal and queued per binding;
 * {@link DittoFeatureInput} / {@link DittoEventInput} drain the queues per
 * tick (00-FRAMEWORK §2.1). When a {@code THING_DELETED} event arrives, the
 * thing is removed from the {@link #aliveThings} set; subsequent reads of
 * any of its features carry {@link Quality#UNCERTAIN} (10-DITTO §4).
 *
 * <p>Write flow: {@link DittoAdvisoryOutputAggregator} computes the
 * advisory payload, calls {@link #writeProperty}; this method enforces the
 * bounded advisory queue (00-FRAMEWORK §6 R4 analogue), validates the
 * advisory-prefix rule a second time, and audits the result.
 */
public final class DittoClientService implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(DittoClientService.class);

    public static final String BRIDGE_NAME = "ditto";
    public static final String TWIN_OFFLINE = "TWIN_OFFLINE";
    public static final String BRIDGE_RECONNECTED = "BRIDGE_RECONNECTED";

    /** Ditto-specific reasons (in addition to the 00-FRAMEWORK §4 vocabulary). */
    public static final class Reason {
        private Reason() {}
        public static final String ADVISORY_QUEUE_FULL = "ADVISORY_QUEUE_FULL";
        public static final String DECODER_ERROR       = "DECODER_ERROR";
        public static final String WRITE_ERROR         = "WRITE_ERROR";
        public static final String NON_ADVISORY_FEATURE = "NON_ADVISORY_FEATURE";
    }

    private final DittoBridgeConfig config;
    private final DittoTransport transport;
    private final DittoSignalMapper mapper;
    private final AbstractBridgeAuditOutput audit;

    /** Pending decoded measurement signals per read-bindingId. Drained per tick. */
    private final Map<String, List<IInputSignal>> measurementsByBinding = new ConcurrentHashMap<>();
    /** Pending alarm/event signals (single queue: alarms aren't tag-scoped). */
    private final List<IInputSignal> events = new ArrayList<>();

    private final Map<String, DittoFeatureBinding> readBindings = new HashMap<>();
    private final Map<String, DittoFeatureBinding> writeBindings = new HashMap<>();
    private final Map<String, DittoBridgeConfig.WriteBindingConfig> writeConfigs = new HashMap<>();

    /** Per-binding lookup by feature path (thingId/feature/property). */
    private final Map<String, DittoBridgeConfig.ReadBindingConfig> readByPath = new HashMap<>();
    /** All read bindings grouped by (thingId, feature) for FEATURE_MODIFIED dispatch. */
    private final Map<String, List<DittoBridgeConfig.ReadBindingConfig>> readByFeature = new HashMap<>();

    /** Things currently considered alive. Empty set ⇒ all configured things default to alive. */
    private final Set<String> aliveThings = ConcurrentHashMap.newKeySet();

    private final AtomicLong advisoryQueueDepth = new AtomicLong();
    private final AtomicBoolean started = new AtomicBoolean();
    private final AtomicBoolean closed = new AtomicBoolean();

    public DittoClientService(DittoBridgeConfig config,
                              DittoTransport transport,
                              DittoSignalMapper mapper,
                              AbstractBridgeAuditOutput audit) {
        this.config = Objects.requireNonNull(config, "config");
        this.transport = Objects.requireNonNull(transport, "transport");
        this.mapper = Objects.requireNonNull(mapper, "mapper");
        this.audit = Objects.requireNonNull(audit, "audit");
        for (DittoBridgeConfig.ReadBindingConfig r : config.reads()) {
            readBindings.put(r.bindingId(), DittoFeatureBinding.fromRead(r));
            measurementsByBinding.put(r.bindingId(), new ArrayList<>());
            readByPath.put(DittoFeatureBinding.defaultTag(r.thingId(), r.feature(), r.property()), r);
            readByFeature.computeIfAbsent(r.thingId() + "/" + r.feature(), k -> new ArrayList<>()).add(r);
        }
        for (DittoBridgeConfig.WriteBindingConfig w : config.writes()) {
            writeBindings.put(w.bindingId(), DittoFeatureBinding.fromWrite(w));
            writeConfigs.put(w.bindingId(), w);
        }
        // Configured things start as alive; THING_DELETED removes them.
        aliveThings.addAll(distinctThings());
    }

    /** Open the WS connection, register the handler, subscribe to every configured thing. Idempotent. */
    public synchronized void start() {
        if (started.get()) return;
        transport.setHandler(this::onEvent);
        transport.connect();
        for (String thingId : distinctThings()) {
            transport.subscribe(thingId);
        }
        started.set(true);
        log.info("DittoClientService started; {} read + {} write bindings, subscribed to {} thing(s)",
                config.reads().size(), config.writes().size(), distinctThings().size());
    }

    /** Drop the alive-thing cache and emit a {@code BRIDGE_RECONNECTED} alarm (00-FRAMEWORK §2.3). */
    public synchronized void onReconnected() {
        aliveThings.clear();
        aliveThings.addAll(distinctThings());
        synchronized (events) {
            events.add(new AlarmSignal(AlarmPriority.LOW, BRIDGE_NAME,
                    BRIDGE_RECONNECTED, System.currentTimeMillis()));
        }
        log.info("DittoClientService: reconnect — cache cleared, advisory event emitted");
    }

    public List<IInputSignal> drainMeasurements(String bindingId) {
        List<IInputSignal> q = measurementsByBinding.get(bindingId);
        if (q == null || q.isEmpty()) return List.of();
        synchronized (q) {
            List<IInputSignal> snap = new ArrayList<>(q);
            q.clear();
            return snap;
        }
    }

    public List<IInputSignal> drainEvents() {
        synchronized (events) {
            if (events.isEmpty()) return List.of();
            List<IInputSignal> snap = new ArrayList<>(events);
            events.clear();
            return snap;
        }
    }

    /**
     * Issue a feature-property modify against the Ditto HTTP API. Returns
     * {@code true} on a 2xx response. Refuses (and audits {@code REJECTED})
     * any binding whose target feature is not advisory.
     */
    public boolean writeProperty(String bindingId, double value, long ts, long run, String signalTag) {
        if (closed.get() || !started.get()) return false;
        DittoBridgeConfig.WriteBindingConfig wc = writeConfigs.get(bindingId);
        DittoFeatureBinding b = writeBindings.get(bindingId);
        if (wc == null || b == null) {
            audit.append(new BridgeAuditRecord(
                    System.currentTimeMillis(), run, BRIDGE_NAME,
                    BridgeAuditRecord.Verdict.REJECTED,
                    bindingId, signalTag, value, null,
                    BridgeAuditRecord.RejectReason.UNKNOWN_TAG, null, List.of()));
            return false;
        }
        // Defence-in-depth: even though the loader should have rejected this, refuse
        // again at the runtime boundary (10-DITTO §4 — "Enforced at config-load and at runtime").
        try {
            DittoBridgeConfig.requireAdvisoryPrefix(wc.feature(), wc.bindingId());
        } catch (IllegalArgumentException ex) {
            audit.append(new BridgeAuditRecord(
                    System.currentTimeMillis(), run, BRIDGE_NAME,
                    BridgeAuditRecord.Verdict.REJECTED,
                    b.loopId(), signalTag, value, null,
                    Reason.NON_ADVISORY_FEATURE, null, List.of()));
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
            byte[] body = mapper.encodeFeaturePropertyRestBody(value);
            return transport.putFeatureProperty(wc.thingId(), wc.feature(), wc.property(), body);
        } catch (RuntimeException ex) {
            audit.append(new BridgeAuditRecord(
                    System.currentTimeMillis(), run, BRIDGE_NAME,
                    BridgeAuditRecord.Verdict.FAILED,
                    b.loopId(), signalTag, value, null,
                    Reason.WRITE_ERROR + ":" + ex.getMessage(), null, List.of()));
            return false;
        } finally {
            advisoryQueueDepth.decrementAndGet();
        }
    }

    public DittoBridgeConfig config() { return config; }
    public DittoFeatureBinding readBinding(String bindingId) { return readBindings.get(bindingId); }
    public DittoFeatureBinding writeBinding(String bindingId) { return writeBindings.get(bindingId); }
    public DittoBridgeConfig.WriteBindingConfig writeConfig(String bindingId) { return writeConfigs.get(bindingId); }
    public DittoFeatureBinding writeBindingForTag(String tag) {
        for (DittoFeatureBinding b : writeBindings.values()) {
            if (Objects.equals(tag, b.signalTag())) return b;
        }
        return null;
    }

    /** Test helper — whether the bridge currently considers {@code thingId} alive. */
    public boolean isThingAlive(String thingId) { return aliveThings.contains(thingId); }

    @Override
    public synchronized void close() {
        if (!closed.compareAndSet(false, true)) return;
        try { transport.close(); } catch (RuntimeException e) {
            log.warn("DittoTransport.close() threw: {}", e.getMessage());
        }
    }

    /* ===== inbound twin-event dispatch =================================== */

    private void onEvent(DittoTransport.TwinEvent ev) {
        try {
            switch (ev.type()) {
                case THING_DELETED -> handleThingDeleted(ev.thingId());
                case FEATURE_PROPERTY_MODIFIED, FEATURE_MODIFIED -> handleFeatureChange(ev);
            }
        } catch (DittoSignalMapper.SignalMapperException ex) {
            audit.append(new BridgeAuditRecord(
                    System.currentTimeMillis(), 0, BRIDGE_NAME,
                    BridgeAuditRecord.Verdict.FAILED,
                    null, ev.thingId(), null, null,
                    Reason.DECODER_ERROR + ":" + ex.getMessage(), null, List.of()));
        }
    }

    private void handleThingDeleted(String thingId) {
        boolean wasAlive = aliveThings.remove(thingId);
        if (!wasAlive) return;
        long ts = System.currentTimeMillis();
        synchronized (events) {
            events.add(new AlarmSignal(AlarmPriority.HIGH, thingId, TWIN_OFFLINE, ts));
        }
    }

    private void handleFeatureChange(DittoTransport.TwinEvent ev) {
        JsonNode root = mapper.decode(ev.payload());
        if (root == null) return;
        long ts = mapper.pickTimestamp(root, System.currentTimeMillis());
        boolean offline = !aliveThings.contains(ev.thingId());

        Set<DittoBridgeConfig.ReadBindingConfig> matched = new HashSet<>();
        if (ev.feature() != null) {
            List<DittoBridgeConfig.ReadBindingConfig> byFeature =
                    readByFeature.get(ev.thingId() + "/" + ev.feature());
            if (byFeature != null) matched.addAll(byFeature);
        }
        // Property-specific event: also try the exact path lookup for correctness.
        for (DittoBridgeConfig.ReadBindingConfig r : matched) {
            JsonNode value = mapper.extractPropertyValue(root, r.feature(), r.property());
            IInputSignal sig = mapper.toSignal(r, value, ts, offline);
            if (sig == null) continue;
            if (sig instanceof AlarmSignal) {
                synchronized (events) { events.add(sig); }
            } else {
                List<IInputSignal> q = measurementsByBinding.get(r.bindingId());
                if (q != null) synchronized (q) { q.add(sig); }
            }
        }
    }

    /** Build an alive-thing list from the explicit {@code things} block plus any binding's thingId. */
    private Set<String> distinctThings() {
        Set<String> all = new HashSet<>(config.things());
        for (DittoBridgeConfig.ReadBindingConfig r : config.reads()) all.add(r.thingId());
        for (DittoBridgeConfig.WriteBindingConfig w : config.writes()) all.add(w.thingId());
        return all;
    }
}
