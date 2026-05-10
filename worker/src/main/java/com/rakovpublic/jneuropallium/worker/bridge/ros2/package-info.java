/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
/**
 * ROS 2 / DDS bridge (04-ROS2-DDS.md).
 *
 * <p><b>Safety ceiling:</b> {@code ADVISORY}. The bridge never publishes to
 * a field-actuating topic ({@code /cmd_vel}, {@code /joint_trajectory},
 * {@code /joint_command}) in production: outbound traffic goes only to a
 * configurable advisory namespace consumed by the external autonomy
 * supervisor. The forbidden-topic rule is enforced both at config load
 * (in {@link com.rakovpublic.jneuropallium.worker.bridge.ros2.Ros2BridgeConfig})
 * and at runtime
 * (in {@link com.rakovpublic.jneuropallium.worker.bridge.ros2.Ros2ClientService#publish}).
 * {@code AUTONOMOUS} per-tag promotion is rejected unless
 * {@code simulatorOnly: true} is set (the simulator-with-watchdog escape
 * from §3).
 *
 * <p><b>Layout (§8):</b>
 * <ul>
 *   <li>{@link com.rakovpublic.jneuropallium.worker.bridge.ros2.Ros2BridgeConfig}
 *       / {@link com.rakovpublic.jneuropallium.worker.bridge.ros2.Ros2BridgeConfigLoader}
 *       — YAML config (00-FRAMEWORK §3, FAIL_ON_UNKNOWN_PROPERTIES).</li>
 *   <li>{@link com.rakovpublic.jneuropallium.worker.bridge.ros2.Ros2TopicBinding}
 *       — read/write binding (BridgeBinding).</li>
 *   <li>{@link com.rakovpublic.jneuropallium.worker.bridge.ros2.Ros2MessageMapper}
 *       — pure functions: ROS 2 JSON ↔ typed Jneopallium signals.</li>
 *   <li>{@link com.rakovpublic.jneuropallium.worker.bridge.ros2.Ros2ClientService}
 *       — connection lifecycle, latest-value cache, decimation,
 *       payload caps, advisory queue (AutoCloseable).</li>
 *   <li>{@link com.rakovpublic.jneuropallium.worker.bridge.ros2.Ros2Transport}
 *       — test seam between the service and the wire.</li>
 *   <li>{@link com.rakovpublic.jneuropallium.worker.bridge.ros2.RosbridgeTransport}
 *       — Strategy B production transport (rosbridge_suite over JDK
 *       {@link java.net.http.WebSocket}).</li>
 *   <li>{@link com.rakovpublic.jneuropallium.worker.bridge.ros2.RcljavaClientService}
 *       — Strategy A placeholder (rcljava, behind a feature flag).</li>
 *   <li>{@link com.rakovpublic.jneuropallium.worker.bridge.ros2.Ros2SensoryInput}
 *       / {@link com.rakovpublic.jneuropallium.worker.bridge.ros2.Ros2StateInput}
 *       — IInitInputs draining the per-binding signal queues.</li>
 *   <li>{@link com.rakovpublic.jneuropallium.worker.bridge.ros2.Ros2AdvisoryOutputAggregator}
 *       — IOutputAggregator publishing only to advisory topics.</li>
 *   <li>{@link com.rakovpublic.jneuropallium.worker.bridge.ros2.Ros2AuditOutput}
 *       — JSONL audit, optionally mirrored to a ROS 2 audit topic.</li>
 * </ul>
 */
package com.rakovpublic.jneuropallium.worker.bridge.ros2;
