/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.ditto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Production {@link DittoTransport} backed by the JDK's built-in
 * {@link HttpClient} for REST and {@link WebSocket} for twin events
 * (10-DITTO.md §3). The bridge is intentionally thin — the
 * <a href="https://www.eclipse.org/ditto/protocol-overview.html">Ditto
 * protocol</a> messages are JSON over WebSocket, so we hand-frame them
 * rather than pulling in the full {@code org.eclipse.ditto:ditto-client}
 * (and its Akka classic transitive deps).
 *
 * <p>Authentication: at present we support
 * {@link DittoBridgeConfig.AuthType#None} and
 * {@link DittoBridgeConfig.AuthType#BasicAuth}. OAuth2 token endpoints can
 * be wired by an out-of-process refresher that updates the env var pointed
 * to by {@code authentication.clientSecretEnv}; this transport will read
 * the latest value on every request.
 *
 * <p>This implementation does not perform reconnect itself — the
 * {@link DittoClientService} owns the reconnect policy
 * (00-FRAMEWORK §2.3) and is the one that calls
 * {@link DittoTransport#connect()} again after a disconnect.
 */
public final class DefaultDittoTransport implements DittoTransport {

    private static final Logger log = LoggerFactory.getLogger(DefaultDittoTransport.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    private final DittoBridgeConfig config;
    private final HttpClient http;
    private final AtomicReference<WebSocket> ws = new AtomicReference<>();
    private final AtomicReference<EventHandler> handler = new AtomicReference<>();
    private final AtomicBoolean connected = new AtomicBoolean();

    public DefaultDittoTransport(DittoBridgeConfig config) {
        this.config = Objects.requireNonNull(config, "config");
        this.http = HttpClient.newBuilder()
                .connectTimeout(config.connection().requestTimeout())
                .build();
    }

    @Override public synchronized void setHandler(EventHandler h) { handler.set(h); }
    @Override public boolean isConnected() { return connected.get(); }

    @Override
    public synchronized void connect() {
        if (connected.get()) return;
        URI wsUri = URI.create(toWsScheme(config.connection().baseUrl())
                + config.connection().webSocketPath());
        try {
            WebSocket socket = http.newWebSocketBuilder()
                    .header("Authorization", buildAuthHeader())
                    .connectTimeout(config.connection().requestTimeout())
                    .buildAsync(wsUri, new Listener())
                    .get();
            ws.set(socket);
            connected.set(true);
            log.info("DittoTransport connected to {}", wsUri);
        } catch (Exception ex) {
            throw new DittoTransportException("Ditto WebSocket connect failed: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void subscribe(String thingId) {
        WebSocket socket = ws.get();
        if (socket == null) throw new DittoTransportException("not connected");
        // START-SEND-EVENTS scoped to the configured thing — Ditto's filter syntax.
        String msg = "START-SEND-EVENTS?filter=eq(thingId,\"" + thingId + "\")";
        socket.sendText(msg, true).join();
    }

    @Override
    public boolean putFeatureProperty(String thingId, String feature, String property, byte[] body) {
        URI uri = URI.create(config.connection().baseUrl()
                + config.connection().httpPath()
                + "/things/" + thingId + "/features/" + feature
                + "/properties/" + property);
        HttpRequest req = HttpRequest.newBuilder(uri)
                .timeout(config.connection().requestTimeout())
                .header("Authorization", buildAuthHeader())
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();
        try {
            HttpResponse<Void> resp = http.send(req, HttpResponse.BodyHandlers.discarding());
            return resp.statusCode() / 100 == 2;
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new DittoTransportException("Ditto PUT failed: " + ex.getMessage(), ex);
        }
    }

    @Override
    public byte[] getFeatureProperty(String thingId, String feature, String property) {
        URI uri = URI.create(config.connection().baseUrl()
                + config.connection().httpPath()
                + "/things/" + thingId + "/features/" + feature
                + "/properties/" + property);
        HttpRequest req = HttpRequest.newBuilder(uri)
                .timeout(config.connection().requestTimeout())
                .header("Authorization", buildAuthHeader())
                .header("Accept", "application/json")
                .GET()
                .build();
        try {
            HttpResponse<byte[]> resp = http.send(req, HttpResponse.BodyHandlers.ofByteArray());
            int sc = resp.statusCode();
            if (sc == 404) return null;
            if (sc / 100 != 2) {
                throw new DittoTransportException("Ditto GET " + uri + " returned " + sc);
            }
            return resp.body();
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new DittoTransportException("Ditto GET failed: " + ex.getMessage(), ex);
        }
    }

    @Override
    public synchronized void close() {
        if (!connected.compareAndSet(true, false)) return;
        WebSocket socket = ws.getAndSet(null);
        if (socket == null) return;
        try {
            socket.sendClose(WebSocket.NORMAL_CLOSURE, "shutdown")
                    .orTimeout(config.connection().requestTimeout().toMillis(),
                            java.util.concurrent.TimeUnit.MILLISECONDS).join();
        } catch (RuntimeException ex) {
            log.warn("DittoTransport.close: WS close threw {}", ex.getMessage());
        }
    }

    /* ===== auth + URL helpers ============================================= */

    private String buildAuthHeader() {
        DittoBridgeConfig.AuthConfig a = config.authentication();
        if (a == null || a.type() == null || a.type() == DittoBridgeConfig.AuthType.None) {
            return "";
        }
        if (a.type() == DittoBridgeConfig.AuthType.BasicAuth) {
            String user = a.username() == null ? "" : a.username();
            String pass = a.passwordEnv() == null ? "" : Optional.ofNullable(System.getenv(a.passwordEnv())).orElse("");
            return "Basic " + Base64.getEncoder().encodeToString(
                    (user + ":" + pass).getBytes(StandardCharsets.UTF_8));
        }
        if (a.type() == DittoBridgeConfig.AuthType.OAuth2BearerToken) {
            String token = a.clientSecretEnv() == null ? "" : Optional.ofNullable(System.getenv(a.clientSecretEnv())).orElse("");
            return "Bearer " + token;
        }
        return "";
    }

    static String toWsScheme(String baseUrl) {
        if (baseUrl.startsWith("https://")) return "wss://" + baseUrl.substring("https://".length());
        if (baseUrl.startsWith("http://")) return "ws://" + baseUrl.substring("http://".length());
        return baseUrl;
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
                String msg = buf.toString();
                buf.setLength(0);
                dispatch(msg);
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            connected.set(false);
            log.info("DittoTransport WS closed: status={} reason={}", statusCode, reason);
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            connected.set(false);
            log.warn("DittoTransport WS error: {}", error.toString());
        }

        @Override public CompletionStage<?> onPing(WebSocket webSocket, ByteBuffer message) {
            webSocket.request(1);
            return CompletableFuture.completedFuture(null);
        }
    }

    private void dispatch(String message) {
        EventHandler h = handler.get();
        if (h == null) return;
        try {
            JsonNode root = JSON.readTree(message);
            JsonNode topic = root.get("topic");
            if (topic == null || !topic.isTextual()) return;
            String topicText = topic.asText();
            // topic format: <namespace>/<name>/things/twin/events/<action>
            String[] parts = topicText.split("/");
            if (parts.length < 6) return;
            String thingId = parts[0] + ":" + parts[1];
            String action = parts[5];
            JsonNode pathNode = root.get("path");
            String path = pathNode == null ? "/" : pathNode.asText();
            EventType type;
            String feature = null;
            if ("deleted".equals(action) && "/".equals(path)) {
                type = EventType.THING_DELETED;
            } else if (path.startsWith("/features/")) {
                String[] pp = path.split("/");
                if (pp.length >= 3) feature = pp[2];
                if (pp.length >= 5 && "properties".equals(pp[3])) {
                    type = EventType.FEATURE_PROPERTY_MODIFIED;
                } else {
                    type = EventType.FEATURE_MODIFIED;
                }
            } else {
                return;
            }
            h.onEvent(new TwinEvent(type, thingId, feature, message.getBytes(StandardCharsets.UTF_8)));
        } catch (RuntimeException | IOException ex) {
            log.warn("DittoTransport dispatch failed: {}", ex.getMessage());
        }
    }
}
