/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.lsl;

import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeBinding;
import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeBindingDirection;

import java.util.List;

/**
 * Resolved per-stream binding (05-LSL.md §7). Implements
 * {@link BridgeBinding} so the universal §2.2 audit / clamp path treats
 * an LSL outlet write the same as any other bridge.
 *
 * <p>{@code channelIndices} maps the configured channel labels onto their
 * resolved offsets in the LSL stream's channel layout. The mapping is
 * computed once, when the stream is resolved (§9 S10) — a missing channel
 * fails fast at config-load through {@link LslClientService}.
 */
public record LslStreamBinding(
        String bindingId,
        BridgeBindingDirection direction,
        String streamName,
        String streamType,
        LslBridgeConfig.ReadSignalKind readKind,
        LslBridgeConfig.OutletKind outletKind,
        String signalTag,
        String signalTagPrefix,
        List<String> channelLabels,
        int[] channelIndices,
        int chunkLengthSamples,
        int decimateBy,
        String calibrationCueRegex,
        int ringBufferMaxSamples,
        Double minClampValue,
        Double maxClampValue,
        Double rampRateMaxPerSec,
        Double failSafeValue,
        boolean stimulationGated,
        double nominalSrate
) implements BridgeBinding {

    public LslStreamBinding {
        channelLabels = channelLabels == null ? List.of() : List.copyOf(channelLabels);
        channelIndices = channelIndices == null ? new int[0] : channelIndices.clone();
    }

    public static LslStreamBinding fromRead(LslBridgeConfig.ReadBindingConfig r,
                                            int[] channelIndices) {
        return new LslStreamBinding(
                r.bindingId(),
                BridgeBindingDirection.READ,
                r.streamName(),
                r.streamType(),
                r.targetSignal(),
                null,
                r.signalTag(),
                r.signalTagPrefix(),
                r.channels(),
                channelIndices,
                r.chunkLengthSamples(),
                r.decimateBy(),
                r.calibrationCueRegex(),
                r.ringBufferMaxSamples(),
                null, null, null, null,
                false,
                0.0);
    }

    public static LslStreamBinding fromWrite(LslBridgeConfig.WriteBindingConfig w) {
        return new LslStreamBinding(
                w.bindingId(),
                BridgeBindingDirection.WRITE,
                w.outletName(),
                null,
                null,
                w.type(),
                w.signalTag(),
                null,
                List.of(),
                new int[0],
                0, 1, null, 0,
                w.minClampValue(),
                w.maxClampValue(),
                w.rampRateMaxPerSec(),
                w.failSafeValue(),
                w.stimulationGated(),
                w.nominalSrate());
    }

    @Override public String loopId() { return bindingId; }

    @Override
    public int[] channelIndices() { return channelIndices.clone(); }
}
