/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.kafka.decoder;

import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;

import java.util.List;

/**
 * Format-specific decoder turning one Kafka record value into zero or more
 * Jneopallium signals (08-KAFKA.md §6).
 *
 * <p>One decoder per source format (Logstash JSON, Zeek conn-log JSON,
 * Suricata EVE JSON, osquery JSON, Avro …). New source formats are added by
 * dropping a new {@code PayloadDecoder} into this package — no other class
 * needs to change.
 *
 * <h2>Failure semantics</h2>
 * Decoders MUST throw a {@link DecoderException} on any malformed input.
 * The {@code KafkaClientService} translates that into the configured
 * failure policy (08-KAFKA.md §9 R1).
 */
public interface PayloadDecoder {

    /**
     * Decode one Kafka message value.
     *
     * @param topic        the source topic (for diagnostics)
     * @param key          the message key (may be {@code null})
     * @param value        the raw bytes (UTF-8 for JSON; binary for Avro)
     * @param signalTagPrefix tag prefix to stamp onto produced signals
     * @return zero or more signals — empty if the record decoded successfully
     *         but produced no signal (e.g. a benign Suricata flow)
     * @throws DecoderException on malformed input
     */
    List<IInputSignal> decode(String topic, String key, byte[] value, String signalTagPrefix)
            throws DecoderException;

    /** Stable name used for config lookup (e.g. {@code "LOGSTASH"}). */
    String name();

    /** Wraps any decoding failure with the offending topic + cause. */
    final class DecoderException extends Exception {
        public DecoderException(String message) { super(message); }
        public DecoderException(String message, Throwable cause) { super(message, cause); }
    }
}
