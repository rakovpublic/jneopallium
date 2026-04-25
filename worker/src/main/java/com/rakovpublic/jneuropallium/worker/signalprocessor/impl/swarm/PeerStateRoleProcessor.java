/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.swarm;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.swarm.IRoleAwarenessNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm.PeerStateSignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor: updates the role-awareness neuron with the
 * incoming peer state. Drives emergent role specialisation.
 */
public class PeerStateRoleProcessor implements ISignalProcessor<PeerStateSignal, IRoleAwarenessNeuron> {

    private static final String DESCRIPTION = "Role-distribution awareness update";

    @Override
    public <I extends ISignal> List<I> process(PeerStateSignal input, IRoleAwarenessNeuron neuron) {
        if (input != null && neuron != null) neuron.onPeerState(input);
        return new LinkedList<>();
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return PeerStateRoleProcessor.class; }
    @Override public Class<IRoleAwarenessNeuron> getNeuronClass() { return IRoleAwarenessNeuron.class; }
    @Override public Class<PeerStateSignal> getSignalClass() { return PeerStateSignal.class; }
}
