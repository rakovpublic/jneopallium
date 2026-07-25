/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.kafka;

import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeBinding;
import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeBindingDirection;

/**
 * One Kafka topic ↔ signal-tag binding (08-KAFKA.md §6).
 *
 * <p>The Kafka bridge is event-shaped, not control-loop-shaped: there are no
 * fail-safes, no clamps, no rate limits at the topic level (those concerns
 * belong to the SOAR / EDR consumer of the advisory topic). This binding
 * therefore implements {@link BridgeBinding} purely so the universal audit
 * record schema (00-FRAMEWORK §4) can identify the binding by tag and loop.
 */
public record KafkaTopicBinding(
        String bindingId,
        String topic,
        String signalTag,
        BridgeBindingDirection direction
) implements BridgeBinding {

    public static KafkaTopicBinding fromRead(KafkaBridgeConfig.ReadBindingConfig r) {
        String tag = r.signalTagPrefix() == null ? r.bindingId() : r.signalTagPrefix();
        return new KafkaTopicBinding(r.bindingId(), r.topic(), tag, BridgeBindingDirection.READ);
    }

    public static KafkaTopicBinding fromWrite(KafkaBridgeConfig.WriteBindingConfig w) {
        return new KafkaTopicBinding(w.bindingId(), w.topic(), w.signalTag(),
                BridgeBindingDirection.WRITE);
    }

    @Override public String loopId() { return bindingId; }
    @Override public Double failSafeValue() { return null; }
    @Override public Double rampRateMaxPerSec() { return null; }
    @Override public Double minClampValue() { return null; }
    @Override public Double maxClampValue() { return null; }
}
