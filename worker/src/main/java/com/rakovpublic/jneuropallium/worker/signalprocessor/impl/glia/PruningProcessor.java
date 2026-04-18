/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.glia;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.glia.MicroglialPruningNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.glia.PruningSignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor that, on arrival of a {@link PruningSignal},
 * records inactivity against a target connection and possibly emits
 * a follow-up prune request for a sibling connection.
 */
public class PruningProcessor implements ISignalProcessor<PruningSignal, MicroglialPruningNeuron> {

    private static final String DESCRIPTION = "Microglial pruning request handling";

    @Override
    @SuppressWarnings("unchecked")
    public <I extends ISignal> List<I> process(PruningSignal input, MicroglialPruningNeuron neuron) {
        List<I> out = new LinkedList<>();
        if (input == null || neuron == null) return out;
        neuron.tick();
        PruningSignal emitted = neuron.maybePrune(input.getAxonSourceId(), input.getAxonTargetId(), input.getReason());
        if (emitted != null) out.add((I) emitted);
        return out;
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return PruningProcessor.class; }
    @Override public Class<MicroglialPruningNeuron> getNeuronClass() { return MicroglialPruningNeuron.class; }
    @Override public Class<PruningSignal> getSignalClass() { return PruningSignal.class; }
}
