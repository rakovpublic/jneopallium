/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.swarm;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm.ConsensusProposalSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm.ConsensusVoteSignal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Layer 4 light-weight consensus participant. Counts unique YES / NO
 * votes per proposal; meets quorum on YES count ≥ k. Loop=2 / Epoch=1.
 */
public class ConsensusParticipantNeuron extends ModulatableNeuron implements IConsensusParticipantNeuron {

    private final Map<String, Set<String>> yes = new HashMap<>();
    private final Map<String, Set<String>> no = new HashMap<>();
    private int quorum = 3;
    private String voterId;

    public ConsensusParticipantNeuron() { super(); }
    public ConsensusParticipantNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    @Override public void setVoterId(String id) { this.voterId = id; }

    @Override
    public ConsensusProposalSignal propose(String proposalId, String state) {
        return new ConsensusProposalSignal(proposalId, state, voterId);
    }

    @Override
    public ConsensusVoteSignal vote(ConsensusProposalSignal p) {
        if (p == null) return null;
        ConsensusVoteSignal v = new ConsensusVoteSignal(p.getProposalId(), VoteKind.YES, voterId);
        tally(v);
        return v;
    }

    @Override
    public void tally(ConsensusVoteSignal v) {
        if (v == null || v.getProposalId() == null) return;
        if (v.getVote() == VoteKind.YES) yes.computeIfAbsent(v.getProposalId(), k -> new HashSet<>()).add(safeVoter(v));
        else if (v.getVote() == VoteKind.NO) no.computeIfAbsent(v.getProposalId(), k -> new HashSet<>()).add(safeVoter(v));
    }

    private static String safeVoter(ConsensusVoteSignal v) {
        return v.getVoterId() == null ? "anon-" + System.identityHashCode(v) : v.getVoterId();
    }

    @Override public boolean isQuorum(String proposalId) { return yesCount(proposalId) >= quorum; }
    @Override public int yesCount(String proposalId) { return yes.getOrDefault(proposalId, java.util.Collections.emptySet()).size(); }
    @Override public int noCount(String proposalId) { return no.getOrDefault(proposalId, java.util.Collections.emptySet()).size(); }
    @Override public void setQuorum(int q) { this.quorum = Math.max(1, q); }
    @Override public int getQuorum() { return quorum; }
}
