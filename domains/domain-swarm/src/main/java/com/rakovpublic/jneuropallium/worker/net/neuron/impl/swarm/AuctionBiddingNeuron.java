/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.swarm;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm.TaskAnnouncementSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm.TaskBidSignal;

/**
 * Layer 4 auction bidder. Bid = euclidean distance to task / battery
 * fraction. Confidence falls with battery. Loop=2 / Epoch=1.
 */
public class AuctionBiddingNeuron extends ModulatableNeuron implements IAuctionBiddingNeuron {

    private String bidderId;
    private double[] selfPosition;
    private double batteryFraction = 1.0;

    public AuctionBiddingNeuron() { super(); }
    public AuctionBiddingNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    @Override public void setBidderId(String id) { this.bidderId = id; }
    @Override public String getBidderId() { return bidderId; }
    @Override public void setSelfPosition(double[] pos) { this.selfPosition = pos == null ? null : pos.clone(); }
    @Override public void setBatteryFraction(double f) { this.batteryFraction = Math.max(0.0, Math.min(1.0, f)); }
    @Override public double getBatteryFraction() { return batteryFraction; }

    @Override
    public TaskBidSignal bid(TaskAnnouncementSignal a) {
        if (a == null || a.getTaskId() == null) return null;
        if (batteryFraction <= 0.0) return null;
        double[] target = a.getLocationGlobal();
        double dist = 0.0;
        if (target != null && selfPosition != null) {
            int n = Math.min(target.length, selfPosition.length);
            for (int i = 0; i < n; i++) { double d = target[i] - selfPosition[i]; dist += d * d; }
            dist = Math.sqrt(dist);
        }
        double cost = dist / Math.max(1e-3, batteryFraction);
        double confidence = batteryFraction;
        return new TaskBidSignal(a.getTaskId(), bidderId, cost, confidence);
    }
}
