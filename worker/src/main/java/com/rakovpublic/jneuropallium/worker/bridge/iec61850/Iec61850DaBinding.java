/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.iec61850;

import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeBinding;
import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeBindingDirection;

import java.util.Objects;

/**
 * Resolved per-binding IEC 61850 Data Attribute read (11-IEC61850.md §6, §7).
 *
 * <p>Every binding is {@link BridgeBindingDirection#READ} — the IEC 61850
 * bridge has no write direction (§0, §3). The clamp / ramp / fail-safe
 * fields required by {@link BridgeBinding} are therefore {@code null};
 * they exist on the interface so the universal audit path treats an IEC
 * binding uniformly with industrial bindings, but the bridge never
 * invokes them (no write code path exists).
 */
public record Iec61850DaBinding(
        String bindingId,
        String iedId,
        String daPath,
        String signalTag,
        Iec61850BridgeConfig.TargetSignal targetSignal
) implements BridgeBinding {

    public Iec61850DaBinding {
        Objects.requireNonNull(bindingId, "bindingId");
        Objects.requireNonNull(iedId, "iedId");
        Objects.requireNonNull(daPath, "daPath");
        Objects.requireNonNull(signalTag, "signalTag");
        Objects.requireNonNull(targetSignal, "targetSignal");
    }

    public static Iec61850DaBinding fromConfig(Iec61850BridgeConfig.DaReadConfig r) {
        return new Iec61850DaBinding(
                r.bindingId(),
                r.iedId(),
                r.daPath(),
                r.signalTag(),
                r.targetSignal());
    }

    @Override public BridgeBindingDirection direction() { return BridgeBindingDirection.READ; }
    @Override public String loopId() { return bindingId; }
    @Override public Double failSafeValue() { return null; }
    @Override public Double rampRateMaxPerSec() { return null; }
    @Override public Double minClampValue() { return null; }
    @Override public Double maxClampValue() { return null; }
}
