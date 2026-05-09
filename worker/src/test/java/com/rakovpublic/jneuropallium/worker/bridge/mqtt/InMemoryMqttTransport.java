/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.mqtt;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

/**
 * Pure-Java in-memory {@link MqttTransport} for the MQTT-bridge integration
 * tests. Runs without a broker; mirrors enough of MQTT's semantics to
 * validate the §9 scenarios in 02-MQTT-SPARKPLUG.md.
 *
 * <ul>
 *   <li>{@link #deliver(String, byte[])} pushes a message into every
 *       matching subscription.</li>
 *   <li>{@link #failNextPublish(int)} flips the publish path to throw N
 *       times — used to test PUBLISH_ERROR audit paths.</li>
 *   <li>{@link #published(String)} returns every payload sent via
 *       {@link #publish}, so tests assert on the advisory traffic.</li>
 * </ul>
 */
public final class InMemoryMqttTransport implements MqttTransport {

    private MessageHandler handler;
    private boolean connected;
    private final List<String> subscriptions = new ArrayList<>();
    private final Map<String, List<byte[]>> publishedByTopic = new LinkedHashMap<>();
    private final List<Published> publishedAll = new ArrayList<>();
    private final AtomicLong failsRemaining = new AtomicLong();

    public record Published(String topic, byte[] payload, int qos, boolean retain) {}

    @Override public synchronized void connect() { connected = true; }
    @Override public synchronized void setHandler(MessageHandler h) { this.handler = h; }
    @Override public synchronized boolean isConnected() { return connected; }

    @Override
    public synchronized void subscribe(String topicFilter, int qos) {
        subscriptions.add(topicFilter);
    }

    @Override
    public synchronized void publish(String topic, byte[] payload, int qos, boolean retain) {
        if (failsRemaining.get() > 0) {
            failsRemaining.decrementAndGet();
            throw new MqttTransportException("simulated publish failure on " + topic);
        }
        publishedByTopic.computeIfAbsent(topic, k -> new ArrayList<>()).add(payload);
        publishedAll.add(new Published(topic, payload, qos, retain));
    }

    @Override
    public synchronized void close() { connected = false; }

    /* ===== test helpers ====================================================== */

    /** Simulate the broker delivering one message; only matching subscriptions receive it. */
    public synchronized void deliver(String topic, byte[] payload) {
        if (handler == null) return;
        for (String filter : subscriptions) {
            if (matches(filter, topic)) {
                handler.onMessage(new InboundMessage(topic, payload));
                return;
            }
        }
    }

    /** Number of registered subscriptions. */
    public synchronized int subscriptionCount() { return subscriptions.size(); }

    /** Make the next {@code n} publishes throw — used for PUBLISH_ERROR tests. */
    public void failNextPublish(int n) { failsRemaining.set(n); }

    /** All publishes ever observed on {@code topic}, in send order. */
    public synchronized List<byte[]> published(String topic) {
        return new ArrayList<>(publishedByTopic.getOrDefault(topic, List.of()));
    }

    /** Every publish observed, in order, with full metadata. */
    public synchronized List<Published> publishes() { return new ArrayList<>(publishedAll); }

    /** Drop the connection — used to simulate transient broker loss. */
    public synchronized void disconnect() { connected = false; }

    /* ===== topic-filter matching (subset of the MQTT spec) =================== */

    static boolean matches(String filter, String topic) {
        // Convert MQTT filter (+, #) to regex.
        StringBuilder rx = new StringBuilder("^");
        String[] parts = filter.split("/", -1);
        for (int i = 0; i < parts.length; i++) {
            String p = parts[i];
            if (i > 0) rx.append("/");
            switch (p) {
                case "+" -> rx.append("[^/]+");
                case "#" -> {
                    rx.append(".*");
                    return Pattern.compile(rx.toString()).matcher(topic).matches();
                }
                default -> rx.append(Pattern.quote(p));
            }
        }
        rx.append("$");
        return Pattern.compile(rx.toString()).matcher(topic).matches();
    }
}
