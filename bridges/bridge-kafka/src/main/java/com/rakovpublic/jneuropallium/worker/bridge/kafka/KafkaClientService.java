/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.kafka;

import com.rakovpublic.jneuropallium.worker.bridge.common.AbstractBridgeAuditOutput;
import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeAuditRecord;
import com.rakovpublic.jneuropallium.worker.bridge.kafka.decoder.PayloadDecoder;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Wraps a {@link KafkaTransport} with the per-binding bookkeeping, decoder
 * dispatch, offset-commit policy, and bounded advisory queue described in
 * 08-KAFKA.md §3 & §10 R4.
 *
 * <p>This class is the single owner of the consumer/producer instances —
 * input adapters call {@link #drain} to pull pre-decoded signals out of an
 * in-process queue, and the {@link KafkaAdvisoryOutputAggregator} calls
 * {@link #publish} to enqueue an outbound record.
 *
 * <h2>Threading</h2>
 * The default deployment is single-threaded: {@link #pollAll} pulls from
 * every read binding in turn on the worker tick, decodes records, and
 * appends them to per-binding queues. Sites that need a dedicated thread per
 * binding ({@code 08-KAFKA.md §10 R3}) wire one up around {@link #pollOne}.
 *
 * <h2>Failure isolation</h2>
 * <ul>
 *   <li>Decoder throw → audit {@code FAILED reason=DECODER_ERROR}; the
 *       binding's failure policy decides whether to advance the offset.</li>
 *   <li>Producer throw → audit {@code FAILED reason=PRODUCER_ERROR}; the
 *       record stays out of any consumer's view.</li>
 *   <li>Bounded advisory queue overrun → drop oldest with audit
 *       {@code FAILED reason=ADVISORY_QUEUE_FULL} (08-KAFKA.md §10 R4).</li>
 * </ul>
 */
public final class KafkaClientService implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(KafkaClientService.class);

    public static final String BRIDGE_NAME = "kafka";

    /** Audit reasons specific to this bridge (in addition to the §4 vocabulary). */
    public static final class Reason {
        private Reason() {}
        public static final String DECODER_ERROR = "DECODER_ERROR";
        public static final String PRODUCER_ERROR = "PRODUCER_ERROR";
        public static final String ADVISORY_QUEUE_FULL = "ADVISORY_QUEUE_FULL";
        public static final String OFFSET_HOLD = "OFFSET_HOLD";
    }

    private final KafkaBridgeConfig config;
    private final KafkaTransport transport;
    private final KafkaSignalMapper mapper;
    private final AbstractBridgeAuditOutput audit;

    /** Decoded signals waiting to be drained by an input adapter, keyed by bindingId. */
    private final Map<String, List<IInputSignal>> pendingByBinding = new ConcurrentHashMap<>();

    /** Resolved binding metadata, keyed by bindingId. */
    private final Map<String, KafkaTopicBinding> readBindings = new HashMap<>();
    private final Map<String, KafkaTopicBinding> writeBindings = new HashMap<>();

    /** Bindings with at least one record that failed to decode — used to honour STOP_AT_FAILED_OFFSET. */
    private final Map<String, Boolean> stuckBindings = new ConcurrentHashMap<>();

    /** Producer queue depth per write binding — backed by the {@code maxQueueSize} from config. */
    private final Map<String, AtomicLong> queueDepth = new ConcurrentHashMap<>();

    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public KafkaClientService(KafkaBridgeConfig config,
                              KafkaTransport transport,
                              KafkaSignalMapper mapper,
                              AbstractBridgeAuditOutput audit) {
        this.config = Objects.requireNonNull(config, "config");
        this.transport = Objects.requireNonNull(transport, "transport");
        this.mapper = Objects.requireNonNull(mapper, "mapper");
        this.audit = Objects.requireNonNull(audit, "audit");
    }

    /** Subscribe every read binding. Idempotent. */
    public synchronized void start() {
        if (started.get()) return;
        // Schema-Registry mandatory check (08-KAFKA.md §10 R2).
        KafkaBridgeConfig.SchemaRegistryConfig sr = config.schemaRegistry();
        if (sr != null && sr.enabled() && sr.mandatory()
                && (sr.url() == null || sr.url().isBlank())) {
            throw new IllegalStateException(
                    "schemaRegistry.mandatory=true but url is missing — refusing to start");
        }
        for (KafkaBridgeConfig.ReadBindingConfig r : config.reads()) {
            transport.subscribe(r.bindingId(), r.topic(), config.cluster().consumerGroupId());
            readBindings.put(r.bindingId(), KafkaTopicBinding.fromRead(r));
            pendingByBinding.put(r.bindingId(), new ArrayList<>());
        }
        for (KafkaBridgeConfig.WriteBindingConfig w : config.writes()) {
            writeBindings.put(w.bindingId(), KafkaTopicBinding.fromWrite(w));
            queueDepth.put(w.bindingId(), new AtomicLong(0));
        }
        started.set(true);
        log.info("KafkaClientService: started with {} read + {} write bindings",
                config.reads().size(), config.writes().size());
    }

    /** Poll every read binding once, decode, and enqueue. */
    public void pollAll(long run) {
        for (KafkaBridgeConfig.ReadBindingConfig r : config.reads()) {
            pollOne(r, run);
        }
    }

    /** Poll one read binding, decode, enqueue. Returns the number of decoded signals. */
    public int pollOne(KafkaBridgeConfig.ReadBindingConfig r, long run) {
        if (closed.get() || !started.get()) return 0;
        if (Boolean.TRUE.equals(stuckBindings.get(r.bindingId()))) {
            // §10 R1: STOP_AT_FAILED_OFFSET — refuse to advance until clearStuck() is called.
            audit.append(new BridgeAuditRecord(
                    System.currentTimeMillis(), run, BRIDGE_NAME,
                    BridgeAuditRecord.Verdict.REJECTED,
                    r.bindingId(), r.signalTagPrefix(), null, null,
                    Reason.OFFSET_HOLD, null, List.of()));
            return 0;
        }

        List<KafkaTransport.InboundRecord> batch;
        try {
            batch = transport.poll(r.bindingId(),
                    config.cluster().pollTimeout(),
                    config.cluster().maxPollRecords());
        } catch (KafkaTransport.KafkaTransportException ex) {
            audit.append(new BridgeAuditRecord(
                    System.currentTimeMillis(), run, BRIDGE_NAME,
                    BridgeAuditRecord.Verdict.FAILED,
                    r.bindingId(), r.signalTagPrefix(), null, null,
                    BridgeAuditRecord.RejectReason.EXCEPTION + ":" + ex.getClass().getSimpleName(),
                    null, List.of()));
            return 0;
        }
        if (batch == null || batch.isEmpty()) return 0;

        PayloadDecoder decoder = mapper.resolve(r);
        List<IInputSignal> decoded = new ArrayList<>(batch.size());
        Map<Integer, Long> commitableMaxOffset = new HashMap<>();
        boolean stuck = false;

        for (KafkaTransport.InboundRecord rec : batch) {
            try {
                List<IInputSignal> sigs = decoder.decode(
                        rec.topic(), rec.key(), rec.value(), r.signalTagPrefix());
                if (sigs != null && !sigs.isEmpty()) decoded.addAll(sigs);
                commitableMaxOffset.merge(rec.partition(), rec.offset() + 1, Math::max);
            } catch (PayloadDecoder.DecoderException de) {
                audit.append(new BridgeAuditRecord(
                        System.currentTimeMillis(), run, BRIDGE_NAME,
                        BridgeAuditRecord.Verdict.FAILED,
                        r.bindingId(), r.signalTagPrefix(), null, null,
                        Reason.DECODER_ERROR + ":" + de.getMessage(),
                        null, List.of()));
                if (r.failurePolicy() == KafkaBridgeConfig.FailurePolicy.STOP_AT_FAILED_OFFSET) {
                    stuck = true;
                    break;  // never commit past the bad record
                } else {
                    // SKIP_AND_LOG: advance the offset past the bad record.
                    commitableMaxOffset.merge(rec.partition(), rec.offset() + 1, Math::max);
                }
            }
        }

        if (!decoded.isEmpty()) {
            pendingByBinding.computeIfAbsent(r.bindingId(), k -> new ArrayList<>()).addAll(decoded);
        }
        if (!commitableMaxOffset.isEmpty() && !config.cluster().enableAutoCommit()) {
            try {
                transport.commitSync(r.bindingId(), commitableMaxOffset);
            } catch (KafkaTransport.KafkaTransportException e) {
                log.warn("Offset commit failed for binding {}: {}", r.bindingId(), e.getMessage());
            }
        }
        if (stuck) stuckBindings.put(r.bindingId(), Boolean.TRUE);
        return decoded.size();
    }

    /** Drain (and clear) all decoded signals for one read binding. Called by {@link KafkaLogInput} et al. */
    public List<IInputSignal> drain(String bindingId) {
        List<IInputSignal> out = pendingByBinding.get(bindingId);
        if (out == null || out.isEmpty()) return List.of();
        synchronized (out) {
            List<IInputSignal> snap = new ArrayList<>(out);
            out.clear();
            return snap;
        }
    }

    /** Operator action to clear a STOP_AT_FAILED_OFFSET halt after the bad record was investigated. */
    public void clearStuck(String bindingId) { stuckBindings.remove(bindingId); }
    public boolean isStuck(String bindingId) { return Boolean.TRUE.equals(stuckBindings.get(bindingId)); }

    /**
     * Publish one advisory record to {@code topic}. Returns {@code true} on a
     * successful enqueue; {@code false} if the queue overflowed or the
     * producer threw.
     */
    public boolean publish(String bindingId, String key, byte[] value, long run, String signalTag) {
        if (closed.get() || !started.get()) return false;
        KafkaTopicBinding b = writeBindings.get(bindingId);
        KafkaBridgeConfig.WriteBindingConfig wc = findWrite(bindingId);
        if (b == null || wc == null) {
            audit.append(new BridgeAuditRecord(
                    System.currentTimeMillis(), run, BRIDGE_NAME,
                    BridgeAuditRecord.Verdict.REJECTED,
                    bindingId, signalTag, null, null,
                    BridgeAuditRecord.RejectReason.UNKNOWN_TAG, null, List.of()));
            return false;
        }

        AtomicLong depth = queueDepth.get(bindingId);
        long current = depth.incrementAndGet();
        if (current > wc.maxQueueSize()) {
            depth.decrementAndGet();
            audit.append(new BridgeAuditRecord(
                    System.currentTimeMillis(), run, BRIDGE_NAME,
                    BridgeAuditRecord.Verdict.FAILED,
                    bindingId, signalTag, null, null,
                    Reason.ADVISORY_QUEUE_FULL, null, List.of()));
            return false;
        }
        try {
            transport.send(b.topic(), key, value);
            audit.append(new BridgeAuditRecord(
                    System.currentTimeMillis(), run, BRIDGE_NAME,
                    BridgeAuditRecord.Verdict.APPLIED,
                    bindingId, signalTag, null, null, null, null, List.of()));
            return true;
        } catch (KafkaTransport.KafkaTransportException ex) {
            audit.append(new BridgeAuditRecord(
                    System.currentTimeMillis(), run, BRIDGE_NAME,
                    BridgeAuditRecord.Verdict.FAILED,
                    bindingId, signalTag, null, null,
                    Reason.PRODUCER_ERROR + ":" + ex.getMessage(), null, List.of()));
            return false;
        } finally {
            depth.decrementAndGet();
        }
    }

    public KafkaBridgeConfig config() { return config; }
    public KafkaTopicBinding readBinding(String bindingId) { return readBindings.get(bindingId); }
    public KafkaTopicBinding writeBinding(String bindingId) { return writeBindings.get(bindingId); }

    @Override
    public synchronized void close() {
        if (!closed.compareAndSet(false, true)) return;
        try { transport.close(); } catch (RuntimeException e) {
            log.warn("KafkaTransport.close() threw: {}", e.getMessage());
        }
    }

    /* ===== internals ====================================================== */

    private KafkaBridgeConfig.WriteBindingConfig findWrite(String bindingId) {
        for (KafkaBridgeConfig.WriteBindingConfig w : config.writes()) {
            if (w.bindingId().equals(bindingId)) return w;
        }
        return null;
    }
}
