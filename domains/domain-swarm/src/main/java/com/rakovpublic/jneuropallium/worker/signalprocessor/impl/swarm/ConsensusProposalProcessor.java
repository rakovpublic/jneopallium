/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.swarm;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.swarm.IConsensusParticipantNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm.ConsensusProposalSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm.ConsensusVoteSignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor: casts a local YES vote on every received proposal
 * (a deployment can swap in a smarter policy by replacing the neuron's
 * implementation).
 */
public class ConsensusProposalProcessor implements ISignalProcessor<ConsensusProposalSignal, IConsensusParticipantNeuron> {

    private static final String DESCRIPTION = "Vote on a received consensus proposal";

    @Override
    @SuppressWarnings("unchecked")
    public <I extends ISignal> List<I> process(ConsensusProposalSignal input, IConsensusParticipantNeuron neuron) {
        List<I> out = new LinkedList<>();
        if (input == null || neuron == null) return out;
        ConsensusVoteSignal v = neuron.vote(input);
        if (v != null) out.add((I) v);
        return out;
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return ConsensusProposalProcessor.class; }
    @Override public Class<IConsensusParticipantNeuron> getNeuronClass() { return IConsensusParticipantNeuron.class; }
    @Override public Class<ConsensusProposalSignal> getSignalClass() { return ConsensusProposalSignal.class; }
}
