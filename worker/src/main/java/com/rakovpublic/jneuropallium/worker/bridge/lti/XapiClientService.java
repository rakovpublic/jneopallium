/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.lti;

import com.fasterxml.jackson.databind.JsonNode;
import com.rakovpublic.jneuropallium.worker.bridge.common.AbstractBridgeAuditOutput;
import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeAuditRecord;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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
 * xAPI LRS polling client + per-binding cache (14-LTI-XAPI.md §4 architecture
 * diagram, §7 package layout).
 *
 * <p>Owns:
 * <ul>
 *   <li>The {@link XapiTransport} seam — pulls statements from the LRS
 *       (PULL mode) or pushes them into the bridge's queue when the
 *       bridge itself is the LRS endpoint (PUSH mode).</li>
 *   <li>Per-binding queues of decoded {@link IInputSignal}s, drained by
 *       the {@link XapiResponseInput} / {@link XapiEngagementInput} /
 *       {@link XapiAffectInput} adapters once per tick.</li>
 *   <li>Pseudonymisation — applied by {@link XapiStatementMapper} so
 *       queues never carry a raw learner identifier.</li>
 * </ul>
 */
public final class XapiClientService implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(XapiClientService.class);

    /** Bridge name carried in every audit record (00-FRAMEWORK §4). */
    public static final String BRIDGE_NAME = "lti";

    /** Default per-binding ring-buffer cap so a slow pipeline cannot exhaust heap. */
    public static final int DEFAULT_RING_BUFFER = 4096;

    private final LtiBridgeConfig config;
    private final XapiTransport transport;
    private final AbstractBridgeAuditOutput audit;
    private final XapiStatementMapper mapper;
    private final PseudonymService pseudonyms;

    private final Map<String, XapiVerbBinding> bindings = new LinkedHashMap<>();
    private final Map<String, Deque<IInputSignal>> queueByBinding = new ConcurrentHashMap<>();
    private final List<IInputSignal> events = new ArrayList<>();

    private final AtomicBoolean started = new AtomicBoolean();
    private final AtomicBoolean closed = new AtomicBoolean();

    public XapiClientService(LtiBridgeConfig config,
                             XapiTransport transport,
                             AbstractBridgeAuditOutput audit) {
        this.config = Objects.requireNonNull(config, "config");
        this.transport = Objects.requireNonNull(transport, "transport");
        this.audit = Objects.requireNonNull(audit, "audit");
        this.pseudonyms = PseudonymService.fromConfig(config.privacy());
        this.mapper = new XapiStatementMapper(pseudonyms, config.privacy().redactFreeText());
    }

    /** §6 — register every read binding. */
    public synchronized void start() {
        if (started.get() || closed.get()) return;
        for (LtiBridgeConfig.ReadBindingConfig r : config.reads()) {
            XapiVerbBinding b = XapiVerbBinding.fromConfig(r);
            bindings.put(b.bindingId(), b);
            queueByBinding.put(b.bindingId(), new ArrayDeque<>());
        }
        started.set(true);
        log.info("XapiClientService started; mode={} bindings={} pseudonymise={}",
                config.xapi() == null ? "<none>" : config.xapi().mode(),
                bindings.size(), pseudonyms.isEnabled());
    }

    /**
     * Run one polling cycle. For every read binding, issue the configured
     * xAPI {@code statements?verb=...} query through the transport and
     * decode every statement into typed signals.
     *
     * <p>Failures are logged and audited but do not propagate — a single
     * unreachable LRS should not stop the rest of the cohort.
     */
    public synchronized void poll() {
        if (!isStarted()) return;
        if (config.xapi() == null || config.xapi().mode() != LtiBridgeConfig.XapiMode.PULL) {
            return; // PUSH mode: statements arrive via acceptStatement.
        }
        if (!transport.isReady()) {
            log.debug("XapiClientService.poll: transport not ready, skipping cycle");
            return;
        }
        for (XapiVerbBinding b : bindings.values()) {
            pollBinding(b);
        }
        int redacted = mapper.drainRedactionCount();
        if (redacted > 0) {
            audit.append(new BridgeAuditRecord(
                    System.currentTimeMillis(), 0, BRIDGE_NAME,
                    BridgeAuditRecord.Verdict.APPLIED, null, null, null, null,
                    "FREE_TEXT_REDACTED:count=" + redacted, null, List.of()));
        }
    }

    /**
     * Accept a statement that arrived via the bridge's PUSH endpoint
     * (14-LTI-XAPI.md §4 — "bridge acts as the LRS endpoint"). Routes the
     * statement to every read binding whose verb matches.
     */
    public synchronized void acceptStatement(JsonNode statement) {
        if (!isStarted() || statement == null || statement.isNull()) return;
        String verb = statement.get("verb") == null ? null
                : statement.get("verb").get("id") == null ? null
                : statement.get("verb").get("id").asText();
        for (XapiVerbBinding b : bindings.values()) {
            if (verb != null && verb.equals(b.xapiVerb())) {
                IInputSignal sig = mapper.map(statement, b.targetSignal());
                if (sig != null) enqueue(b.bindingId(), sig);
            }
        }
    }

    private void pollBinding(XapiVerbBinding b) {
        String query = "statements?verb=" + urlEncode(b.xapiVerb());
        try {
            JsonNode response = transport.pollStatements(query);
            int decoded = 0;
            for (JsonNode stmt : mapper.statements(response)) {
                IInputSignal signal = mapper.map(stmt, b.targetSignal());
                if (signal == null) continue;
                enqueue(b.bindingId(), signal);
                decoded++;
            }
            log.debug("XapiClientService: binding={} verb={} decoded={}",
                    b.bindingId(), b.xapiVerb(), decoded);
        } catch (IOException | RuntimeException e) {
            audit.append(new BridgeAuditRecord(
                    System.currentTimeMillis(), 0, BRIDGE_NAME,
                    BridgeAuditRecord.Verdict.FAILED, b.loopId(), b.signalTag(),
                    null, null,
                    BridgeAuditRecord.RejectReason.EXCEPTION + ":" + e.getClass().getSimpleName(),
                    null, List.of()));
            log.warn("XapiClientService: poll '{}' failed: {}", query, e.getMessage());
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

    /** Drain bridge-level events (e.g. reconnect advisories). */
    public List<IInputSignal> drainEvents() {
        synchronized (events) {
            if (events.isEmpty()) return List.of();
            List<IInputSignal> snap = new ArrayList<>(events);
            events.clear();
            return snap;
        }
    }

    public final boolean isStarted() { return started.get() && !closed.get(); }

    public Map<String, XapiVerbBinding> bindings() { return Map.copyOf(bindings); }

    public LtiBridgeConfig config() { return config; }

    public PseudonymService pseudonymService() { return pseudonyms; }

    public XapiStatementMapper statementMapper() { return mapper; }

    public XapiTransport transport() { return transport; }

    public AbstractBridgeAuditOutput audit() { return audit; }

    @Override
    public synchronized void close() {
        if (!closed.compareAndSet(false, true)) return;
        try { transport.close(); } catch (RuntimeException e) {
            log.warn("XapiTransport close threw: {}", e.getMessage());
        }
    }

    private static String urlEncode(String s) {
        return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8);
    }
}
