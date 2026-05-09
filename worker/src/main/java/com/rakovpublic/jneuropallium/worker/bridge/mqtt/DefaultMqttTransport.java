/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.mqtt;

import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Objects;

/**
 * Production {@link MqttTransport} backed by the HiveMQ MQTT client
 * (02-MQTT-SPARKPLUG.md §2). Translates the bridge's bytes-only API onto
 * Mqtt5 publish/subscribe.
 *
 * <p>This adapter is intentionally minimal: TLS, auth, will-message, and
 * Sparkplug NDEATH-as-LWT live in {@code MqttClientService} or in the
 * builder hooks below. The bridge does not depend on the HiveMQ types
 * outside this file so swapping clients is local.
 */
public final class DefaultMqttTransport implements MqttTransport {

    private static final Logger log = LoggerFactory.getLogger(DefaultMqttTransport.class);

    private final MqttBridgeConfig.ConnectionConfig connection;
    private final MqttBridgeConfig.SecurityConfig security;
    private final Mqtt5BlockingClient client;
    private MessageHandler handler;
    private volatile boolean connected;

    public DefaultMqttTransport(MqttBridgeConfig config) {
        this.connection = Objects.requireNonNull(config.connection(), "connection");
        this.security = config.security();

        URI uri = URI.create(connection.brokerUrl());
        boolean tls = "ssl".equalsIgnoreCase(uri.getScheme())
                || "mqtts".equalsIgnoreCase(uri.getScheme())
                || "wss".equalsIgnoreCase(uri.getScheme());
        var builder = Mqtt5Client.builder()
                .identifier(connection.clientId())
                .serverHost(uri.getHost() == null ? uri.getSchemeSpecificPart() : uri.getHost())
                .serverPort(uri.getPort() < 0
                        ? (tls ? 8883 : 1883)
                        : uri.getPort());
        if (tls) builder = builder.sslWithDefaultConfig();
        if (security != null
                && security.type() == MqttBridgeConfig.SecurityType.UsernamePassword
                && security.username() != null) {
            String pwd = security.passwordEnv() == null ? "" : System.getenv(security.passwordEnv());
            builder = builder.simpleAuth()
                    .username(security.username())
                    .password((pwd == null ? "" : pwd).getBytes(java.nio.charset.StandardCharsets.UTF_8))
                    .applySimpleAuth();
        }
        this.client = builder.buildBlocking();
    }

    @Override
    public synchronized void connect() {
        if (connected) return;
        try {
            client.connectWith()
                    .cleanStart(connection.cleanSession())
                    .keepAlive((int) connection.keepAlive().toSeconds())
                    .send();
            connected = true;
        } catch (Exception e) {
            throw new MqttTransportException("connect failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void setHandler(MessageHandler handler) { this.handler = handler; }

    @Override
    public synchronized void subscribe(String topicFilter, int qos) {
        if (!connected) connect();
        try {
            client.toAsync().subscribeWith()
                    .topicFilter(topicFilter)
                    .qos(qosOf(qos))
                    .callback(this::dispatch)
                    .send();
        } catch (Exception e) {
            throw new MqttTransportException("subscribe " + topicFilter + " failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void publish(String topic, byte[] payload, int qos, boolean retain) {
        try {
            client.publishWith()
                    .topic(topic)
                    .qos(qosOf(qos))
                    .retain(retain)
                    .payload(payload)
                    .send();
        } catch (Exception e) {
            throw new MqttTransportException("publish " + topic + " failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isConnected() { return connected; }

    @Override
    public synchronized void close() {
        if (!connected) return;
        try { client.disconnect(); } catch (Exception e) {
            log.warn("disconnect threw: {}", e.getMessage());
        }
        connected = false;
    }

    private void dispatch(Mqtt5Publish pub) {
        MessageHandler h = this.handler;
        if (h == null) return;
        h.onMessage(new InboundMessage(pub.getTopic().toString(), pub.getPayloadAsBytes()));
    }

    private static MqttQos qosOf(int q) {
        return switch (q) {
            case 0 -> MqttQos.AT_MOST_ONCE;
            case 2 -> MqttQos.EXACTLY_ONCE;
            default -> MqttQos.AT_LEAST_ONCE;
        };
    }
}
