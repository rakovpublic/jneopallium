/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.lsl;

import com.rakovpublic.jneuropallium.worker.bridge.common.AbstractBridgeAuditOutput;
import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeAuditRecord;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.AlarmPriority;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.bci.CalibrationSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.AlarmSignal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Per-stream inlet client + per-outlet publisher (05-LSL.md §4 architecture
 * diagram, §7 package layout).
 *
 * <p>Owns:
 *
 * <ul>
 *   <li>Inlet resolution (§9 S7) and channel-index resolution against the
 *       configured channel labels (§9 S10 — config-load fails fast on a
 *       missing channel name).</li>
 *   <li>Per-binding ring-buffered queues of decoded {@link IInputSignal}s
 *       (§4 "per-stream chunk cache + ring buffer", §10 R2 — when the ML
 *       pipeline lags the oldest sample is dropped and an audit entry is
 *       appended).</li>
 *   <li>Stream-lost detection (§9 S9 — synthetic
 *       {@code AlarmSignal(LSL_STREAM_LOST)} when an inlet stops being
 *       alive).</li>
 *   <li>Outlet publishing for the advisory egress (§5 egress table).</li>
 * </ul>
 *
 * <p>The platform-specific binding to {@code liblsl-Java} lives behind
 * {@link LslTransport} so unit tests can pump synthetic chunks through the
 * full pipeline without launching mDNS multicast (§9 S7..S11).
 */
public final class LslClientService implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(LslClientService.class);

    /** Bridge name carried in every audit record (00-FRAMEWORK §4). */
    public static final String BRIDGE_NAME = "lsl";

    /** Reconnect / cache-cleared advisory event (00-FRAMEWORK §2.3). */
    public static final String BRIDGE_RECONNECTED = "BRIDGE_RECONNECTED";

    /** Stream-lost event code (05-LSL.md §9 S9). */
    public static final String LSL_STREAM_LOST = "LSL_STREAM_LOST";

    /** LSL local-clock epoch is seconds; we publish nanoseconds on neural signals. */
    public static final long NS_PER_SEC = 1_000_000_000L;

    /** §10 R2 — when a stream is decimated, this many samples may queue per binding. */
    public static final int DEFAULT_RING_BUFFER = 4096;

    private final LslBridgeConfig config;
    private final LslTransport transport;
    private final AbstractBridgeAuditOutput audit;
    private final LslSignalMapper mapper = new LslSignalMapper();

    /** Resolved inlets by binding id. */
    private final Map<String, LslTransport.Inlet> inlets = new LinkedHashMap<>();
    /** Resolved outlets by binding id. */
    private final Map<String, LslTransport.Outlet> outlets = new LinkedHashMap<>();
    /** Resolved bindings by id. */
    private final Map<String, LslStreamBinding> bindings = new LinkedHashMap<>();

    /** Per-binding ring-buffered output queue, drained by inputs once per tick. */
    private final Map<String, Deque<IInputSignal>> queueByBinding = new ConcurrentHashMap<>();
    /** Per-binding ring-buffer caps (so different streams can have different lag tolerances). */
    private final Map<String, Integer> ringCaps = new ConcurrentHashMap<>();
    /** Per-binding decimation counters. */
    private final Map<String, AtomicLong> decimationCounters = new ConcurrentHashMap<>();
    /** Last-seen alive flag per inlet, used to fire LSL_STREAM_LOST exactly once. */
    private final Map<String, Boolean> aliveBefore = new ConcurrentHashMap<>();

    /** Bridge-level event queue (LSL_STREAM_LOST, BRIDGE_RECONNECTED, drops). */
    private final List<IInputSignal> events = new ArrayList<>();

    private final AtomicBoolean started = new AtomicBoolean();
    private final AtomicBoolean closed = new AtomicBoolean();

    public LslClientService(LslBridgeConfig config,
                            LslTransport transport,
                            AbstractBridgeAuditOutput audit) {
        this.config = Objects.requireNonNull(config, "config");
        this.transport = Objects.requireNonNull(transport, "transport");
        this.audit = Objects.requireNonNull(audit, "audit");
    }

    /* ===== lifecycle ====================================================== */

    /**
     * Resolve every {@code reads:} stream and open every {@code writes:}
     * outlet (§9 S7, S10).
     *
     * <p>Fails fast — within {@link LslBridgeConfig.DiscoveryConfig#resolveTimeoutMs()} —
     * if a configured {@code expectedStreams} entry has no live publisher,
     * or if a configured channel label is not present in the resolved
     * stream's channel list (S10).
     */
    public synchronized void start() {
        if (started.get() || closed.get()) return;
        long timeout = config.discovery().resolveTimeoutMs();
        // Resolve every read binding.
        for (LslBridgeConfig.ReadBindingConfig r : config.reads()) {
            LslTransport.Inlet inlet = transport.resolveInlet(r.streamName(), r.streamType(), timeout);
            if (inlet == null) {
                throw new LslTransport.LslTransportException(
                        "LSL bridge: stream '" + r.streamName() + "' (type=" + r.streamType()
                                + ") did not resolve within " + timeout + " ms");
            }
            int[] channelIdx = resolveChannels(r, inlet);
            LslStreamBinding b = LslStreamBinding.fromRead(r, channelIdx);
            bindings.put(r.bindingId(), b);
            inlets.put(r.bindingId(), inlet);
            queueByBinding.put(r.bindingId(), new ArrayDeque<>());
            ringCaps.put(r.bindingId(), Math.max(64, b.ringBufferMaxSamples()));
            decimationCounters.put(r.bindingId(), new AtomicLong());
            aliveBefore.put(r.bindingId(), Boolean.TRUE);
        }
        // §6 expectedStreams: every entry must be present in the resolved set.
        for (String expected : config.discovery().expectedStreams()) {
            boolean found = inlets.values().stream().anyMatch(i -> expected.equals(i.name()));
            if (!found) {
                throw new LslTransport.LslTransportException(
                        "LSL bridge: expected stream '" + expected + "' not present after resolve");
            }
        }
        // Open outlets.
        for (LslBridgeConfig.WriteBindingConfig w : config.writes()) {
            LslTransport.Outlet outlet = transport.openOutlet(
                    w.outletName(), w.type(), w.nominalSrate(), List.of(w.signalTag()));
            outlets.put(w.bindingId(), outlet);
            bindings.put(w.bindingId(), LslStreamBinding.fromWrite(w));
        }
        started.set(true);
        log.info("LslClientService started; {} inlet(s), {} outlet(s)", inlets.size(), outlets.size());
    }

    @Override
    public synchronized void close() {
        if (!closed.compareAndSet(false, true)) return;
        for (LslTransport.Inlet i : inlets.values()) {
            try { i.close(); } catch (RuntimeException e) {
                log.warn("LSL inlet close threw: {}", e.getMessage());
            }
        }
        for (LslTransport.Outlet o : outlets.values()) {
            try { o.close(); } catch (RuntimeException e) {
                log.warn("LSL outlet close threw: {}", e.getMessage());
            }
        }
        try { transport.close(); } catch (RuntimeException e) {
            log.warn("LSL transport close threw: {}", e.getMessage());
        }
    }

    public final boolean isStarted() { return started.get() && !closed.get(); }

    /**
     * Drop every inlet's pending cache and emit a {@code BRIDGE_RECONNECTED}
     * advisory alarm. Called by the host after the underlying transport
     * reports a reconnect (00-FRAMEWORK §2.3).
     */
    public synchronized void onReconnected() {
        for (Deque<IInputSignal> q : queueByBinding.values()) {
            synchronized (q) { q.clear(); }
        }
        synchronized (events) {
            events.add(new AlarmSignal(AlarmPriority.LOW, BRIDGE_NAME,
                    BRIDGE_RECONNECTED, System.currentTimeMillis()));
        }
        log.info("LslClientService: reconnect — cache cleared, advisory event emitted");
    }

    /* ===== read-side ====================================================== */

    /**
     * Pull the next batch of samples from every resolved inlet, decode
     * them through {@link LslSignalMapper}, and enqueue the typed signals
     * onto the per-binding ring buffer. Call once per tick.
     *
     * <p>Per §9 S8 the bridge applies the LSL {@code time_correction()} to
     * every source timestamp before emitting it on a typed signal. Per
     * §9 S9 a transition from "alive" to "not alive" emits exactly one
     * {@code LSL_STREAM_LOST} alarm.
     */
    public synchronized void poll() {
        if (!isStarted()) return;
        for (Map.Entry<String, LslTransport.Inlet> entry : inlets.entrySet()) {
            String bid = entry.getKey();
            LslTransport.Inlet inlet = entry.getValue();
            LslStreamBinding b = bindings.get(bid);
            if (b == null) continue;
            // §9 S9 — stream-loss detection.
            boolean alive = inlet.isAlive();
            Boolean prev = aliveBefore.get(bid);
            if (Boolean.TRUE.equals(prev) && !alive) {
                synchronized (events) {
                    events.add(new AlarmSignal(AlarmPriority.HIGH,
                            tagFor(b), LSL_STREAM_LOST, System.currentTimeMillis()));
                }
            }
            aliveBefore.put(bid, alive);
            if (!alive) continue;
            List<LslTransport.Sample> chunk = inlet.pull(b.chunkLengthSamples());
            if (chunk == null || chunk.isEmpty()) continue;
            double tc = inlet.timeCorrectionSeconds();
            AtomicLong dec = decimationCounters.get(bid);
            int decBy = Math.max(1, b.decimateBy());
            for (LslTransport.Sample s : chunk) {
                long n = dec == null ? 0 : dec.incrementAndGet();
                if (n % decBy != 0) continue;
                long correctedNs = (long) ((s.timestamp() + tc) * NS_PER_SEC);
                long correctedMs = correctedNs / 1_000_000L;
                List<IInputSignal> sigs = decodeOne(b, s, correctedNs, correctedMs);
                if (sigs.isEmpty()) continue;
                Deque<IInputSignal> q = queueByBinding.get(bid);
                int cap = ringCaps.getOrDefault(bid, DEFAULT_RING_BUFFER);
                synchronized (q) {
                    int dropped = 0;
                    for (IInputSignal sig : sigs) {
                        while (q.size() >= cap) {
                            q.pollFirst();
                            dropped++;
                        }
                        q.addLast(sig);
                    }
                    if (dropped > 0) {
                        audit.append(new BridgeAuditRecord(
                                System.currentTimeMillis(), 0, BRIDGE_NAME,
                                BridgeAuditRecord.Verdict.REJECTED,
                                b.loopId(), tagFor(b), null, null,
                                "RING_BUFFER_OVERFLOW:dropped=" + dropped, null, List.of()));
                    }
                }
            }
        }
    }

    /** Drain (and clear) all decoded signals for one binding. */
    public List<IInputSignal> drain(String bindingId) {
        Deque<IInputSignal> q = queueByBinding.get(bindingId);
        if (q == null || q.isEmpty()) return List.of();
        synchronized (q) {
            List<IInputSignal> snap = new ArrayList<>(q);
            q.clear();
            return snap;
        }
    }

    /** Drain (and clear) all advisory event signals (LSL_STREAM_LOST, RECONNECTED, …). */
    public List<IInputSignal> drainEvents() {
        synchronized (events) {
            if (events.isEmpty()) return List.of();
            List<IInputSignal> snap = new ArrayList<>(events);
            events.clear();
            return snap;
        }
    }

    /* ===== write-side ===================================================== */

    /**
     * Publish a marker on the named outlet. The aggregator has already
     * audited the verdict; this method enforces the runtime backstop:
     * the binding must exist and the corresponding outlet must be open.
     */
    public boolean pushMarker(String bindingId, String marker, long ts, long run) {
        if (!isStarted()) return false;
        LslTransport.Outlet o = outlets.get(bindingId);
        LslStreamBinding b = bindings.get(bindingId);
        if (o == null || b == null) {
            audit.append(new BridgeAuditRecord(
                    ts, run, BRIDGE_NAME, BridgeAuditRecord.Verdict.REJECTED,
                    bindingId, null, null, null,
                    "UNKNOWN_BINDING", null, List.of()));
            return false;
        }
        try {
            o.pushMarker(marker, transport.localClockSeconds());
            return true;
        } catch (RuntimeException ex) {
            audit.append(new BridgeAuditRecord(
                    ts, run, BRIDGE_NAME, BridgeAuditRecord.Verdict.FAILED,
                    b.loopId(), b.signalTag(), null, null,
                    "PUBLISH_ERROR:" + ex.getClass().getSimpleName(),
                    null, List.of()));
            return false;
        }
    }

    /** Publish a numeric sample on the named outlet (e.g. SeizureRiskSignal). */
    public boolean pushNumeric(String bindingId, double value, long ts, long run) {
        if (!isStarted()) return false;
        LslTransport.Outlet o = outlets.get(bindingId);
        LslStreamBinding b = bindings.get(bindingId);
        if (o == null || b == null) {
            audit.append(new BridgeAuditRecord(
                    ts, run, BRIDGE_NAME, BridgeAuditRecord.Verdict.REJECTED,
                    bindingId, null, value, null,
                    "UNKNOWN_BINDING", null, List.of()));
            return false;
        }
        try {
            o.pushNumeric(new double[]{value}, transport.localClockSeconds());
            return true;
        } catch (RuntimeException ex) {
            audit.append(new BridgeAuditRecord(
                    ts, run, BRIDGE_NAME, BridgeAuditRecord.Verdict.FAILED,
                    b.loopId(), b.signalTag(), value, null,
                    "PUBLISH_ERROR:" + ex.getClass().getSimpleName(),
                    null, List.of()));
            return false;
        }
    }

    /* ===== accessors ====================================================== */

    public LslBridgeConfig config() { return config; }
    public LslTransport transport() { return transport; }
    public LslStreamBinding binding(String id) { return bindings.get(id); }
    public List<String> readBindingIds() { return List.copyOf(inlets.keySet()); }
    public List<String> writeBindingIds() { return List.copyOf(outlets.keySet()); }

    /** Test hook — visible to the same package for §9 S9 / S10 simulation. */
    LslTransport.Inlet inlet(String bindingId) { return inlets.get(bindingId); }
    /** Test hook — visible to the same package. */
    LslTransport.Outlet outlet(String bindingId) { return outlets.get(bindingId); }
    AbstractBridgeAuditOutput audit() { return audit; }

    /* ===== helpers ======================================================== */

    /**
     * Resolve every configured channel label against the inlet's reported
     * channel layout. Throws on the first missing label (§9 S10).
     */
    private static int[] resolveChannels(LslBridgeConfig.ReadBindingConfig r, LslTransport.Inlet inlet) {
        List<String> available = inlet.channelLabels();
        if (r.channels().isEmpty()) {
            int n = inlet.channelCount();
            int[] all = new int[n];
            for (int i = 0; i < n; i++) all[i] = i;
            return all;
        }
        int[] out = new int[r.channels().size()];
        for (int i = 0; i < r.channels().size(); i++) {
            String want = r.channels().get(i);
            int idx = available.indexOf(want);
            if (idx < 0) {
                throw new LslTransport.LslTransportException(
                        "LSL bridge: stream '" + r.streamName() + "' has no channel '" + want
                                + "'. Available: " + available);
            }
            out[i] = idx;
        }
        return out;
    }

    private List<IInputSignal> decodeOne(LslStreamBinding b, LslTransport.Sample s,
                                         long timestampNs, long timestampMs) {
        if (b.readKind() == LslBridgeConfig.ReadSignalKind.CALIBRATION_MARKER) {
            CalibrationSignal sig = mapper.toCalibration(b, s.marker(), b.bindingId());
            return sig == null ? List.of() : List.of(sig);
        }
        return switch (b.readKind()) {
            case LFP -> mapper.toLfp(b, s.values(), timestampNs, b.bindingId());
            case ECOG -> mapper.toEcog(b, s.values(), timestampNs, b.bindingId());
            case NEURAL_SPIKE -> mapper.toSpike(b, s.values(), timestampNs, b.bindingId());
            case EMG_PROPRIOCEPTIVE -> mapper.toProprioceptive(b, s.values(), timestampMs, b.bindingId());
            case INTEROCEPTIVE -> mapper.toInteroceptive(b, s.values(), b.bindingId());
            case APPRAISAL -> mapper.toAppraisal(b, s.values(), b.bindingId());
            case THERMAL -> mapper.toThermal(b, s.values(), b.bindingId());
            case CALIBRATION_MARKER -> List.of();
        };
    }

    private static String tagFor(LslStreamBinding b) {
        if (b.signalTag() != null) return b.signalTag();
        return (b.signalTagPrefix() == null ? "BCI.LSL" : b.signalTagPrefix())
                + "." + b.bindingId();
    }
}
