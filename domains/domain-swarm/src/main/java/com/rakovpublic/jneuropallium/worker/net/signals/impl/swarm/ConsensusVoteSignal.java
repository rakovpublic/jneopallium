/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.swarm.VoteKind;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/** Single vote on a consensus proposal. ProcessingFrequency: loop=2, epoch=1. */
public class ConsensusVoteSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(1L, 2);

    private String proposalId;
    private VoteKind vote;
    private String voterId;

    public ConsensusVoteSignal() {
        super();
        this.loop = 2;
        this.epoch = 1L;
        this.timeAlive = 500;
        this.vote = VoteKind.ABSTAIN;
    }

    public ConsensusVoteSignal(String proposalId, VoteKind vote, String voterId) {
        this();
        this.proposalId = proposalId;
        this.vote = vote == null ? VoteKind.ABSTAIN : vote;
        this.voterId = voterId;
    }

    public String getProposalId() { return proposalId; }
    public void setProposalId(String p) { this.proposalId = p; }
    public VoteKind getVote() { return vote; }
    public void setVote(VoteKind v) { this.vote = v == null ? VoteKind.ABSTAIN : v; }
    public String getVoterId() { return voterId; }
    public void setVoterId(String v) { this.voterId = v; }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return ConsensusVoteSignal.class; }
    @Override public String getDescription() { return "ConsensusVoteSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        ConsensusVoteSignal c = new ConsensusVoteSignal(proposalId, vote, voterId);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
