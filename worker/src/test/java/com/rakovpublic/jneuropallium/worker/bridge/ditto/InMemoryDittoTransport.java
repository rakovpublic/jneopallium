/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.ditto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Pure-Java in-memory {@link DittoTransport} for the Ditto-bridge integration
 * tests. Runs without a Ditto sandbox; mirrors enough of Ditto's protocol
 * semantics to validate the §8 scenarios in 10-DITTO.md.
 *
 * <ul>
 *   <li>{@link #deliver(DittoTransport.TwinEvent)} pushes a twin event into the
 *       configured handler.</li>
 *   <li>{@link #failNextPut(int)} flips the PUT path to throw N times — used
 *       to test {@code WRITE_ERROR} audit paths.</li>
 *   <li>{@link #puts()} returns every PUT issued via
 *       {@link #putFeatureProperty(String, String, String, byte[])}, so tests
 *       assert on the advisory traffic.</li>
 * </ul>
 */
public final class InMemoryDittoTransport implements DittoTransport {

    public record Put(String thingId, String feature, String property, byte[] body) {}

    private EventHandler handler;
    private boolean connected;
    private final List<String> subscriptions = new ArrayList<>();
    private final List<Put> puts = new ArrayList<>();
    private final Map<String, byte[]> snapshots = new LinkedHashMap<>();
    private final AtomicLong putFailsRemaining = new AtomicLong();

    @Override public synchronized void connect() { connected = true; }
    @Override public synchronized void setHandler(EventHandler h) { this.handler = h; }
    @Override public synchronized boolean isConnected() { return connected; }
    @Override public synchronized void subscribe(String thingId) { subscriptions.add(thingId); }

    @Override
    public synchronized boolean putFeatureProperty(String thingId, String feature, String property, byte[] body) {
        if (putFailsRemaining.get() > 0) {
            putFailsRemaining.decrementAndGet();
            throw new DittoTransportException("simulated PUT failure on " + thingId + "/" + feature + "/" + property);
        }
        puts.add(new Put(thingId, feature, property, Arrays.copyOf(body, body.length)));
        snapshots.put(thingId + "/" + feature + "/" + property, Arrays.copyOf(body, body.length));
        return true;
    }

    @Override
    public synchronized byte[] getFeatureProperty(String thingId, String feature, String property) {
        byte[] v = snapshots.get(thingId + "/" + feature + "/" + property);
        return v == null ? null : Arrays.copyOf(v, v.length);
    }

    @Override
    public synchronized void close() { connected = false; }

    /* ===== test helpers ====================================================== */

    public synchronized void deliver(TwinEvent event) {
        EventHandler h = this.handler;
        if (h == null) return;
        // Only deliver to subscribed things — mirrors START-SEND-EVENTS behaviour.
        if (!subscriptions.contains(event.thingId())) return;
        h.onEvent(event);
    }

    public void failNextPut(int n) { putFailsRemaining.set(n); }

    public synchronized List<Put> puts() { return new ArrayList<>(puts); }

    public synchronized int subscriptionCount() { return subscriptions.size(); }

    public synchronized void disconnect() { connected = false; }
}
