/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.canopen;

import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeBinding;
import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeBindingDirection;

/**
 * One CANopen (nodeId, odIndex, subIndex) ↔ signal-tag binding
 * (13-CANOPEN.md §5, §6, §7).
 *
 * <p>Implements {@link BridgeBinding} so the universal audit record
 * (00-FRAMEWORK §4) can identify the binding by tag and loop. For
 * {@code READ} and event bindings only the routing fields are meaningful;
 * clamp / rate / fail-safe fields are {@code null}.
 */
public record CanopenNodeBinding(
        String bindingId,
        int nodeId,
        int odIndex,
        int subIndex,
        BridgeBindingDirection direction,
        CanopenBridgeConfig.PdoSource pdoSource,
        CanopenBridgeConfig.OdType odType,
        CanopenBridgeConfig.WriteVia writeVia,
        CanopenBridgeConfig.ReadSignalKind targetSignal,
        CanopenBridgeConfig.EventSource eventSource,
        String signalTag,
        String signalTagPrefix,
        double scale,
        double offset,
        int decimateBy,
        Double minClampValue,
        Double maxClampValue,
        Double rampRateMaxPerSec,
        Double failSafeValue
) implements BridgeBinding {

    public static CanopenNodeBinding fromRead(CanopenBridgeConfig.ReadBindingConfig r) {
        return new CanopenNodeBinding(
                r.bindingId(), r.nodeId(), r.odIndex(), r.subIndex(),
                BridgeBindingDirection.READ,
                r.pdoSource(), r.odType(), null,
                r.targetSignal(), null,
                r.signalTag(), null,
                r.scale(), r.offset(), r.decimateBy(),
                null, null, null, null);
    }

    public static CanopenNodeBinding fromEvent(CanopenBridgeConfig.EventBindingConfig e) {
        return new CanopenNodeBinding(
                e.bindingId(), e.nodeId(), 0, 0,
                BridgeBindingDirection.READ,
                null, CanopenBridgeConfig.OdType.UINT16, null,
                e.targetSignal(), e.source(),
                null, e.signalTagPrefix(),
                1.0, 0.0, 1,
                null, null, null, null);
    }

    public static CanopenNodeBinding fromWrite(CanopenBridgeConfig.WriteBindingConfig w) {
        return new CanopenNodeBinding(
                w.bindingId(), w.nodeId(), w.odIndex(), w.subIndex(),
                BridgeBindingDirection.WRITE,
                null, w.odType(), w.via(),
                null, null,
                w.signalTag(), null,
                1.0, 0.0, 1,
                w.minClampValue(), w.maxClampValue(),
                w.rampRateMaxPerSec(), w.failSafeValue());
    }

    @Override public String loopId() { return bindingId; }
}
