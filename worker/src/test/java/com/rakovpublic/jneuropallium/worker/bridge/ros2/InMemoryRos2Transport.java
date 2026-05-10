/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.ros2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory {@link Ros2Transport} for the ROS 2 bridge integration tests.
 * Runs without rosbridge; mirrors enough of the wire to validate the §10
 * scenarios in 04-ROS2-DDS.md.
 */
public final class InMemoryRos2Transport implements Ros2Transport {

    private MessageHandler handler;
    private boolean connected;
    private final List<String> subscriptions = new ArrayList<>();
    private final Map<String, String> advertised = new HashMap<>();
    private final Map<String, List<String>> publishedByTopic = new LinkedHashMap<>();
    private final List<Published> publishedAll = new ArrayList<>();
    private final AtomicLong failsRemaining = new AtomicLong();

    public record Published(String topic, String json) {}

    @Override public synchronized void connect() { connected = true; }
    @Override public synchronized void setHandler(MessageHandler h) { this.handler = h; }
    @Override public synchronized boolean isConnected() { return connected; }

    @Override
    public synchronized void subscribe(String topic, String msgType) {
        subscriptions.add(topic);
    }

    @Override
    public synchronized void advertise(String topic, String msgType) {
        advertised.put(topic, msgType);
    }

    @Override
    public synchronized void publish(String topic, String json) {
        if (failsRemaining.get() > 0) {
            failsRemaining.decrementAndGet();
            throw new Ros2TransportException("simulated publish failure on " + topic);
        }
        publishedByTopic.computeIfAbsent(topic, k -> new ArrayList<>()).add(json);
        publishedAll.add(new Published(topic, json));
    }

    @Override
    public synchronized void close() { connected = false; }

    /* ===== test helpers ====================================================== */

    /** Deliver a rosbridge-shape "publish" envelope to every matching subscription. */
    public synchronized void deliverEnvelope(String topic, String envelopeJson) {
        if (handler == null) return;
        if (subscriptions.contains(topic)) {
            handler.onMessage(new InboundMessage(topic, envelopeJson));
        }
    }

    /** Build a rosbridge envelope and deliver it. */
    public synchronized void deliver(String topic, String msgJson) {
        deliverEnvelope(topic,
                "{\"op\":\"publish\",\"topic\":\"" + topic + "\",\"msg\":" + msgJson + "}");
    }

    /** Number of registered subscriptions. */
    public synchronized int subscriptionCount() { return subscriptions.size(); }

    public synchronized boolean isAdvertised(String topic) { return advertised.containsKey(topic); }

    /** Make the next {@code n} publishes throw — used for PUBLISH_ERROR tests. */
    public void failNextPublish(int n) { failsRemaining.set(n); }

    public synchronized List<String> published(String topic) {
        return new ArrayList<>(publishedByTopic.getOrDefault(topic, List.of()));
    }

    public synchronized List<Published> publishes() { return new ArrayList<>(publishedAll); }

    public synchronized void disconnect() { connected = false; }
}
