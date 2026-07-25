/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.kafka;

import com.rakovpublic.jneuropallium.worker.bridge.kafka.decoder.AnomalyScoreJsonDecoder;
import com.rakovpublic.jneuropallium.worker.bridge.kafka.decoder.LogstashJsonDecoder;
import com.rakovpublic.jneuropallium.worker.bridge.kafka.decoder.OsqueryDecoder;
import com.rakovpublic.jneuropallium.worker.bridge.kafka.decoder.PayloadDecoder;
import com.rakovpublic.jneuropallium.worker.bridge.kafka.decoder.SuricataEveDecoder;
import com.rakovpublic.jneuropallium.worker.bridge.kafka.decoder.ZeekConnDecoder;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Registry that resolves a {@code decoder} string from a
 * {@link KafkaBridgeConfig.ReadBindingConfig} into a concrete
 * {@link PayloadDecoder} (08-KAFKA.md §6).
 *
 * <p>Comes pre-loaded with the built-in decoders. A deployment that has
 * additional sources (a custom JSON shape, an Avro schema with Schema
 * Registry, …) calls {@link #register} at startup before passing the
 * mapper to {@link KafkaClientService}.
 */
public final class KafkaSignalMapper {

    private final Map<String, PayloadDecoder> decoders = new HashMap<>();

    public KafkaSignalMapper() {
        register(new LogstashJsonDecoder());
        register(new ZeekConnDecoder());
        register(new SuricataEveDecoder());
        register(new OsqueryDecoder());
        register(new AnomalyScoreJsonDecoder());
    }

    public KafkaSignalMapper register(PayloadDecoder decoder) {
        Objects.requireNonNull(decoder, "decoder");
        decoders.put(decoder.name(), decoder);
        return this;
    }

    /**
     * Resolve a decoder for the given {@link KafkaBridgeConfig.ReadBindingConfig}.
     *
     * @throws IllegalArgumentException when the binding's {@code decoder}
     *         attribute does not match a registered decoder name
     */
    public PayloadDecoder resolve(KafkaBridgeConfig.ReadBindingConfig r) {
        String name = r.decoder() == null ? defaultName(r.targetSignal()) : r.decoder();
        PayloadDecoder d = decoders.get(name);
        if (d == null) {
            throw new IllegalArgumentException(
                    "No decoder registered for binding '" + r.bindingId() + "' (decoder=" + name + ")");
        }
        return d;
    }

    private static String defaultName(KafkaBridgeConfig.TargetSignal target) {
        return switch (target) {
            case LOG_EVENT -> LogstashJsonDecoder.NAME;
            case PACKET -> ZeekConnDecoder.NAME;
            case SIGNATURE_MATCH -> SuricataEveDecoder.NAME;
            case SYSCALL -> OsqueryDecoder.NAME;
            case ANOMALY_SCORE -> AnomalyScoreJsonDecoder.NAME;
        };
    }
}
