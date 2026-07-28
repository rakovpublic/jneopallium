/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.affect;

import com.rakovpublic.jneuropallium.ai.signals.fast.SpikeSignal;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.affect.AmygdalaValenceNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.affect.IAffectiveNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor that tags incoming {@link SpikeSignal}s with valence
 * via the {@link AmygdalaValenceNeuron}.
 */
public class ValenceTaggingProcessor implements ISignalProcessor<SpikeSignal, IAffectiveNeuron> {

    private static final String DESCRIPTION = "Amygdala-like fast valence tagging of sensory spikes";

    @Override
    @SuppressWarnings("unchecked")
    public <I extends ISignal> List<I> process(SpikeSignal input, IAffectiveNeuron neuron) {
        if (input == null || neuron == null) return new LinkedList<>();
        if (neuron instanceof AmygdalaValenceNeuron) {
            double threat = input.getBurstCount() > 2 ? Math.min(1.0, 0.3 * input.getBurstCount()) : 0.0;
            ((AmygdalaValenceNeuron) neuron).tag(input.getMagnitude(), threat);
        }
        return new LinkedList<>();
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return ValenceTaggingProcessor.class; }
    @Override public Class<IAffectiveNeuron> getNeuronClass() { return IAffectiveNeuron.class; }
    @Override public Class<SpikeSignal> getSignalClass() { return SpikeSignal.class; }
}
