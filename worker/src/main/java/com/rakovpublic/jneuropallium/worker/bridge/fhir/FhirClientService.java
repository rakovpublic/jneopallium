/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.fhir;

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
 * FHIR REST polling client + per-binding cache (06-FHIR.md §4 architecture
 * diagram, §7 package layout).
 *
 * <p>Owns:
 * <ul>
 *   <li>The {@link FhirTransport} seam — read-only access to the FHIR
 *       endpoint (§3 rule 1).</li>
 *   <li>Per-binding queues of decoded {@link IInputSignal}s, drained by
 *       the {@link FhirObservationInput} / {@link FhirMedicationInput} /
 *       {@link FhirConditionInput} adapters once per tick.</li>
 *   <li>Cohort iteration — the search template's {@code {pid}} placeholder
 *       is resolved per cohort patient on every poll (§6 {@code cohort:}).</li>
 *   <li>Pseudonymisation — applied by the {@link FhirResourceMapper} so
 *       the queues never carry a raw cohort identifier.</li>
 * </ul>
 *
 * <p>The service does not write to FHIR. It does not expose a method that
 * could; the {@link FhirTransport} interface deliberately omits any
 * non-{@code GET} operation (§3 rule 1).
 */
public final class FhirClientService implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(FhirClientService.class);

    /** Bridge name carried in every audit record (00-FRAMEWORK §4). */
    public static final String BRIDGE_NAME = "fhir";

    /** Default per-binding ring-buffer cap so a slow pipeline cannot run the bridge out of heap. */
    public static final int DEFAULT_RING_BUFFER = 4096;

    private final FhirBridgeConfig config;
    private final FhirTransport transport;
    private final AbstractBridgeAuditOutput audit;
    private final FhirResourceMapper mapper;
    private final PseudonymService pseudonyms;

    private final Map<String, FhirSearchBinding> bindings = new LinkedHashMap<>();
    private final Map<String, Deque<IInputSignal>> queueByBinding = new ConcurrentHashMap<>();
    private final List<IInputSignal> events = new ArrayList<>();

    private final AtomicBoolean started = new AtomicBoolean();
    private final AtomicBoolean closed = new AtomicBoolean();

    public FhirClientService(FhirBridgeConfig config,
                             FhirTransport transport,
                             AbstractBridgeAuditOutput audit) {
        this.config = Objects.requireNonNull(config, "config");
        this.transport = Objects.requireNonNull(transport, "transport");
        this.audit = Objects.requireNonNull(audit, "audit");
        this.pseudonyms = PseudonymService.fromConfig(config.privacy());
        this.mapper = new FhirResourceMapper(pseudonyms, config.privacy().redactFreeText());
    }

    /** §6 — register every read binding. No outlets exist (FHIR ceiling §3). */
    public synchronized void start() {
        if (started.get() || closed.get()) return;
        for (FhirBridgeConfig.ReadBindingConfig r : config.reads()) {
            FhirSearchBinding b = FhirSearchBinding.fromConfig(r);
            bindings.put(b.bindingId(), b);
            queueByBinding.put(b.bindingId(), new ArrayDeque<>());
        }
        started.set(true);
        log.info("FhirClientService started; baseUrl={} bindings={} pseudonymise={}",
                config.fhir().baseUrl(), bindings.size(), pseudonyms.isEnabled());
    }

    /**
     * Run one polling cycle. For every cohort patient and every read
     * binding, issue the configured FHIR search through the transport and
     * decode the {@code Bundle} response into typed signals.
     *
     * <p>Failures are logged and audited but do not propagate — a single
     * EHR being down should not stop the rest of the cohort.
     */
    public synchronized void poll() {
        if (!isStarted()) return;
        if (!transport.isReady()) {
            log.debug("FhirClientService.poll: transport not ready, skipping cycle");
            return;
        }
        List<String> patientIds = config.cohort().patientIds();
        if (patientIds.isEmpty()) patientIds = List.of("");
        for (String rawPid : patientIds) {
            for (FhirSearchBinding b : bindings.values()) {
                pollBinding(b, rawPid);
            }
        }
        int redacted = mapper.drainRedactionCount();
        if (redacted > 0) {
            audit.append(new BridgeAuditRecord(
                    System.currentTimeMillis(), 0, BRIDGE_NAME,
                    BridgeAuditRecord.Verdict.APPLIED, null, null, null, null,
                    "FREE_TEXT_REDACTED:count=" + redacted, null, List.of()));
        }
    }

    private void pollBinding(FhirSearchBinding b, String rawPid) {
        String search = b.resolveSearch(rawPid);
        try {
            JsonNode bundle = transport.search(search);
            int decoded = 0;
            for (JsonNode resource : mapper.entries(bundle)) {
                IInputSignal signal = mapper.map(resource, b.targetSignal());
                if (signal == null) continue;
                enqueue(b.bindingId(), signal);
                decoded++;
            }
            log.debug("FhirClientService: binding={} pid={} decoded={}",
                    b.bindingId(), pseudonyms.pseudonymise(rawPid), decoded);
        } catch (IOException | RuntimeException e) {
            audit.append(new BridgeAuditRecord(
                    System.currentTimeMillis(), 0, BRIDGE_NAME,
                    BridgeAuditRecord.Verdict.FAILED, b.loopId(), b.signalTag(),
                    null, null,
                    BridgeAuditRecord.RejectReason.EXCEPTION + ":" + e.getClass().getSimpleName(),
                    null, List.of()));
            log.warn("FhirClientService: search '{}' failed: {}", search, e.getMessage());
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

    public Map<String, FhirSearchBinding> bindings() {
        return Map.copyOf(bindings);
    }

    public FhirBridgeConfig config() { return config; }

    public PseudonymService pseudonymService() { return pseudonyms; }

    public FhirResourceMapper resourceMapper() { return mapper; }

    @Override
    public synchronized void close() {
        if (!closed.compareAndSet(false, true)) return;
        try { transport.close(); } catch (RuntimeException e) {
            log.warn("FhirTransport close threw: {}", e.getMessage());
        }
    }
}
