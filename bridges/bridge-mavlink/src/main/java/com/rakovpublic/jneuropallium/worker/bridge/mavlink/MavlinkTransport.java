/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.mavlink;

/**
 * Test seam between {@link MavlinkClientService} and the actual MAVLink wire
 * (12-MAVLINK.md §4). One transport instance corresponds to one MAVLink
 * connection (a UDP bind, a TCP socket, or — out of scope here — a serial
 * port) and surfaces inbound MAVLink messages as already-typed dronefleet
 * payload objects via {@link InboundMessage}.
 *
 * <p>Egress takes an already-encoded payload — the service hands the
 * transport the source/component ids and an {@code Object} payload that the
 * dronefleet codec knows how to serialize. The transport is responsible for
 * pushing the resulting bytes to the underlying socket.
 *
 * <p>Tests inject {@code InMemoryMavlinkTransport} so the §10 scenarios can
 * run without a real SITL instance.
 */
public interface MavlinkTransport extends AutoCloseable {

    /**
     * One MAVLink message observed on this connection. The
     * {@link io.dronefleet.mavlink.MavlinkMessage} type is package-private
     * to construct, so the transport contract surfaces just the routing
     * fields plus the typed payload (one of the dronefleet message
     * records).
     */
    record InboundMessage(int systemId, int componentId, Object payload) {}

    /** Callback invoked for every received MAVLink message. */
    @FunctionalInterface
    interface MessageHandler { void onMessage(InboundMessage message); }

    /**
     * Open the connection. Idempotent. Implementations doing real network
     * work block until connected or fail with {@link MavlinkTransportException}.
     */
    void connect();

    /** Set (or replace) the inbound message handler. */
    void setHandler(MessageHandler handler);

    /**
     * Encode {@code payload} as a MAVLink message and push it onto the wire.
     * Throws {@link MavlinkTransportException} on protocol/transport failure.
     */
    void send(int systemId, int componentId, Object payload);

    /** {@code true} once {@link #connect} succeeded and no disconnect has occurred since. */
    boolean isConnected();

    /** Shut down. Idempotent. */
    @Override
    void close();

    /** Wraps any client throwable so the bridge can audit it uniformly. */
    final class MavlinkTransportException extends RuntimeException {
        public MavlinkTransportException(String message) { super(message); }
        public MavlinkTransportException(String message, Throwable cause) { super(message, cause); }
    }
}
