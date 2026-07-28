/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.ros2;

/**
 * Strategy A — embedded {@code rcljava} client (04-ROS2-DDS.md §1, §2).
 *
 * <p>Strategy A is opt-in (§9 Phase 3) and lives behind a feature flag.
 * The community-maintained {@code rcljava} dependency is not pulled into
 * the worker module at v1; this class is a placeholder so the package
 * layout in §8 is complete and so {@link Ros2BridgeConfig.TransportMode#RCLJAVA}
 * can compile-link to a sentinel rather than break loading.
 *
 * <p>To finish Strategy A: add the {@code org.ros2:rcljava} dependency,
 * implement {@link Ros2Transport} on top of {@code rcljava} {@code Node} and
 * {@code Subscription} primitives, and bridge the {@code domainId} +
 * {@code qosProfile} fields onto the {@code QoSProfile} struct.
 *
 * <p>Until then, instantiating this class throws
 * {@link UnsupportedOperationException} so the bridge fails fast at start
 * rather than silently fall back to no-op.
 */
public final class RcljavaClientService {

    /** Sentinel: feature is not built into the v1 worker jar. */
    public RcljavaClientService(Ros2BridgeConfig config) {
        if (config != null && config.mode() == Ros2BridgeConfig.TransportMode.RCLJAVA) {
            throw new UnsupportedOperationException(
                    "Strategy A (rcljava) not yet built into the worker jar; see 04-ROS2-DDS.md §9 Phase 3");
        }
    }
}
