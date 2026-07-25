/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
/**
 * MQTT + Sparkplug B bridge (02-MQTT-SPARKPLUG.md).
 *
 * <p><b>Safety ceiling:</b> {@code ADVISORY}. The bridge never publishes to a
 * {@code DCMD} topic that triggers a real field actuator — outbound traffic
 * goes only to a configurable advisory namespace consumed by an operator
 * HMI. {@code AUTONOMOUS} per-tag promotion is rejected at config load.
 *
 * <p><b>Layout:</b>
 * <ul>
 *   <li>{@link com.rakovpublic.jneuropallium.worker.bridge.mqtt.MqttBridgeConfig}
 *       / {@link com.rakovpublic.jneuropallium.worker.bridge.mqtt.MqttBridgeConfigLoader}
 *       — YAML config (00-FRAMEWORK §3, FAIL_ON_UNKNOWN_PROPERTIES).</li>
 *   <li>{@link com.rakovpublic.jneuropallium.worker.bridge.mqtt.MqttTopicBinding}
 *       — read/write binding (BridgeBinding).</li>
 *   <li>{@link com.rakovpublic.jneuropallium.worker.bridge.mqtt.SparkplugMetricResolver}
 *       — Sparkplug topic + metric → bindingId resolution.</li>
 *   <li>{@link com.rakovpublic.jneuropallium.worker.bridge.mqtt.MqttSignalMapper}
 *       — pure functions: payload bytes → typed Jneopallium signals.</li>
 *   <li>{@link com.rakovpublic.jneuropallium.worker.bridge.mqtt.MqttClientService}
 *       — lifecycle, BIRTH cache, DEATH alarm, advisory queue (AutoCloseable).</li>
 *   <li>{@link com.rakovpublic.jneuropallium.worker.bridge.mqtt.MqttMetricInput}
 *       / {@link com.rakovpublic.jneuropallium.worker.bridge.mqtt.MqttEventInput}
 *       — IInitInputs draining the per-binding signal queues.</li>
 *   <li>{@link com.rakovpublic.jneuropallium.worker.bridge.mqtt.MqttAdvisoryOutputAggregator}
 *       — IOutputAggregator publishing only to advisory topics.</li>
 * </ul>
 */
package com.rakovpublic.jneuropallium.worker.bridge.mqtt;
