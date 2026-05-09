/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.fmi;

import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeBinding;
import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeBindingDirection;

import java.util.Objects;

/**
 * Runtime binding for one FMU write variable (03-FMI-FMU.md §5).
 *
 * <p>Adapts {@link FmiBridgeConfig.WriteBindingConfig} onto the
 * {@link BridgeBinding} interface consumed by
 * {@link com.rakovpublic.jneuropallium.worker.bridge.common.AbstractBridgeOutputAggregator}.
 * The {@link #loopId()} is the {@code bindingId} from the YAML config, which
 * is also the key used to look up the per-tag safety mode.
 */
public record FmuVariableBinding(
        String bindingId,
        String fmuVariable,
        String signalTag,
        int valueReference,
        Double failSafeValue,
        Double rampRateMaxPerSec,
        Double minClampValue,
        Double maxClampValue
) implements BridgeBinding {

    public FmuVariableBinding {
        Objects.requireNonNull(bindingId, "bindingId");
        Objects.requireNonNull(fmuVariable, "fmuVariable");
        Objects.requireNonNull(signalTag, "signalTag");
    }

    @Override public String loopId() { return bindingId; }
    @Override public BridgeBindingDirection direction() { return BridgeBindingDirection.WRITE; }

    /** Build from config + resolved valueReference. */
    public static FmuVariableBinding of(FmiBridgeConfig.WriteBindingConfig cfg, int valueReference) {
        return new FmuVariableBinding(
                cfg.bindingId(),
                cfg.fmuVariable(),
                cfg.signalTag(),
                valueReference,
                cfg.failSafeValue(),
                cfg.rampRateMaxPerSec(),
                cfg.minClampValue(),
                cfg.maxClampValue());
    }
}
