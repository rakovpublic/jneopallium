/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.swarm;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.swarm.IPeerStateIntegrationNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm.PeerStateSignal;

import java.util.LinkedList;
import java.util.List;

/** Stateless processor: feeds a {@link PeerStateSignal} into the neighbourhood-view neuron. */
public class PeerStateIntegrationProcessor implements ISignalProcessor<PeerStateSignal, IPeerStateIntegrationNeuron> {

    private static final String DESCRIPTION = "Local-view update from a peer state broadcast";

    @Override
    public <I extends ISignal> List<I> process(PeerStateSignal input, IPeerStateIntegrationNeuron neuron) {
        if (input != null && neuron != null) neuron.onState(input, input.getEpoch());
        return new LinkedList<>();
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return PeerStateIntegrationProcessor.class; }
    @Override public Class<IPeerStateIntegrationNeuron> getNeuronClass() { return IPeerStateIntegrationNeuron.class; }
    @Override public Class<PeerStateSignal> getSignalClass() { return PeerStateSignal.class; }
}
