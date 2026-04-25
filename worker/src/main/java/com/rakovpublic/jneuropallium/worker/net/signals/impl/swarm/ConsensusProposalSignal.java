/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/** Light-weight consensus proposal. ProcessingFrequency: loop=2, epoch=1. */
public class ConsensusProposalSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(1L, 2);

    private String proposalId;
    private String proposedState;
    private String proposerId;

    public ConsensusProposalSignal() { super(); this.loop = 2; this.epoch = 1L; this.timeAlive = 500; }

    public ConsensusProposalSignal(String proposalId, String proposedState, String proposerId) {
        this();
        this.proposalId = proposalId;
        this.proposedState = proposedState;
        this.proposerId = proposerId;
    }

    public String getProposalId() { return proposalId; }
    public void setProposalId(String p) { this.proposalId = p; }
    public String getProposedState() { return proposedState; }
    public void setProposedState(String s) { this.proposedState = s; }
    public String getProposerId() { return proposerId; }
    public void setProposerId(String p) { this.proposerId = p; }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return ConsensusProposalSignal.class; }
    @Override public String getDescription() { return "ConsensusProposalSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        ConsensusProposalSignal c = new ConsensusProposalSignal(proposalId, proposedState, proposerId);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
