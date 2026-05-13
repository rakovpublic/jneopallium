/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.iec61850;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * In-memory {@link Iec61850MmsClient} used by acceptance tests
 * (11-IEC61850.md §9 S7, S8, S10).
 *
 * <p>Lets a test pre-load latest values per {@code <iedId, daPath>} pair
 * and trigger Report Control Block reports on demand. No network is
 * involved — the in-memory client mirrors the simulator-driven setup
 * described in §8 phase 1 (run libiec61850's
 * {@code server_example_basic_io} on {@code localhost:102}) but without
 * shelling out to native code.
 *
 * <p>Like the production seam, this implementation exposes <b>no write
 * methods</b>: there is no path to set a Data Attribute on the
 * simulated IED.
 */
public final class InMemoryIec61850MmsClient implements Iec61850MmsClient {

    private final Map<String, MmsRead> values = new ConcurrentHashMap<>();
    private final Map<String, List<Subscription>> subscriptions = new ConcurrentHashMap<>();
    private volatile boolean ready = true;
    private volatile boolean failNextRead;
    private volatile IOException nextException;

    public InMemoryIec61850MmsClient putMeasurement(String iedId, String daPath,
                                                    double value,
                                                    Iec61850Quality quality,
                                                    long sourceTimestampMillis) {
        values.put(key(iedId, daPath),
                MmsRead.measurement(value, quality, sourceTimestampMillis));
        return this;
    }

    public InMemoryIec61850MmsClient putStatus(String iedId, String daPath,
                                               boolean closed,
                                               Iec61850Quality quality,
                                               long sourceTimestampMillis) {
        values.put(key(iedId, daPath),
                MmsRead.status(closed, quality, sourceTimestampMillis));
        return this;
    }

    public InMemoryIec61850MmsClient setReady(boolean ready) {
        this.ready = ready;
        return this;
    }

    public InMemoryIec61850MmsClient failNextReadWith(IOException ex) {
        this.failNextRead = true;
        this.nextException = Objects.requireNonNull(ex, "ex");
        return this;
    }

    /**
     * Fan out a synthetic report to every active subscriber of
     * {@code reportControlBlock} on {@code iedId}.
     */
    public void emitReport(String iedId, String reportControlBlock, MmsReport report) {
        List<Subscription> subs = subscriptions.get(key(iedId, reportControlBlock));
        if (subs == null) return;
        for (Subscription s : List.copyOf(subs)) {
            if (s.active) s.consumer.accept(report);
        }
    }

    @Override
    public boolean isReady() {
        return ready;
    }

    @Override
    public MmsRead readDa(String iedId, String daPath) throws IOException {
        if (failNextRead) {
            failNextRead = false;
            IOException ex = nextException;
            nextException = null;
            throw ex == null ? new IOException("simulated read failure") : ex;
        }
        MmsRead v = values.get(key(iedId, daPath));
        return v == null ? MmsRead.none() : v;
    }

    @Override
    public AutoCloseable subscribeReport(String iedId, String reportControlBlock,
                                         Consumer<MmsReport> consumer) {
        Objects.requireNonNull(consumer, "consumer");
        String k = key(iedId, reportControlBlock);
        Subscription s = new Subscription(consumer);
        subscriptions.computeIfAbsent(k, kk -> new ArrayList<>()).add(s);
        return () -> {
            s.active = false;
            List<Subscription> list = subscriptions.get(k);
            if (list != null) list.remove(s);
        };
    }

    @Override
    public void close() {
        subscriptions.clear();
    }

    /** Snapshot the registered values (test inspection only). */
    public Map<String, MmsRead> snapshot() {
        return new LinkedHashMap<>(values);
    }

    private static String key(String iedId, String tail) {
        return iedId + "::" + tail;
    }

    private static final class Subscription {
        final Consumer<MmsReport> consumer;
        volatile boolean active = true;
        Subscription(Consumer<MmsReport> consumer) {
            this.consumer = consumer;
        }
    }

    /** Convenience: build a Logical-Node-keyed report entry list. */
    public static List<MmsReport.Entry> entriesFor(Map<String, String> lnClassToCondition) {
        List<MmsReport.Entry> out = new ArrayList<>(lnClassToCondition.size());
        for (Map.Entry<String, String> e : new HashMap<>(lnClassToCondition).entrySet()) {
            out.add(new MmsReport.Entry(null, e.getKey(), null, Boolean.TRUE,
                    Iec61850Quality.GOOD, e.getValue()));
        }
        return out;
    }
}
