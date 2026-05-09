/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.ditto;

/**
 * Test seam between {@link DittoClientService} and the actual Ditto wire
 * (10-DITTO.md §3). Production wiring uses {@link DefaultDittoTransport}
 * (java.net.http for REST and WebSocket); tests inject an in-memory
 * implementation so the §8 scenarios run without a Ditto sandbox.
 *
 * <p>The transport is bytes-only; signal decoding lives in
 * {@link DittoSignalMapper}.
 */
public interface DittoTransport extends AutoCloseable {

    /** One twin event observed on the WebSocket. */
    record TwinEvent(EventType type, String thingId, String feature, byte[] payload) {}

    /** The relevant subset of Ditto twin event types the bridge reacts to. */
    enum EventType {
        /** A feature property changed. */
        FEATURE_PROPERTY_MODIFIED,
        /** A feature value was replaced as a whole. */
        FEATURE_MODIFIED,
        /** The thing was deleted (or all its features deleted). */
        THING_DELETED
    }

    /** Callback invoked for every received twin event. */
    @FunctionalInterface
    interface EventHandler { void onEvent(TwinEvent event); }

    /**
     * Open the connection (HTTP for REST, WebSocket for events). Idempotent.
     * Implementations doing real network work block until connected or fail
     * with {@link DittoTransportException}.
     */
    void connect();

    /**
     * Subscribe to twin events for one thing. Implementations send the
     * Ditto protocol {@code START-SEND-EVENTS} message bound to the thing.
     */
    void subscribe(String thingId);

    /** Set (or replace) the event handler used by every subscription. */
    void setHandler(EventHandler handler);

    /**
     * Issue a feature-property modify against the Ditto HTTP API:
     * {@code PUT /api/2/things/<thingId>/features/<feature>/properties/<property>}
     * with {@code body} as the JSON request entity. Returns {@code true} when
     * the response status is 2xx.
     */
    boolean putFeatureProperty(String thingId, String feature, String property, byte[] body);

    /**
     * Read a feature-property snapshot:
     * {@code GET /api/2/things/<thingId>/features/<feature>/properties/<property>}.
     * Returns the raw response body, or {@code null} when the property does
     * not exist (HTTP 404). Throws {@link DittoTransportException} on other
     * non-2xx responses.
     */
    byte[] getFeatureProperty(String thingId, String feature, String property);

    /** {@code true} once {@link #connect} succeeded and no disconnect has occurred since. */
    boolean isConnected();

    /** Shut down. Idempotent. */
    @Override
    void close();

    /** Wraps any client throwable so the bridge can audit it uniformly. */
    final class DittoTransportException extends RuntimeException {
        public DittoTransportException(String message) { super(message); }
        public DittoTransportException(String message, Throwable cause) { super(message, cause); }
    }
}
