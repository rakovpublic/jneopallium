/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.BatchStateSignal;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Layer 4 campaign planner. Queues target batch-state transitions and
 * emits the next one on demand. Loop=2 / Epoch=5.
 */
public class CampaignPlanningNeuron extends ModulatableNeuron implements ICampaignPlanningNeuron {

    private static final class Entry { String id; BatchStateSignal target; }

    private final Deque<Entry> queue = new ArrayDeque<>();

    public CampaignPlanningNeuron() { super(); }
    public CampaignPlanningNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    @Override
    public void enqueueCampaign(String campaignId, BatchStateSignal target) {
        if (campaignId == null || target == null) return;
        Entry e = new Entry(); e.id = campaignId; e.target = target;
        queue.addLast(e);
    }

    @Override public String currentCampaign() {
        Entry e = queue.peekFirst();
        return e == null ? null : e.id;
    }

    @Override public BatchStateSignal nextPhase() {
        Entry e = queue.pollFirst();
        return e == null ? null : e.target;
    }

    @Override public int queuedCampaigns() { return queue.size(); }
}
