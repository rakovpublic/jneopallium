/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.plc4x;

import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeBinding;
import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeBindingDirection;

import java.util.Objects;

/**
 * Runtime binding for one PLC4X write field (01-PLC4X.md §6).
 *
 * <p>Adapts {@link Plc4xConfig.WriteBindingConfig} onto the
 * {@link BridgeBinding} interface consumed by
 * {@link com.rakovpublic.jneuropallium.worker.bridge.common.AbstractBridgeOutputAggregator}.
 * The {@link #loopId()} is the {@code bindingId} from the YAML config —
 * also the key used to look up the per-tag safety mode.
 */
public record Plc4xFieldBinding(
        String bindingId,
        String connectionId,
        String fieldAddress,
        String signalTag,
        Double failSafeValue,
        Double rampRateMaxPerSec,
        Double minClampValue,
        Double maxClampValue
) implements BridgeBinding {

    public Plc4xFieldBinding {
        Objects.requireNonNull(bindingId, "bindingId");
        Objects.requireNonNull(connectionId, "connectionId");
        Objects.requireNonNull(fieldAddress, "fieldAddress");
        Objects.requireNonNull(signalTag, "signalTag");
    }

    @Override public String loopId() { return bindingId; }
    @Override public BridgeBindingDirection direction() { return BridgeBindingDirection.WRITE; }

    public static Plc4xFieldBinding from(Plc4xConfig.WriteBindingConfig cfg) {
        return new Plc4xFieldBinding(
                cfg.bindingId(),
                cfg.connectionId(),
                cfg.fieldAddress(),
                cfg.signalTag(),
                cfg.failSafeValue(),
                cfg.rampRateMaxPerSec(),
                cfg.minClampValue(),
                cfg.maxClampValue());
    }
}
