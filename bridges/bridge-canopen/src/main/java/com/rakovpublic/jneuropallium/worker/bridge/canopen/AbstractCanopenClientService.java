/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.canopen;

import com.rakovpublic.jneuropallium.worker.bridge.common.AbstractBridgeAuditOutput;
import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeAuditRecord;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.AlarmPriority;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.Quality;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.AlarmSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.BatchStateSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MeasurementSignal;
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
 * Shared orchestration for every CANopen platform backend
 * (13-CANOPEN.md §4 architecture diagram). Owns:
 *
 * <ul>
 *   <li>Binding tables (read / event / write) and the COB-ID-keyed
 *       routing index.</li>
 *   <li>Per-node heartbeat watchdog (§5, §10 S9).</li>
 *   <li>Per-node "online" flag (§5: {@code Quality.UNCERTAIN} when a
 *       node is offline).</li>
 *   <li>The runtime backstop on the {@code writeIndexAllowList}
 *       (§3, §6, §10 R3).</li>
 *   <li>Audit emission for unknown-binding sends, decoder errors, and
 *       publish failures.</li>
 * </ul>
 *
 * <p>Subclasses provide raw frame I/O via {@link #sendRawFrame(CanFrame)}
 * and call {@link #onCanFrame(CanFrame)} from their reader thread (or, for
 * tests, directly from the test method).
 */
public abstract class AbstractCanopenClientService implements CanopenClientService {

    private static final Logger log = LoggerFactory.getLogger(AbstractCanopenClientService.class);

    /** CANopen-specific reasons in addition to 00-FRAMEWORK §4 vocabulary. */
    public static final class Reason {
        private Reason() {}
        public static final String FORBIDDEN_OD_INDEX  = "FORBIDDEN_OD_INDEX";
        public static final String NOT_ON_ALLOW_LIST   = "NOT_ON_ALLOW_LIST";
        public static final String NODE_OFFLINE        = "NODE_OFFLINE";
        public static final String DECODER_ERROR       = "DECODER_ERROR";
        public static final String PUBLISH_ERROR       = "PUBLISH_ERROR";
        public static final String SDO_ABORT           = "SDO_ABORT";
        public static final String UNKNOWN_BINDING     = "UNKNOWN_BINDING";
    }

    private final CanopenBridgeConfig config;
    private final AbstractBridgeAuditOutput audit;
    private final CanopenSignalMapper mapper = new CanopenSignalMapper();

    /** Resolved bindings keyed by bindingId. */
    private final Map<String, CanopenNodeBinding> readBindings = new LinkedHashMap<>();
    private final Map<String, CanopenNodeBinding> eventBindings = new LinkedHashMap<>();
    private final Map<String, CanopenNodeBinding> writeBindings = new LinkedHashMap<>();

    /**
     * Index: COB-ID → bindings to fire for that COB. Built from
     * (nodeId, pdoSource): TPDO1=0x180+n, TPDO2=0x280+n, TPDO3=0x380+n,
     * TPDO4=0x480+n.
     */
    private final Map<Integer, List<CanopenNodeBinding>> readRoutes = new HashMap<>();

    /**
     * Index for synthetic statusword routing — the bindings that map a
     * {@code BATCH_STATE} CiA-402 statusword PDO. Stored separately so the
     * decoder can also emit a fault {@code AlarmSignal} from bit 3.
     */
    private final Map<Integer, List<CanopenNodeBinding>> stateRoutes = new HashMap<>();

    /** Index: nodeId → EMCY-class event bindings. EMCY COB-ID = 0x080 + nodeId. */
    private final Map<Integer, List<CanopenNodeBinding>> emcyRoutes = new HashMap<>();

    /** Index: nodeId → HEARTBEAT_LOSS-class event bindings. */
    private final Map<Integer, List<CanopenNodeBinding>> heartbeatLossRoutes = new HashMap<>();

    /** Pending decoded signals per binding. Drained per tick. */
    private final Map<String, List<IInputSignal>> queueByBinding = new ConcurrentHashMap<>();

    /** Pending alarm / event signals (advisory channel). */
    private final List<IInputSignal> events = new ArrayList<>();

    /** Per-binding decimation counters. */
    private final Map<String, AtomicLong> decimationCounters = new ConcurrentHashMap<>();

    /** Last heartbeat timestamp per nodeId. */
    private final Map<Integer, Long> heartbeats = new ConcurrentHashMap<>();

    /** Sticky "we already alarmed offline" per nodeId. */
    private final Map<Integer, Boolean> offlineAlarmed = new ConcurrentHashMap<>();

    /**
     * Most-recent decoded {@link BatchStateSignal} per state-binding. The
     * carrier on the queue is a {@link MeasurementSignal} (because
     * {@code BatchStateSignal} is not an {@code IInputSignal}); downstream
     * code that wants the typed phase reads it from here via
     * {@link #lastDriveState(String)}.
     */
    private final Map<String, BatchStateSignal> lastDriveState = new ConcurrentHashMap<>();

    private final AtomicBoolean started = new AtomicBoolean();
    private final AtomicBoolean closed = new AtomicBoolean();

    protected AbstractCanopenClientService(CanopenBridgeConfig config,
                                           AbstractBridgeAuditOutput audit) {
        this.config = Objects.requireNonNull(config, "config");
        this.audit = Objects.requireNonNull(audit, "audit");

        for (CanopenBridgeConfig.ReadBindingConfig r : config.reads()) {
            CanopenNodeBinding b = CanopenNodeBinding.fromRead(r);
            readBindings.put(r.bindingId(), b);
            queueByBinding.put(r.bindingId(), new ArrayList<>());
            decimationCounters.put(r.bindingId(), new AtomicLong());
            int cob = pdoCobId(b.pdoSource(), b.nodeId());
            if (cob >= 0) {
                if (b.targetSignal() == CanopenBridgeConfig.ReadSignalKind.BATCH_STATE) {
                    stateRoutes.computeIfAbsent(cob, k -> new ArrayList<>()).add(b);
                } else {
                    readRoutes.computeIfAbsent(cob, k -> new ArrayList<>()).add(b);
                }
            }
        }
        for (CanopenBridgeConfig.EventBindingConfig ev : config.events()) {
            CanopenNodeBinding b = CanopenNodeBinding.fromEvent(ev);
            eventBindings.put(ev.bindingId(), b);
            queueByBinding.put(ev.bindingId(), new ArrayList<>());
            switch (ev.source()) {
                case EMCY -> emcyRoutes.computeIfAbsent(ev.nodeId(), k -> new ArrayList<>()).add(b);
                case HEARTBEAT_LOSS -> heartbeatLossRoutes.computeIfAbsent(ev.nodeId(), k -> new ArrayList<>()).add(b);
            }
        }
        for (CanopenBridgeConfig.WriteBindingConfig w : config.writes()) {
            writeBindings.put(w.bindingId(), CanopenNodeBinding.fromWrite(w));
        }
    }

    /* ===== platform-specific raw I/O ====================================== */

    /**
     * Send a raw CAN frame on the underlying bus. Implementations MUST
     * NOT throw on protocol-level failures — wrap them and return
     * {@code false}.
     *
     * @return {@code true} on a successful transport-level publish.
     */
    protected abstract boolean sendRawFrame(CanFrame frame);

    /**
     * Open the underlying CAN interface. Default: no-op (test subclasses
     * skip this). Real implementations open a SocketCAN socket / a USB-CAN
     * dongle here.
     */
    protected void openTransport() { /* no-op */ }

    /**
     * Close the underlying CAN interface. Default: no-op. Real
     * implementations release the socket / USB device here.
     */
    protected void closeTransport() { /* no-op */ }

    /* ===== lifecycle ===================================================== */

    @Override
    public synchronized void start() {
        if (started.get() || closed.get()) return;
        openTransport();
        started.set(true);
        log.info("CanopenClientService started; {} read + {} event + {} write bindings on {} ({} bps)",
                readBindings.size(), eventBindings.size(), writeBindings.size(),
                config.canBus().device(), config.canBus().bitrate());
    }

    @Override
    public synchronized void close() {
        if (!closed.compareAndSet(false, true)) return;
        try { closeTransport(); } catch (RuntimeException e) {
            log.warn("Canopen transport close threw: {}", e.getMessage());
        }
    }

    /** Whether the orchestrator has been opened and not yet closed. */
    public final boolean isStarted() { return started.get() && !closed.get(); }

    @Override
    public synchronized void onReconnected() {
        for (Map.Entry<String, List<IInputSignal>> e : queueByBinding.entrySet()) {
            synchronized (e.getValue()) { e.getValue().clear(); }
        }
        heartbeats.clear();
        offlineAlarmed.clear();
        synchronized (events) {
            events.add(new AlarmSignal(AlarmPriority.LOW, BRIDGE_NAME,
                    BRIDGE_RECONNECTED, System.currentTimeMillis()));
        }
        log.info("CanopenClientService: reconnect — cache cleared, advisory event emitted");
    }

    /* ===== read-side ===================================================== */

    @Override
    public List<IInputSignal> drain(String bindingId) {
        List<IInputSignal> out = queueByBinding.get(bindingId);
        if (out == null || out.isEmpty()) return List.of();
        synchronized (out) {
            List<IInputSignal> snap = new ArrayList<>(out);
            out.clear();
            return snap;
        }
    }

    @Override
    public List<IInputSignal> drainEvents() {
        synchronized (events) {
            if (events.isEmpty()) return List.of();
            List<IInputSignal> snap = new ArrayList<>(events);
            events.clear();
            return snap;
        }
    }

    @Override
    public void checkHeartbeats(long nowMs) {
        for (Map.Entry<Integer, Long> e : heartbeats.entrySet()) {
            int nodeId = e.getKey();
            long last = e.getValue();
            if (nowMs - last > HEARTBEAT_TIMEOUT_MS) {
                Boolean prev = offlineAlarmed.put(nodeId, Boolean.TRUE);
                if (prev == null || !prev) {
                    AlarmSignal alarm = mapper.nodeOffline(nodeId, nowMs);
                    synchronized (events) { events.add(alarm); }
                    // Also fire any HEARTBEAT_LOSS event-binding queues.
                    List<CanopenNodeBinding> hl = heartbeatLossRoutes.get(nodeId);
                    if (hl != null) {
                        for (CanopenNodeBinding b : hl) {
                            String tag = (b.signalTagPrefix() == null ? "NODE" : b.signalTagPrefix())
                                    + "." + nodeId;
                            AlarmSignal a = new AlarmSignal(
                                    AlarmPriority.HIGH, tag, "NODE_OFFLINE", nowMs);
                            List<IInputSignal> q = queueByBinding.get(b.bindingId());
                            if (q != null) synchronized (q) { q.add(a); }
                        }
                    }
                }
            } else {
                offlineAlarmed.put(nodeId, Boolean.FALSE);
            }
        }
    }

    /** {@code true} if the node has gone past the heartbeat timeout (used for §5 quality propagation). */
    public final boolean isOffline(int nodeId) {
        return Boolean.TRUE.equals(offlineAlarmed.get(nodeId));
    }

    @Override
    public void onCanFrame(CanFrame frame) {
        if (frame == null) return;
        long now = frame.arrivalTimeMs() == 0 ? System.currentTimeMillis() : frame.arrivalTimeMs();
        int cob = frame.cobId();
        int fc = frame.functionCode();
        int nodeId = frame.nodeId();

        // Heartbeat: COB = 0x700 + nodeId.
        if ((cob & 0x780) == 0x700) {
            heartbeats.put(nodeId, now);
            offlineAlarmed.put(nodeId, Boolean.FALSE);
            return;
        }
        // EMCY: COB = 0x080 + nodeId (function code 1, but with non-zero node).
        if (fc == 1 && nodeId != 0) {
            List<CanopenNodeBinding> list = emcyRoutes.get(nodeId);
            if (list == null) return;
            for (CanopenNodeBinding b : list) {
                try {
                    AlarmSignal a = mapper.toEmcyAlarm(b, frame.data(), now);
                    if (a == null) continue;
                    List<IInputSignal> q = queueByBinding.get(b.bindingId());
                    if (q != null) synchronized (q) { q.add(a); }
                    synchronized (events) { events.add(a); }
                } catch (RuntimeException ex) {
                    auditDecoderError(b, ex, now);
                }
            }
            return;
        }

        // PDO state route (statusword) — emits the raw statusword as a MeasurementSignal
        // (downstream consumers can rebuild the typed BatchStateSignal via
        //  CanopenSignalMapper.toDriveState if they need it; the queue itself is
        //  IInputSignal-typed, and BatchStateSignal is an ISignal not an IInputSignal,
        //  so the carrier here is the numeric statusword) plus an AlarmSignal
        //  whenever bit 3 of the statusword reports DRIVE_FAULT.
        List<CanopenNodeBinding> stateList = stateRoutes.get(cob);
        if (stateList != null) {
            for (CanopenNodeBinding b : stateList) {
                try {
                    int sw = (int) CanopenSignalMapper.readRaw(b.odType(), frame.data(), 0);
                    Quality q = isOffline(b.nodeId()) ? Quality.UNCERTAIN : Quality.GOOD;
                    MeasurementSignal carrier = (MeasurementSignal) mapper.toMeasurement(
                            b, (double) (sw & 0xffff), q, now);
                    List<IInputSignal> queue = queueByBinding.get(b.bindingId());
                    if (queue != null) synchronized (queue) { queue.add(carrier); }
                    BatchStateSignal phase = mapper.toDriveState(b, sw, now);
                    lastDriveState.put(b.bindingId(), phase);
                    AlarmSignal fault = mapper.statuswordFault(b, sw, now);
                    if (fault != null) {
                        synchronized (events) { events.add(fault); }
                    }
                } catch (RuntimeException ex) {
                    auditDecoderError(b, ex, now);
                }
            }
        }

        // PDO scalar route — fires whatever bindings map this COB-ID.
        List<CanopenNodeBinding> rdList = readRoutes.get(cob);
        if (rdList == null) return;
        for (CanopenNodeBinding b : rdList) {
            if (b.decimateBy() > 1) {
                AtomicLong c = decimationCounters.get(b.bindingId());
                long n = c == null ? 0 : c.incrementAndGet();
                if (n % b.decimateBy() != 0) continue;
            }
            try {
                deliverPdoScalar(b, frame.data(), now);
            } catch (RuntimeException ex) {
                auditDecoderError(b, ex, now);
            }
        }
    }

    private void deliverPdoScalar(CanopenNodeBinding b, byte[] payload, long now) {
        double value = mapper.decodeScalar(b, payload);
        Quality q = isOffline(b.nodeId()) ? Quality.UNCERTAIN : Quality.GOOD;
        IInputSignal sig = switch (b.targetSignal()) {
            case PROPRIOCEPTIVE -> mapper.toProprioceptive(b, value, now);
            case EFFICIENCY -> mapper.toEfficiency(b, value);
            case MEASUREMENT -> mapper.toMeasurement(b, value, q, now);
            case ALARM -> mapper.toMeasurement(b, value, q, now);
            case BATCH_STATE -> null; // state route handled it
        };
        if (sig instanceof MeasurementSignal ms && q == Quality.UNCERTAIN) {
            ms.setQuality(Quality.UNCERTAIN);
        }
        if (sig == null) return;
        List<IInputSignal> queue = queueByBinding.get(b.bindingId());
        if (queue != null) synchronized (queue) { queue.add(sig); }
    }

    /* ===== write-side ==================================================== */

    @Override
    public boolean send(String bindingId, double value, long ts, long run) {
        if (!isStarted()) return false;
        CanopenNodeBinding b = writeBindings.get(bindingId);
        if (b == null) {
            audit.append(new BridgeAuditRecord(
                    ts, run, BRIDGE_NAME, BridgeAuditRecord.Verdict.REJECTED,
                    bindingId, null, value, null,
                    Reason.UNKNOWN_BINDING, null, List.of()));
            return false;
        }
        // §3 / §6 / §10 R3 — runtime backstop. Even if the index slipped past
        // the loader (e.g. a hot-reload that bypasses the validator) the
        // platform service must refuse the write.
        if (CanopenBridgeConfig.FORBIDDEN_WRITE_OD_INDICES.contains(b.odIndex())) {
            audit.append(new BridgeAuditRecord(
                    ts, run, BRIDGE_NAME, BridgeAuditRecord.Verdict.REJECTED,
                    b.loopId(), b.signalTag(), value, null,
                    Reason.FORBIDDEN_OD_INDEX + ":0x" + Integer.toHexString(b.odIndex()),
                    null, List.of()));
            return false;
        }
        List<Integer> allowed = config.writeIndexAllowList().get(b.nodeId());
        if (allowed == null || !allowed.contains(b.odIndex())) {
            audit.append(new BridgeAuditRecord(
                    ts, run, BRIDGE_NAME, BridgeAuditRecord.Verdict.REJECTED,
                    b.loopId(), b.signalTag(), value, null,
                    Reason.NOT_ON_ALLOW_LIST + ":(node=0x" + Integer.toHexString(b.nodeId())
                            + ",idx=0x" + Integer.toHexString(b.odIndex()) + ")",
                    null, List.of()));
            return false;
        }
        try {
            byte[] payload = mapper.encodeScalar(b.odType(), value);
            int cob = switch (b.writeVia()) {
                case SDO -> 0x600 + b.nodeId();
                case RPDO -> 0x200 + b.nodeId(); // RPDO1 by default
            };
            byte[] frame = b.writeVia() == CanopenBridgeConfig.WriteVia.SDO
                    ? sdoDownloadExpedited(b, payload)
                    : payload;
            return sendRawFrame(new CanFrame(cob, frame, false, ts));
        } catch (RuntimeException ex) {
            audit.append(new BridgeAuditRecord(
                    ts, run, BRIDGE_NAME, BridgeAuditRecord.Verdict.FAILED,
                    b.loopId(), b.signalTag(), value, null,
                    Reason.PUBLISH_ERROR + ":" + ex.getClass().getSimpleName(),
                    null, List.of()));
            return false;
        }
    }

    /**
     * Build an SDO expedited download frame (CiA-301 §7.2.4.3.3) for up to
     * 4 data bytes. Layout: command-specifier byte + 2 index bytes (LE) +
     * subindex byte + up to 4 data bytes.
     */
    static byte[] sdoDownloadExpedited(CanopenNodeBinding b, byte[] payload) {
        int n = payload.length;
        if (n == 0 || n > 4) {
            throw new IllegalArgumentException(
                    "SDO expedited download requires 1..4 data bytes, got " + n);
        }
        // 0x22 = expedited + size-not-indicated; for size-indicated use 0x23 + ((4-n) << 2).
        int cs = 0x23 | ((4 - n) << 2);
        byte[] frame = new byte[8];
        frame[0] = (byte) cs;
        frame[1] = (byte) (b.odIndex() & 0xff);
        frame[2] = (byte) ((b.odIndex() >>> 8) & 0xff);
        frame[3] = (byte) (b.subIndex() & 0xff);
        System.arraycopy(payload, 0, frame, 4, n);
        return frame;
    }

    /* ===== accessors ===================================================== */

    @Override public CanopenBridgeConfig config() { return config; }
    @Override public CanopenNodeBinding readBinding(String id) { return readBindings.get(id); }
    @Override public CanopenNodeBinding writeBinding(String id) { return writeBindings.get(id); }
    @Override public CanopenNodeBinding writeBindingForTag(String tag) {
        for (CanopenNodeBinding b : writeBindings.values()) {
            if (Objects.equals(tag, b.signalTag())) return b;
        }
        return null;
    }
    @Override public List<String> readBindingIds() { return List.copyOf(readBindings.keySet()); }

    public final List<String> eventBindingIds() { return List.copyOf(eventBindings.keySet()); }
    public final CanopenNodeBinding eventBinding(String id) { return eventBindings.get(id); }
    public final BatchStateSignal lastDriveState(String bindingId) { return lastDriveState.get(bindingId); }
    protected final AbstractBridgeAuditOutput audit() { return audit; }

    /* ===== helpers ======================================================= */

    /** COB-ID for a TPDO source (CiA-301 default ranges). Returns -1 for SDO source. */
    static int pdoCobId(CanopenBridgeConfig.PdoSource source, int nodeId) {
        if (source == null) return -1;
        return switch (source) {
            case TPDO1 -> 0x180 + nodeId;
            case TPDO2 -> 0x280 + nodeId;
            case TPDO3 -> 0x380 + nodeId;
            case TPDO4 -> 0x480 + nodeId;
            case SDO -> -1;
        };
    }

    private void auditDecoderError(CanopenNodeBinding b, RuntimeException ex, long now) {
        audit.append(new BridgeAuditRecord(
                now, 0, BRIDGE_NAME, BridgeAuditRecord.Verdict.FAILED,
                b.loopId(), b.signalTag(), null, null,
                Reason.DECODER_ERROR + ":" + ex.getClass().getSimpleName(),
                null, List.of()));
    }
}
