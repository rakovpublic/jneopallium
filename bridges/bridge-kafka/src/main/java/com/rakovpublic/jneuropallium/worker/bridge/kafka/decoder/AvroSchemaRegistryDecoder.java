/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.kafka.decoder;

import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;

import java.util.List;
import java.util.Objects;

/**
 * Pluggable Avro / Schema-Registry decoder (08-KAFKA.md §2, §10 R2).
 *
 * <p>The Confluent {@code kafka-avro-serializer} and
 * {@code kafka-schema-registry-client} jars are optional dependencies for
 * deployments that wire up Schema Registry. Rather than pull them into the
 * default worker classpath, this class delegates the byte-stream → JSON
 * conversion to a user-supplied {@link AvroToJsonConverter} and then hands
 * the JSON to a wrapped {@link PayloadDecoder} (typically
 * {@link AnomalyScoreJsonDecoder}).
 *
 * <p>A site that uses Schema Registry implements
 * {@link AvroToJsonConverter} on top of {@code KafkaAvroDeserializer} and
 * passes it in. Tests inject an in-memory converter that returns canned JSON.
 *
 * <p>If the registry is unavailable and {@code mandatory=true} (08-KAFKA.md
 * §10 R2), the {@code KafkaClientService} fails fast at startup; this
 * decoder simply propagates whatever {@link DecoderException} the converter
 * throws.
 */
public final class AvroSchemaRegistryDecoder implements PayloadDecoder {

    public static final String NAME = "AVRO_SCHEMA_REGISTRY";

    /** Adapter from raw Avro bytes to a JSON byte-array the wrapped decoder can read. */
    @FunctionalInterface
    public interface AvroToJsonConverter {
        byte[] toJson(String topic, byte[] avroValue) throws DecoderException;
    }

    private final AvroToJsonConverter converter;
    private final PayloadDecoder jsonDelegate;

    public AvroSchemaRegistryDecoder(AvroToJsonConverter converter, PayloadDecoder jsonDelegate) {
        this.converter = Objects.requireNonNull(converter, "converter");
        this.jsonDelegate = Objects.requireNonNull(jsonDelegate, "jsonDelegate");
    }

    @Override
    public List<IInputSignal> decode(String topic, String key, byte[] value, String signalTagPrefix)
            throws DecoderException {
        byte[] json = converter.toJson(topic, value);
        return jsonDelegate.decode(topic, key, json, signalTagPrefix);
    }

    @Override public String name() { return NAME; }
}
