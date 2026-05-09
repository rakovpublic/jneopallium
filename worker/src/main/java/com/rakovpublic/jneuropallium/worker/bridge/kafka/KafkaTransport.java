/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.kafka;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Test seam between {@link KafkaClientService} and the
 * {@code org.apache.kafka.clients} library (08-KAFKA.md §3).
 *
 * <p>Production wiring uses the real {@code KafkaConsumer} +
 * {@code KafkaProducer}; tests inject an in-memory implementation so the
 * scenarios in §8 can run without a broker on the CI classpath. Same pattern
 * as {@code Plc4xDriver} in the PLC4X bridge.
 */
public interface KafkaTransport extends AutoCloseable {

    /** One Kafka record observed by a consumer. Bytes-only — decoding lives in {@code PayloadDecoder}. */
    record InboundRecord(String topic, int partition, long offset, String key, byte[] value) {}

    /** Subscribe a per-binding consumer to {@code topic} for {@code groupId}. */
    void subscribe(String bindingId, String topic, String groupId);

    /**
     * Poll one batch from the named binding. Returns {@code []} if nothing new.
     *
     * @param bindingId one previously passed to {@link #subscribe}
     * @param maxRecords upper bound (08-KAFKA.md §5 {@code maxPollRecords})
     */
    List<InboundRecord> poll(String bindingId, Duration timeout, int maxRecords);

    /**
     * Synchronously commit the offsets implied by the records returned from
     * the most recent {@link #poll} for {@code bindingId} (08-KAFKA.md §8 S9 —
     * at-least-once semantics).
     */
    void commitSync(String bindingId, Map<Integer, Long> partitionToOffset);

    /**
     * Send one record to {@code topic}. Throws {@link KafkaTransportException}
     * on a transport-level failure; {@link KafkaClientService} translates the
     * throw into a {@code FAILED} audit verdict.
     */
    void send(String topic, String key, byte[] value);

    /** Shut down both consumers and the producer; idempotent. */
    @Override
    void close();

    /** Wraps any Kafka client throwable so the bridge can audit it uniformly. */
    final class KafkaTransportException extends RuntimeException {
        public KafkaTransportException(String message) { super(message); }
        public KafkaTransportException(String message, Throwable cause) { super(message, cause); }
    }
}
