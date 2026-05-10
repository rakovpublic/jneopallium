/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.mavlink;

import java.util.List;
import java.util.Objects;

/**
 * Resolved per-connection binding (12-MAVLINK.md §4, §8). Wraps the
 * {@link MavlinkBridgeConfig.ConnectionConfig} record with helpers used by
 * {@link MavlinkClientService}.
 */
public record MavlinkConnectionBinding(
        String id,
        MavlinkBridgeConfig.Transport transport,
        String bindAddress,
        Integer bindPort,
        String host,
        Integer port,
        List<Integer> expectedSystems
) {

    public MavlinkConnectionBinding {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(transport, "transport");
        expectedSystems = expectedSystems == null ? List.of() : List.copyOf(expectedSystems);
    }

    public static MavlinkConnectionBinding from(MavlinkBridgeConfig.ConnectionConfig c) {
        return new MavlinkConnectionBinding(
                c.id(), c.transport(),
                c.bindAddress(), c.bindPort(),
                c.host(), c.port(),
                c.expectedSystems());
    }

    /** {@code true} if {@code systemId} is allowed on this connection. Empty whitelist = allow-all. */
    public boolean isExpected(int systemId) {
        if (expectedSystems.isEmpty()) return true;
        for (int s : expectedSystems) if (s == systemId) return true;
        return false;
    }
}
