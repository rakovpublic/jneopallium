/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.iec61850;

import com.rakovpublic.jneuropallium.worker.bridge.common.AbstractBridgeAuditOutput;
import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeAuditRecord;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * IEC 61850 bridge polling client + per-binding cache
 * (11-IEC61850.md §4, §7).
 *
 * <p>Owns:
 * <ul>
 *   <li>The {@link Iec61850MmsClient} seam — read-only access to the IEDs
 *       (§3, §4 diagram). No write surface exists.</li>
 *   <li>Per-binding queues of decoded signals (one per read binding,
 *       drained by {@link Iec61850MeasurementInput}; one per event
 *       binding, drained by {@link Iec61850EventInput}).</li>
 *   <li>The parsed {@link SclParser.SclModel} per IED — ground-truth data
 *       model required by §6 ("bridge fails fast at startup if SCL is
 *       missing or malformed").</li>
 *   <li>Active Report Control Block subscriptions for the events bindings.</li>
 * </ul>
 *
 * <p>The service has no write methods. There is no aggregator class in
 * this package (§7) and the MMS client seam exposes no write surface — a
 * code path that pushes data back to an IED cannot be expressed inside
 * the bridge.
 */
public final class Iec61850ClientService implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(Iec61850ClientService.class);

    /** Bridge name carried in every audit record (00-FRAMEWORK §4). */
    public static final String BRIDGE_NAME = "iec61850";

    /** Default per-binding ring-buffer cap. */
    public static final int DEFAULT_RING_BUFFER = 4096;

    private final Iec61850BridgeConfig config;
    private final Iec61850MmsClient mms;
    private final AbstractBridgeAuditOutput audit;

    private final Map<String, SclParser.SclModel> sclByIed = new LinkedHashMap<>();
    private final Map<String, Iec61850DaBinding> readBindings = new LinkedHashMap<>();
    private final Map<String, Iec61850BridgeConfig.ReportEventConfig> eventBindings = new LinkedHashMap<>();
    private final Map<String, Deque<IInputSignal>> queueByBinding = new ConcurrentHashMap<>();
    private final List<AutoCloseable> subscriptions = new ArrayList<>();

    private final AtomicBoolean started = new AtomicBoolean();
    private final AtomicBoolean closed = new AtomicBoolean();

    public Iec61850ClientService(Iec61850BridgeConfig config,
                                 Iec61850MmsClient mms,
                                 AbstractBridgeAuditOutput audit) {
        this.config = Objects.requireNonNull(config, "config");
        this.mms = Objects.requireNonNull(mms, "mms");
        this.audit = Objects.requireNonNull(audit, "audit");
    }

    /**
     * Parse every configured IED's SCL file, register read bindings,
     * subscribe to event bindings, and prepare per-binding queues. Fails
     * fast at the first missing or malformed SCL — there is no fallback
     * (§6 last line).
     */
    public synchronized void start() throws IOException {
        if (started.get() || closed.get()) return;
        for (Iec61850BridgeConfig.IedConfig ied : config.ied()) {
            SclParser.SclModel model = SclParser.parseFile(Paths.get(ied.sclFile()));
            sclByIed.put(ied.id(), model);
        }
        for (Iec61850BridgeConfig.DaReadConfig r : config.reads()) {
            Iec61850DaBinding b = Iec61850DaBinding.fromConfig(r);
            readBindings.put(b.bindingId(), b);
            queueByBinding.put(b.bindingId(), new ArrayDeque<>());
        }
        for (Iec61850BridgeConfig.ReportEventConfig e : config.events()) {
            eventBindings.put(e.bindingId(), e);
            queueByBinding.put(e.bindingId(), new ArrayDeque<>());
            try {
                AutoCloseable sub = mms.subscribeReport(
                        e.iedId(), e.reportControlBlock(),
                        report -> onReport(e, report));
                subscriptions.add(sub);
            } catch (IOException io) {
                audit.append(new BridgeAuditRecord(
                        System.currentTimeMillis(), 0, BRIDGE_NAME,
                        BridgeAuditRecord.Verdict.FAILED, e.bindingId(),
                        e.reportControlBlock(), null, null,
                        BridgeAuditRecord.RejectReason.EXCEPTION + ":SUBSCRIBE",
                        null, List.of()));
                throw io;
            }
        }
        started.set(true);
        log.info("Iec61850ClientService started; ieds={} reads={} events={}",
                sclByIed.size(), readBindings.size(), eventBindings.size());
    }

    /**
     * Run one polling cycle over the read bindings. Each binding reads
     * one Data Attribute from its IED and, if a value is present, enqueues
     * a mapped signal. MMS read errors are isolated per binding — one
     * binding failing must not stop the cycle for the others.
     */
    public synchronized void poll() {
        if (!isStarted()) return;
        if (!mms.isReady()) {
            log.debug("Iec61850ClientService.poll: MMS not ready, skipping cycle");
            return;
        }
        for (Iec61850DaBinding b : readBindings.values()) {
            try {
                Iec61850MmsClient.MmsRead read = mms.readDa(b.iedId(), b.daPath());
                IInputSignal mapped = Iec61850SignalMapper.mapDaRead(b, read);
                if (mapped != null) enqueue(b.bindingId(), mapped);
            } catch (IOException | RuntimeException e) {
                audit.append(new BridgeAuditRecord(
                        System.currentTimeMillis(), 0, BRIDGE_NAME,
                        BridgeAuditRecord.Verdict.FAILED, b.loopId(), b.signalTag(),
                        null, null,
                        BridgeAuditRecord.RejectReason.EXCEPTION + ":" + e.getClass().getSimpleName(),
                        null, List.of()));
                log.warn("Iec61850ClientService: binding={} failed: {}",
                        b.bindingId(), e.getMessage());
            }
        }
    }

    private void onReport(Iec61850BridgeConfig.ReportEventConfig event,
                          Iec61850MmsClient.MmsReport report) {
        try {
            List<IInputSignal> signals = Iec61850SignalMapper.mapReport(
                    event, report, event.bindingId());
            for (IInputSignal s : signals) enqueue(event.bindingId(), s);
        } catch (RuntimeException ex) {
            audit.append(new BridgeAuditRecord(
                    System.currentTimeMillis(), 0, BRIDGE_NAME,
                    BridgeAuditRecord.Verdict.FAILED, event.bindingId(),
                    event.reportControlBlock(), null, null,
                    BridgeAuditRecord.RejectReason.EXCEPTION + ":REPORT",
                    null, List.of()));
            log.warn("Iec61850ClientService: report handler for binding={} threw: {}",
                    event.bindingId(), ex.getMessage());
        }
    }

    private void enqueue(String bindingId, IInputSignal signal) {
        Deque<IInputSignal> q = queueByBinding.get(bindingId);
        if (q == null) return;
        synchronized (q) {
            int dropped = 0;
            while (q.size() >= DEFAULT_RING_BUFFER) {
                q.pollFirst();
                dropped++;
            }
            q.addLast(signal);
            if (dropped > 0) {
                audit.append(new BridgeAuditRecord(
                        System.currentTimeMillis(), 0, BRIDGE_NAME,
                        BridgeAuditRecord.Verdict.REJECTED, bindingId, null, null, null,
                        "RING_BUFFER_OVERFLOW:dropped=" + dropped, null, List.of()));
            }
        }
    }

    /** Drain (and clear) the decoded signal queue for one binding. */
    public List<IInputSignal> drain(String bindingId) {
        Deque<IInputSignal> q = queueByBinding.get(bindingId);
        if (q == null || q.isEmpty()) return List.of();
        synchronized (q) {
            List<IInputSignal> snap = new ArrayList<>(q);
            q.clear();
            return snap;
        }
    }

    public boolean isStarted() { return started.get() && !closed.get(); }

    public Map<String, Iec61850DaBinding> readBindings() { return Map.copyOf(readBindings); }

    public Map<String, Iec61850BridgeConfig.ReportEventConfig> eventBindings() {
        return Map.copyOf(eventBindings);
    }

    /** Parsed SCL model for an IED, or {@code null} if no such IED is configured. */
    public SclParser.SclModel scl(String iedId) { return sclByIed.get(iedId); }

    public Iec61850BridgeConfig config() { return config; }

    @Override
    public synchronized void close() {
        if (!closed.compareAndSet(false, true)) return;
        for (AutoCloseable sub : subscriptions) {
            try { sub.close(); } catch (Exception e) {
                log.warn("Iec61850ClientService: subscription close threw: {}", e.getMessage());
            }
        }
        subscriptions.clear();
        try { mms.close(); } catch (RuntimeException e) {
            log.warn("Iec61850MmsClient close threw: {}", e.getMessage());
        }
    }

    @SuppressWarnings("unused")
    private static Path requirePath(String s) { return Paths.get(s); }
}
