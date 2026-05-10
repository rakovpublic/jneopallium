/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.ros2;

import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeBinding;
import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeBindingDirection;

/**
 * One ROS 2 topic ↔ signal-tag binding (04-ROS2-DDS.md §5, §7).
 *
 * <p>For READ bindings {@code topic} is the subscribed ROS 2 topic; the
 * binding additionally carries the message type and a per-binding decimation
 * factor. For WRITE bindings {@code topic} is the advisory namespace topic
 * that the operator's autonomy supervisor consumes.
 *
 * <p>Implements {@link BridgeBinding} so the universal audit record
 * (00-FRAMEWORK §4) can identify the binding by tag and loop, and so write
 * bindings can carry their clamp / rate / fail-safe metadata.
 */
public record Ros2TopicBinding(
        String bindingId,
        String topic,
        String msgType,
        String signalTag,
        BridgeBindingDirection direction,
        Ros2BridgeConfig.ReadSignalKind readKind,
        boolean asPeerObservation,
        String peerId,
        int decimateBy,
        int maxRangeBins,
        int maxPayloadBytes,
        Double minClampValue,
        Double maxClampValue,
        Double rampRateMaxPerSec,
        Double failSafeValue
) implements BridgeBinding {

    public static Ros2TopicBinding fromRead(Ros2BridgeConfig.ReadBindingConfig r) {
        return new Ros2TopicBinding(
                r.bindingId(), r.topic(), r.msgType(), r.signalTag(),
                BridgeBindingDirection.READ,
                r.signalKind(), r.asPeerObservation(), r.peerId(),
                r.decimateBy(), r.maxRangeBins(), r.maxPayloadBytes(),
                null, null, null, null);
    }

    public static Ros2TopicBinding fromWrite(Ros2BridgeConfig.WriteBindingConfig w) {
        return new Ros2TopicBinding(
                w.bindingId(), w.topic(), w.msgType(), w.signalTag(),
                BridgeBindingDirection.WRITE,
                null, false, null, 1, 0, 0,
                w.minClampValue(), w.maxClampValue(),
                w.rampRateMaxPerSec(), w.failSafeValue());
    }

    @Override public String loopId() { return bindingId; }
}
