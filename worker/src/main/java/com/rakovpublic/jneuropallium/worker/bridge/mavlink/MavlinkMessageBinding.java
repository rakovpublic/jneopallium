/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.mavlink;

import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeBinding;
import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeBindingDirection;

/**
 * One MAVLink (system, component, messageType) ↔ signal-tag binding
 * (12-MAVLINK.md §5, §6, §8).
 *
 * <p>Implements {@link BridgeBinding} so the universal audit record
 * (00-FRAMEWORK §4) can identify the binding by tag and loop, and so write
 * bindings can carry their clamp / rate / fail-safe metadata.
 *
 * <p>For {@code READ} and event-class bindings only the routing fields are
 * meaningful; clamp and rate fields are {@code null}. For {@code WRITE}
 * bindings the {@code targetSystemId}/{@code targetComponentId} pair names
 * the destination system on the connection.
 */
public record MavlinkMessageBinding(
        String bindingId,
        String connectionId,
        BridgeBindingDirection direction,
        Integer systemId,
        Integer componentId,
        String messageType,
        String signalTag,
        MavlinkBridgeConfig.ReadSignalKind targetSignal,
        String peerId,
        String signalTagPrefix,
        int decimateBy,
        Double minClampValue,
        Double maxClampValue,
        Double rampRateMaxPerSec,
        Double failSafeValue
) implements BridgeBinding {

    public static MavlinkMessageBinding fromRead(MavlinkBridgeConfig.ReadBindingConfig r) {
        return new MavlinkMessageBinding(
                r.bindingId(), r.connectionId(),
                BridgeBindingDirection.READ,
                r.systemId(), r.componentId(),
                r.messageType(), r.signalTag(),
                r.targetSignal(), r.peerId(), null,
                r.decimateBy(),
                null, null, null, null);
    }

    public static MavlinkMessageBinding fromEvent(MavlinkBridgeConfig.EventBindingConfig e) {
        return new MavlinkMessageBinding(
                e.bindingId(), e.connectionId(),
                BridgeBindingDirection.READ,
                null, null,
                e.messageType(), null,
                e.targetSignal(), null, e.signalTagPrefix(),
                1,
                null, null, null, null);
    }

    public static MavlinkMessageBinding fromWrite(MavlinkBridgeConfig.WriteBindingConfig w) {
        return new MavlinkMessageBinding(
                w.bindingId(), w.connectionId(),
                BridgeBindingDirection.WRITE,
                w.targetSystemId(), w.targetComponentId(),
                w.messageType(), w.signalTag(),
                null, null, null,
                1,
                w.minClampValue(), w.maxClampValue(),
                w.rampRateMaxPerSec(), w.failSafeValue());
    }

    @Override public String loopId() { return bindingId; }
}
