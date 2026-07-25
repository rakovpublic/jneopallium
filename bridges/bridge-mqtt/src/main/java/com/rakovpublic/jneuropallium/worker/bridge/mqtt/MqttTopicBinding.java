/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.mqtt;

import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeBinding;
import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeBindingDirection;

/**
 * One MQTT topic ↔ signal-tag binding (02-MQTT-SPARKPLUG.md §6).
 *
 * <p>For READ bindings, {@code topic} is either a Sparkplug topic prefix
 * ({@code spBv1.0/<group>/...}) or a plain MQTT topic. For WRITE bindings
 * {@code topic} is the advisory topic the egress aggregator publishes to.
 *
 * <p>Implements {@link BridgeBinding} so the universal audit record
 * (00-FRAMEWORK §4) can identify the binding by tag and loop, and so write
 * bindings can carry their clamp range.
 */
public record MqttTopicBinding(
        String bindingId,
        String topic,
        String signalTag,
        String sparkplugMetric,
        BridgeBindingDirection direction,
        Double minClampValue,
        Double maxClampValue
) implements BridgeBinding {

    public static MqttTopicBinding fromRead(MqttBridgeConfig.ReadBindingConfig r) {
        String topic = r.sparkplugMetric() != null ? r.sparkplugMetric() : r.plainMqttTopic();
        return new MqttTopicBinding(r.bindingId(), topic, r.signalTag(),
                r.sparkplugMetric(), BridgeBindingDirection.READ, null, null);
    }

    public static MqttTopicBinding fromWrite(MqttBridgeConfig.WriteBindingConfig w) {
        return new MqttTopicBinding(w.bindingId(), w.advisoryTopic(), w.signalTag(),
                w.sparkplugMetric(), BridgeBindingDirection.WRITE,
                w.minClampValue(), w.maxClampValue());
    }

    @Override public String loopId() { return bindingId; }
    @Override public Double failSafeValue() { return null; }
    @Override public Double rampRateMaxPerSec() { return null; }
}
