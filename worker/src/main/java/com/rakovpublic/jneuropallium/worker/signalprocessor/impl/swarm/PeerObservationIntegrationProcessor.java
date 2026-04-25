/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.swarm;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.swarm.IPeerStateIntegrationNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm.PeerObservationSignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor: pushes a {@link PeerObservationSignal} into the
 * neighbourhood-view neuron. Uses the signal's epoch tick as "now"
 * for last-seen bookkeeping.
 */
public class PeerObservationIntegrationProcessor implements ISignalProcessor<PeerObservationSignal, IPeerStateIntegrationNeuron> {

    private static final String DESCRIPTION = "Local-view update from a peer observation";

    @Override
    public <I extends ISignal> List<I> process(PeerObservationSignal input, IPeerStateIntegrationNeuron neuron) {
        if (input != null && neuron != null) neuron.onObservation(input, input.getEpoch());
        return new LinkedList<>();
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return PeerObservationIntegrationProcessor.class; }
    @Override public Class<IPeerStateIntegrationNeuron> getNeuronClass() { return IPeerStateIntegrationNeuron.class; }
    @Override public Class<PeerObservationSignal> getSignalClass() { return PeerObservationSignal.class; }
}
