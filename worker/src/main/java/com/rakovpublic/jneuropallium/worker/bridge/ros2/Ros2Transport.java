/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.ros2;

/**
 * Test seam between {@link Ros2ClientService} and the actual ROS 2 wire
 * (04-ROS2-DDS.md §1, §2).
 *
 * <p>Strategy B (the MVP) plugs in {@link RosbridgeTransport}, a thin
 * WebSocket adapter on top of {@code rosbridge_suite}. Strategy A
 * (rcljava, §2 advanced) would plug a different implementation behind the
 * same interface. Tests inject an in-memory implementation so the §10
 * scenarios run without rosbridge.
 *
 * <p>The transport is JSON-bytes-only on egress and JSON-text on ingress;
 * payload decoding lives in {@link Ros2MessageMapper}.
 */
public interface Ros2Transport extends AutoCloseable {

    /** One ROS 2 message observed on a subscribed topic. */
    record InboundMessage(String topic, String json) {}

    /** Callback invoked for every received message. */
    @FunctionalInterface
    interface MessageHandler { void onMessage(InboundMessage msg); }

    /**
     * Open the connection. Idempotent. Implementations doing real network
     * work block until connected or fail with {@link Ros2TransportException}.
     */
    void connect();

    /** Subscribe to one ROS 2 topic of the given message type. */
    void subscribe(String topic, String msgType);

    /** Advertise a ROS 2 topic for publishing. */
    void advertise(String topic, String msgType);

    /** Set (or replace) the inbound message handler. */
    void setHandler(MessageHandler handler);

    /** Publish a JSON-serialised ROS 2 message to {@code topic}. */
    void publish(String topic, String json);

    /** {@code true} once {@link #connect} succeeded and no disconnect has occurred since. */
    boolean isConnected();

    /** Shut down. Idempotent. */
    @Override
    void close();

    /** Wraps any client throwable so the bridge can audit it uniformly. */
    final class Ros2TransportException extends RuntimeException {
        public Ros2TransportException(String message) { super(message); }
        public Ros2TransportException(String message, Throwable cause) { super(message, cause); }
    }
}
