/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.mqtt;

/**
 * Test seam between {@link MqttClientService} and the actual MQTT client
 * library (02-MQTT-SPARKPLUG.md §2). Production wiring uses the HiveMQ
 * client (see {@code DefaultMqttTransport}); tests inject an in-memory
 * implementation so the §9 scenarios can run without a broker.
 */
public interface MqttTransport extends AutoCloseable {

    /** One MQTT message observed by a subscriber. Bytes-only — decoding lives in {@link MqttSignalMapper}. */
    record InboundMessage(String topic, byte[] payload) {}

    /** Callback invoked when a subscribed topic delivers a message. */
    @FunctionalInterface
    interface MessageHandler { void onMessage(InboundMessage msg); }

    /**
     * Open the connection. Idempotent. Implementations doing real network
     * work block until connected or fail with {@link MqttTransportException}.
     */
    void connect();

    /**
     * Subscribe to one MQTT topic filter; the provided handler is invoked for
     * every received message. The same handler is registered for every
     * subscription on this transport.
     */
    void subscribe(String topicFilter, int qos);

    /** Set (or replace) the message handler used by every subscription. */
    void setHandler(MessageHandler handler);

    /** Publish a message. Throws {@link MqttTransportException} on transport-level failures. */
    void publish(String topic, byte[] payload, int qos, boolean retain);

    /** {@code true} once {@link #connect} succeeded and no disconnect has occurred since. */
    boolean isConnected();

    /** Shut down. Idempotent. */
    @Override
    void close();

    /** Wraps any client throwable so the bridge can audit it uniformly. */
    final class MqttTransportException extends RuntimeException {
        public MqttTransportException(String message) { super(message); }
        public MqttTransportException(String message, Throwable cause) { super(message, cause); }
    }
}
