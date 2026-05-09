/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.ditto;

import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeBinding;
import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeBindingDirection;

/**
 * One Ditto thing/feature/property ↔ signal-tag binding (10-DITTO.md §4).
 *
 * <p>The {@code featurePath} is the canonical address {@code thingId/feature/property}
 * used as the default {@code signalTag} when one is not configured. Implements
 * {@link BridgeBinding} so the universal audit record (00-FRAMEWORK §4)
 * identifies the binding by tag and so write bindings can carry their
 * clamp range.
 */
public record DittoFeatureBinding(
        String bindingId,
        String thingId,
        String feature,
        String property,
        String signalTag,
        BridgeBindingDirection direction,
        Double minClampValue,
        Double maxClampValue
) implements BridgeBinding {

    public static DittoFeatureBinding fromRead(DittoBridgeConfig.ReadBindingConfig r) {
        String tag = (r.signalTag() == null || r.signalTag().isBlank())
                ? defaultTag(r.thingId(), r.feature(), r.property())
                : r.signalTag();
        return new DittoFeatureBinding(r.bindingId(), r.thingId(), r.feature(), r.property(),
                tag, BridgeBindingDirection.READ, null, null);
    }

    public static DittoFeatureBinding fromWrite(DittoBridgeConfig.WriteBindingConfig w) {
        String tag = (w.signalTag() == null || w.signalTag().isBlank())
                ? defaultTag(w.thingId(), w.feature(), w.property())
                : w.signalTag();
        return new DittoFeatureBinding(w.bindingId(), w.thingId(), w.feature(), w.property(),
                tag, BridgeBindingDirection.WRITE,
                w.minClampValue(), w.maxClampValue());
    }

    /** Canonical {@code thingId/feature/property} address. */
    public static String defaultTag(String thingId, String feature, String property) {
        return thingId + "/" + feature + "/" + property;
    }

    /** Canonical {@code thingId/feature/property} address for this binding. */
    public String featurePath() { return defaultTag(thingId, feature, property); }

    @Override public String loopId() { return bindingId; }
    @Override public Double failSafeValue() { return null; }
    @Override public Double rampRateMaxPerSec() { return null; }
}
