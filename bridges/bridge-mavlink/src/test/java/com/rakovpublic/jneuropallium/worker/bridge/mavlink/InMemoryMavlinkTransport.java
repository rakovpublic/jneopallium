/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.mavlink;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory {@link MavlinkTransport} for the MAVLink bridge integration
 * tests. Mirrors enough of the wire to validate the §10 scenarios in
 * 12-MAVLINK.md without a real SITL instance.
 */
public final class InMemoryMavlinkTransport implements MavlinkTransport {

    private MessageHandler handler;
    private boolean connected;
    private final List<Sent> sent = new ArrayList<>();
    private final AtomicLong failsRemaining = new AtomicLong();

    public record Sent(int systemId, int componentId, Object payload) {}

    @Override public synchronized void connect() { connected = true; }
    @Override public synchronized void setHandler(MessageHandler h) { this.handler = h; }
    @Override public synchronized boolean isConnected() { return connected; }

    @Override
    public synchronized void send(int systemId, int componentId, Object payload) {
        if (failsRemaining.get() > 0) {
            failsRemaining.decrementAndGet();
            throw new MavlinkTransportException("simulated send failure");
        }
        sent.add(new Sent(systemId, componentId, payload));
    }

    @Override public synchronized void close() { connected = false; }

    /* ===== test helpers ============================================== */

    /** Deliver an already-typed message to the handler. */
    public synchronized <T> void deliver(int systemId, int componentId, T payload) {
        if (handler == null) return;
        handler.onMessage(new InboundMessage(systemId, componentId, payload));
    }

    /** Number of messages we have sent on this transport. */
    public synchronized int sendCount() { return sent.size(); }

    public synchronized List<Sent> sentMessages() { return new ArrayList<>(sent); }

    /** Make the next {@code n} sends throw — used for PUBLISH_ERROR tests. */
    public void failNextSends(int n) { failsRemaining.set(n); }
}
