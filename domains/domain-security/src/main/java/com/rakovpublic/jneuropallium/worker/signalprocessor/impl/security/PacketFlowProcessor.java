/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.security;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.security.INetworkFlowNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.PacketSignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor: aggregates a {@link PacketSignal} into the
 * per-flow byte / packet counters held by an {@link INetworkFlowNeuron}.
 */
public class PacketFlowProcessor implements ISignalProcessor<PacketSignal, INetworkFlowNeuron> {

    private static final String DESCRIPTION = "Per-flow byte / packet aggregation";

    @Override
    public <I extends ISignal> List<I> process(PacketSignal input, INetworkFlowNeuron neuron) {
        if (input != null && neuron != null) neuron.accumulate(input);
        return new LinkedList<>();
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return PacketFlowProcessor.class; }
    @Override public Class<INetworkFlowNeuron> getNeuronClass() { return INetworkFlowNeuron.class; }
    @Override public Class<PacketSignal> getSignalClass() { return PacketSignal.class; }
}
