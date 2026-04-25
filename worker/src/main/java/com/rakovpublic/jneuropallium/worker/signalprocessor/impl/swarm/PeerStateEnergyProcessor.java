/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.swarm;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.swarm.IEnergyCoordinatorNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm.PeerStateSignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor: feeds a {@link PeerStateSignal} to the energy
 * coordinator so battery-aware task deferral remains current.
 */
public class PeerStateEnergyProcessor implements ISignalProcessor<PeerStateSignal, IEnergyCoordinatorNeuron> {

    private static final String DESCRIPTION = "Battery-aware peer-state update";

    @Override
    public <I extends ISignal> List<I> process(PeerStateSignal input, IEnergyCoordinatorNeuron neuron) {
        if (input != null && neuron != null) neuron.onPeerState(input);
        return new LinkedList<>();
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return PeerStateEnergyProcessor.class; }
    @Override public Class<IEnergyCoordinatorNeuron> getNeuronClass() { return IEnergyCoordinatorNeuron.class; }
    @Override public Class<PeerStateSignal> getSignalClass() { return PeerStateSignal.class; }
}
