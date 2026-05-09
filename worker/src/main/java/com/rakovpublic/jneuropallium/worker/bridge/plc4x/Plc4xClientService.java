/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.plc4x;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Manages the per-connection PLC4X drivers, the per-binding polling loops,
 * and the latest-value cache (01-PLC4X.md §3, §6).
 *
 * <h2>Connection lifecycle</h2>
 * <ul>
 *   <li>{@link #connect()} opens every configured connection through the
 *       injected {@link Plc4xDriver}, then validates each read/write/event
 *       field address with a one-shot read. <b>Address validation failures
 *       are fatal at startup</b> (S9 in 01-PLC4X.md §8).</li>
 *   <li>One {@link ScheduledExecutorService} thread per connection runs the
 *       polling loop. Each binding is rescheduled at its own
 *       {@code pollIntervalMs} so one slow binding does not delay another on
 *       the same connection.</li>
 *   <li>{@link #close()} cancels every scheduled task and closes every open
 *       connection through the driver. Idempotent.</li>
 * </ul>
 *
 * <h2>Reconnect</h2>
 * Per-connection reconnect is handled by the {@link Plc4xDriver}
 * implementation. When a poll throws or returns a non-OK code repeatedly, the
 * latest cache for that binding is set to a non-OK response — the input
 * adapter then emits a {@code Quality.BAD}/{@code UNCERTAIN} signal per
 * 00-FRAMEWORK §0.5.
 *
 * <h2>Thread safety</h2>
 * {@link #latest} is a {@link ConcurrentHashMap}; {@link #write} delegates
 * to the driver which is required to be thread-safe. {@link #connect} and
 * {@link #close} synchronise on the instance monitor.
 */
public final class Plc4xClientService implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(Plc4xClientService.class);

    private final Plc4xDriver driver;
    private final Plc4xConfig config;

    /** Latest read response per signalTag. */
    private final ConcurrentHashMap<String, Plc4xDriver.ReadResponse> latest = new ConcurrentHashMap<>();

    /** Per-connection scheduler running its bindings' polls. */
    private final Map<String, ScheduledExecutorService> schedulers = new ConcurrentHashMap<>();

    /** All currently scheduled poll tasks (cancelled at close). */
    private final List<ScheduledFuture<?>> pollTasks = new ArrayList<>();

    /** Counts successful polls per binding — used by tests to assert the cadence. */
    private final ConcurrentHashMap<String, AtomicLong> pollCounts = new ConcurrentHashMap<>();

    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public Plc4xClientService(Plc4xDriver driver, Plc4xConfig config) {
        this.driver = Objects.requireNonNull(driver, "driver");
        this.config = Objects.requireNonNull(config, "config");
    }

    /**
     * Open all configured connections, validate every read/write/event
     * field address, then start the polling loops. Idempotent: a second
     * call is a no-op.
     *
     * @throws Plc4xException if a connection cannot be opened (S10) or any
     *                        field address is rejected by its driver (S9)
     */
    public synchronized void connect() {
        if (started.get()) return;

        // 1. Open every configured connection
        for (Plc4xConfig.ConnectionConfig c : config.connections()) {
            try {
                driver.open(c.id(), c.connectionString());
            } catch (RuntimeException e) {
                throw new Plc4xException(
                        "Failed to open PLC connection '" + c.id()
                                + "' (" + c.connectionString() + "): " + e.getMessage(), e);
            }
        }

        // 2. Validate every address (fail-fast — S9, S10)
        for (Plc4xConfig.ReadBindingConfig r : config.reads()) {
            validateOrThrow(r.connectionId(), r.fieldAddress(), r.bindingId());
        }
        for (Plc4xConfig.WriteBindingConfig w : config.writes()) {
            validateOrThrow(w.connectionId(), w.fieldAddress(), w.bindingId());
        }
        for (Plc4xConfig.EventBindingConfig e : config.events()) {
            validateOrThrow(e.connectionId(), e.fieldAddress(), e.bindingId());
        }

        // 3. Start one scheduler per connection, schedule each binding
        for (Plc4xConfig.ConnectionConfig c : config.connections()) {
            ScheduledExecutorService es = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "plc4x-poll-" + c.id());
                t.setDaemon(true);
                return t;
            });
            schedulers.put(c.id(), es);
        }

        for (Plc4xConfig.ReadBindingConfig r : config.reads()) {
            scheduleRead(r);
        }
        for (Plc4xConfig.EventBindingConfig e : config.events()) {
            scheduleEvent(e);
        }

        started.set(true);
        log.info("Plc4xClientService: started {} connections, {} read + {} event bindings",
                config.connections().size(), config.reads().size(), config.events().size());
    }

    /**
     * Issue a write through the driver. Called by the aggregator after the
     * universal safety pipeline has approved the value (00-FRAMEWORK §2.2).
     */
    public Plc4xResponseCode write(String connectionId, String fieldAddress, Object value) {
        if (closed.get()) return Plc4xResponseCode.REMOTE_ERROR;
        try {
            return driver.write(connectionId, fieldAddress, value);
        } catch (RuntimeException e) {
            log.warn("Plc4x write failed for connection={} field={}: {}",
                    connectionId, fieldAddress, e.toString());
            return Plc4xResponseCode.REMOTE_ERROR;
        }
    }

    /** Latest cached response for a signal tag, or {@code null} if never polled. */
    public Plc4xDriver.ReadResponse latest(String signalTag) {
        return latest.get(signalTag);
    }

    /** Snapshot of the entire latest-value cache. */
    public Map<String, Plc4xDriver.ReadResponse> snapshot() {
        return Map.copyOf(latest);
    }

    /** Successful poll count for a binding (for cadence / health checks). */
    public long pollCount(String bindingId) {
        AtomicLong n = pollCounts.get(bindingId);
        return n == null ? 0L : n.get();
    }

    @Override
    public synchronized void close() {
        if (!closed.compareAndSet(false, true)) return;
        for (ScheduledFuture<?> f : pollTasks) f.cancel(true);
        pollTasks.clear();
        for (ScheduledExecutorService es : schedulers.values()) {
            es.shutdownNow();
            try {
                es.awaitTermination(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        schedulers.clear();
        try {
            driver.closeAll();
        } catch (RuntimeException e) {
            log.warn("Plc4xDriver.closeAll() threw: {}", e.getMessage());
        }
        log.info("Plc4xClientService: closed");
    }

    /* ===== internals ====================================================== */

    private void validateOrThrow(String connectionId, String fieldAddress, String bindingId) {
        Plc4xResponseCode code;
        try {
            code = driver.validate(connectionId, fieldAddress);
        } catch (RuntimeException e) {
            throw new Plc4xException(
                    "Address validation threw for binding '" + bindingId
                            + "' (connection=" + connectionId + ", field=" + fieldAddress
                            + "): " + e.getMessage(), e);
        }
        if (code != Plc4xResponseCode.OK) {
            throw new Plc4xException(
                    "Address rejected for binding '" + bindingId
                            + "' (connection=" + connectionId + ", field=" + fieldAddress
                            + "): " + code, code);
        }
    }

    private void scheduleRead(Plc4xConfig.ReadBindingConfig r) {
        ScheduledExecutorService es = schedulers.get(r.connectionId());
        if (es == null) {
            throw new Plc4xException(
                    "Read binding '" + r.bindingId()
                            + "' references unknown connectionId '" + r.connectionId() + "'");
        }
        ScheduledFuture<?> f = es.scheduleAtFixedRate(
                () -> pollOnce(r.connectionId(), r.fieldAddress(), r.signalTag(), r.bindingId()),
                0L, r.pollIntervalMs(), TimeUnit.MILLISECONDS);
        pollTasks.add(f);
    }

    private void scheduleEvent(Plc4xConfig.EventBindingConfig e) {
        ScheduledExecutorService es = schedulers.get(e.connectionId());
        if (es == null) {
            throw new Plc4xException(
                    "Event binding '" + e.bindingId()
                            + "' references unknown connectionId '" + e.connectionId() + "'");
        }
        ScheduledFuture<?> f = es.scheduleAtFixedRate(
                () -> pollOnce(e.connectionId(), e.fieldAddress(), e.signalTag(), e.bindingId()),
                0L, e.pollIntervalMs(), TimeUnit.MILLISECONDS);
        pollTasks.add(f);
    }

    private void pollOnce(String connectionId, String fieldAddress, String signalTag, String bindingId) {
        if (closed.get()) return;
        try {
            Plc4xDriver.ReadResponse resp = driver.read(connectionId, fieldAddress);
            latest.put(signalTag, resp);
            if (resp.code() == Plc4xResponseCode.OK) {
                pollCounts.computeIfAbsent(bindingId, k -> new AtomicLong()).incrementAndGet();
            }
        } catch (RuntimeException e) {
            // Cache the failure so the input emits Quality.BAD per §0.5.
            latest.put(signalTag, Plc4xDriver.ReadResponse.failure(Plc4xResponseCode.REMOTE_ERROR));
            log.warn("Plc4x poll threw for binding={} connection={} field={}: {}",
                    bindingId, connectionId, fieldAddress, e.toString());
        }
    }
}
