/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.swarm;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.swarm.IAuctionBiddingNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm.TaskAnnouncementSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm.TaskBidSignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor: produces a bid signal for every task
 * announcement the agent is interested in.
 */
public class TaskAnnouncementBidProcessor implements ISignalProcessor<TaskAnnouncementSignal, IAuctionBiddingNeuron> {

    private static final String DESCRIPTION = "Auction bid generation from task announcement";

    @Override
    @SuppressWarnings("unchecked")
    public <I extends ISignal> List<I> process(TaskAnnouncementSignal input, IAuctionBiddingNeuron neuron) {
        List<I> out = new LinkedList<>();
        if (input == null || neuron == null) return out;
        TaskBidSignal b = neuron.bid(input);
        if (b != null) out.add((I) b);
        return out;
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return TaskAnnouncementBidProcessor.class; }
    @Override public Class<IAuctionBiddingNeuron> getNeuronClass() { return IAuctionBiddingNeuron.class; }
    @Override public Class<TaskAnnouncementSignal> getSignalClass() { return TaskAnnouncementSignal.class; }
}
