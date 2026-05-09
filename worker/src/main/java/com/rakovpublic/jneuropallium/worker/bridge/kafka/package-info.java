/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
/**
 * Apache Kafka bridge (08-KAFKA.md).
 *
 * <p>Bidirectional adapter between Kafka topics carrying enterprise telemetry
 * (Logstash logs, Zeek conn-logs, Suricata EVE alerts, osquery events,
 * upstream anomaly scores) and the Jneopallium cybersecurity signal pipeline.
 * The bridge implements 00-FRAMEWORK.md §0 ground rules and §4 audit schema.
 *
 * <p>Unlike industrial bridges (PLC4X, OPC UA, FMI), this bridge is
 * <i>event-shaped</i>: there is no fail-safe value, no clamp, and no
 * rate-limit at the topic level — those concerns belong to the SOAR / EDR
 * consumer of the advisory topics. Hence the bridge implements
 * {@link com.rakovpublic.jneuropallium.worker.application.IOutputAggregator}
 * directly rather than extending {@link
 * com.rakovpublic.jneuropallium.worker.bridge.common.AbstractBridgeOutputAggregator}.
 *
 * <h2>Safety ceiling</h2>
 * 08-KAFKA.md §1 fixes the safety ceiling at {@code ADVISORY}. Any
 * {@code AUTONOMOUS} promotion in {@code perTagSafetyMode} is rejected by
 * {@link com.rakovpublic.jneuropallium.worker.bridge.kafka.KafkaBridgeConfig}
 * at load time (defence in depth — the same rule is also documented in
 * 08-KAFKA.md §5).
 *
 * <h2>Test seam</h2>
 * The bridge talks to Kafka through the {@link
 * com.rakovpublic.jneuropallium.worker.bridge.kafka.KafkaTransport}
 * abstraction so unit tests can inject an in-memory broker. Production
 * wiring uses {@link
 * com.rakovpublic.jneuropallium.worker.bridge.kafka.DefaultKafkaTransport}.
 *
 * <h2>Decoders</h2>
 * Source-format decoders live in {@code decoder/}. New formats are added by
 * implementing {@link
 * com.rakovpublic.jneuropallium.worker.bridge.kafka.decoder.PayloadDecoder}
 * and registering it on the {@link
 * com.rakovpublic.jneuropallium.worker.bridge.kafka.KafkaSignalMapper} —
 * no other class needs to change.
 */
package com.rakovpublic.jneuropallium.worker.bridge.kafka;
