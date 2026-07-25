/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.plc4x;

import java.time.Duration;
import java.util.Objects;

/**
 * Runtime view of one PLC connection (01-PLC4X.md §3 — the
 * {@code Plc4xClientService} owns a {@code Map<connectionId, PlcConnection>}).
 *
 * <p>Adds runtime state on top of {@link Plc4xConfig.ConnectionConfig}:
 * the most recent reconnect attempt timestamp, used by the bridge for
 * exponential-backoff reconnect logic shared with the OPC UA bridge
 * (00-FRAMEWORK §2.3).
 */
public record Plc4xConnectionBinding(
        String id,
        String connectionString,
        Duration requestTimeout,
        Duration keepAliveInterval
) {
    public Plc4xConnectionBinding {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(connectionString, "connectionString");
        requestTimeout = requestTimeout == null ? Duration.ofSeconds(5) : requestTimeout;
    }

    public static Plc4xConnectionBinding from(Plc4xConfig.ConnectionConfig cfg) {
        return new Plc4xConnectionBinding(
                cfg.id(), cfg.connectionString(),
                cfg.requestTimeout(), cfg.keepAliveInterval());
    }
}
