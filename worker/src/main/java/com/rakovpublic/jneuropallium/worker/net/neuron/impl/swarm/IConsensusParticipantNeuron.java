package com.rakovpublic.jneuropallium.worker.net.neuron.impl.swarm;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm.ConsensusProposalSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm.ConsensusVoteSignal;

public interface IConsensusParticipantNeuron extends IModulatableNeuron {
    /** Propose a value; returns the proposal record. */
    ConsensusProposalSignal propose(String proposalId, String state);
    /** Cast a local vote on a proposal. Returns the resulting vote signal. */
    ConsensusVoteSignal vote(ConsensusProposalSignal p);
    /** Tally a vote arriving from a peer. */
    void tally(ConsensusVoteSignal v);
    /** True iff the proposal has reached the configured quorum YES count. */
    boolean isQuorum(String proposalId);
    int yesCount(String proposalId);
    int noCount(String proposalId);
    void setQuorum(int q);
    int getQuorum();
    void setVoterId(String id);
}
