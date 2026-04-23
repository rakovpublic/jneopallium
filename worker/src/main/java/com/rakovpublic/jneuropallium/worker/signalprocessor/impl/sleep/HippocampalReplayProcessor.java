/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.sleep;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.sleep.IHippocampalReplayNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.sleep.ReplaySignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor: arrival of a {@link ReplaySignal} (request or
 * scheduler tick) prompts the {@link IHippocampalReplayNeuron} to emit
 * its top-K most-salient buffered episodes. This processor forwards
 * every emitted replay for downstream long-term memory consolidation.
 */
public class HippocampalReplayProcessor implements ISignalProcessor<ReplaySignal, IHippocampalReplayNeuron> {

    private static final String DESCRIPTION = "NREM replay of top-K salient episodes";

    @Override
    @SuppressWarnings("unchecked")
    public <I extends ISignal> List<I> process(ReplaySignal input, IHippocampalReplayNeuron neuron) {
        List<I> out = new LinkedList<>();
        if (input == null || neuron == null) return out;
        List<ReplaySignal> replays = neuron.emitTopK();
        if (replays != null) {
            for (ReplaySignal r : replays) if (r != null) out.add((I) r);
        }
        return out;
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return HippocampalReplayProcessor.class; }
    @Override public Class<IHippocampalReplayNeuron> getNeuronClass() { return IHippocampalReplayNeuron.class; }
    @Override public Class<ReplaySignal> getSignalClass() { return ReplaySignal.class; }
}
