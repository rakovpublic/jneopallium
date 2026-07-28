package com.rakovpublic.jneuropallium.worker.net.neuron.impl.swarm;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm.TaskAnnouncementSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm.TaskBidSignal;

public interface IAuctionBiddingNeuron extends IModulatableNeuron {
    void setBidderId(String id);
    String getBidderId();
    void setSelfPosition(double[] pos);
    /** Compute a bid for this announcement; returns null when disinterested. */
    TaskBidSignal bid(TaskAnnouncementSignal announcement);
    void setBatteryFraction(double f);
    double getBatteryFraction();
}
