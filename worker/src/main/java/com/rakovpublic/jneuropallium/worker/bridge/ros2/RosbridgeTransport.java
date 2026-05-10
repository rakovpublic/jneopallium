/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.ros2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Production {@link Ros2Transport} backed by the JDK's built-in
 * {@link HttpClient} {@link WebSocket} talking the JSON-over-WebSocket
 * protocol exposed by
 * <a href="https://github.com/RobotWebTools/rosbridge_suite">rosbridge_suite</a>
 * (04-ROS2-DDS.md §1 Strategy B, §2). The bridge is intentionally thin — no
 * extra Java-WebSocket dependency is needed because the JDK ships one since
 * Java 11.
 *
 * <p>This implementation does not perform reconnect itself — the
 * {@link Ros2ClientService} owns the reconnect policy (00-FRAMEWORK §2.3)
 * and is the one that calls {@link Ros2Transport#connect()} again after a
 * disconnect.
 */
public final class RosbridgeTransport implements Ros2Transport {

    private static final Logger log = LoggerFactory.getLogger(RosbridgeTransport.class);
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);

    private final Ros2BridgeConfig config;
    private final Ros2MessageMapper mapper;
    private final HttpClient http;
    private final AtomicReference<WebSocket> ws = new AtomicReference<>();
    private final AtomicReference<MessageHandler> handler = new AtomicReference<>();
    private final AtomicBoolean connected = new AtomicBoolean();

    public RosbridgeTransport(Ros2BridgeConfig config, Ros2MessageMapper mapper) {
        this.config = Objects.requireNonNull(config, "config");
        this.mapper = Objects.requireNonNull(mapper, "mapper");
        this.http = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
    }

    @Override public synchronized void setHandler(MessageHandler h) { handler.set(h); }
    @Override public boolean isConnected() { return connected.get(); }

    @Override
    public synchronized void connect() {
        if (connected.get()) return;
        URI uri = URI.create(config.rosbridgeUrl());
        try {
            WebSocket socket = http.newWebSocketBuilder()
                    .connectTimeout(CONNECT_TIMEOUT)
                    .buildAsync(uri, new Listener())
                    .get();
            ws.set(socket);
            connected.set(true);
            log.info("RosbridgeTransport connected to {}", uri);
        } catch (Exception ex) {
            throw new Ros2TransportException("rosbridge connect failed: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void subscribe(String topic, String msgType) {
        WebSocket socket = ws.get();
        if (socket == null) throw new Ros2TransportException("not connected");
        socket.sendText(mapper.rosbridgeSubscribe(topic, msgType), true).join();
    }

    @Override
    public void advertise(String topic, String msgType) {
        WebSocket socket = ws.get();
        if (socket == null) throw new Ros2TransportException("not connected");
        socket.sendText(mapper.rosbridgeAdvertise(topic, msgType), true).join();
    }

    @Override
    public void publish(String topic, String json) {
        WebSocket socket = ws.get();
        if (socket == null) throw new Ros2TransportException("not connected");
        try {
            socket.sendText(mapper.rosbridgePublishEnvelope(topic, json), true).join();
        } catch (RuntimeException ex) {
            throw new Ros2TransportException("rosbridge publish failed: " + ex.getMessage(), ex);
        }
    }

    @Override
    public synchronized void close() {
        if (!connected.compareAndSet(true, false)) return;
        WebSocket socket = ws.getAndSet(null);
        if (socket == null) return;
        try {
            socket.sendClose(WebSocket.NORMAL_CLOSURE, "shutdown")
                    .orTimeout(CONNECT_TIMEOUT.toMillis(),
                            java.util.concurrent.TimeUnit.MILLISECONDS).join();
        } catch (RuntimeException ex) {
            log.warn("RosbridgeTransport.close: WS close threw {}", ex.getMessage());
        }
    }

    /* ===== inbound WebSocket listener ===================================== */

    private final class Listener implements WebSocket.Listener {
        private final StringBuilder buf = new StringBuilder();

        @Override public void onOpen(WebSocket webSocket) {
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            buf.append(data);
            if (last) {
                String text = buf.toString();
                buf.setLength(0);
                dispatch(text);
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            connected.set(false);
            log.info("RosbridgeTransport WS closed: status={} reason={}", statusCode, reason);
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            connected.set(false);
            log.warn("RosbridgeTransport WS error: {}", error.toString());
        }

        @Override public CompletionStage<?> onPing(WebSocket webSocket, ByteBuffer message) {
            webSocket.request(1);
            return CompletableFuture.completedFuture(null);
        }
    }

    private void dispatch(String message) {
        MessageHandler h = handler.get();
        if (h == null) return;
        try {
            // Parse just enough to find the topic — full payload decoding
            // happens inside the mapper.
            com.fasterxml.jackson.databind.JsonNode root = mapper.jsonMapper().readTree(message);
            com.fasterxml.jackson.databind.JsonNode op = root.get("op");
            if (op == null || !"publish".equals(op.asText())) return;
            com.fasterxml.jackson.databind.JsonNode topic = root.get("topic");
            if (topic == null || !topic.isTextual()) return;
            h.onMessage(new InboundMessage(topic.asText(), message));
        } catch (Exception ex) {
            log.warn("RosbridgeTransport dispatch failed: {}", ex.getMessage());
        }
    }
}
