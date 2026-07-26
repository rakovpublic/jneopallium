/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.lti;

import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeBinding;
import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeBindingDirection;

import java.util.Objects;

/**
 * Resolved per-verb xAPI read binding (14-LTI-XAPI.md §7).
 *
 * <p>READ-only — the bridge consumes statements from an LRS (pull) or
 * accepts them at an endpoint (push) and decodes them into Jneopallium
 * signals. Write bindings are modelled separately by
 * {@link LtiBridgeConfig.WriteBindingConfig} and consumed by
 * {@link LtiAdvisoryOutputAggregator}.
 */
public record XapiVerbBinding(
        String bindingId,
        String xapiVerb,
        LtiBridgeConfig.TargetSignal targetSignal,
        String signalTagPrefix
) implements BridgeBinding {

    public XapiVerbBinding {
        Objects.requireNonNull(bindingId, "bindingId");
        Objects.requireNonNull(xapiVerb, "xapiVerb");
        Objects.requireNonNull(targetSignal, "targetSignal");
    }

    public static XapiVerbBinding fromConfig(LtiBridgeConfig.ReadBindingConfig r) {
        return new XapiVerbBinding(r.bindingId(), r.xapiVerb(), r.targetSignal(),
                r.signalTagPrefix());
    }

    @Override public String signalTag() { return signalTagPrefix; }
    @Override public BridgeBindingDirection direction() { return BridgeBindingDirection.READ; }
    @Override public String loopId() { return bindingId; }
    @Override public Double failSafeValue() { return null; }
    @Override public Double rampRateMaxPerSec() { return null; }
    @Override public Double minClampValue() { return null; }
    @Override public Double maxClampValue() { return null; }
}
