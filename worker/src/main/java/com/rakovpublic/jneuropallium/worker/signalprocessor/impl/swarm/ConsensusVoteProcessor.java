/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.swarm;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.swarm.IConsensusParticipantNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm.ConsensusVoteSignal;

import java.util.LinkedList;
import java.util.List;

/** Stateless processor: tallies an incoming vote on the consensus participant. */
public class ConsensusVoteProcessor implements ISignalProcessor<ConsensusVoteSignal, IConsensusParticipantNeuron> {

    private static final String DESCRIPTION = "Tally a peer-cast consensus vote";

    @Override
    public <I extends ISignal> List<I> process(ConsensusVoteSignal input, IConsensusParticipantNeuron neuron) {
        if (input != null && neuron != null) neuron.tally(input);
        return new LinkedList<>();
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return ConsensusVoteProcessor.class; }
    @Override public Class<IConsensusParticipantNeuron> getNeuronClass() { return IConsensusParticipantNeuron.class; }
    @Override public Class<ConsensusVoteSignal> getSignalClass() { return ConsensusVoteSignal.class; }
}
