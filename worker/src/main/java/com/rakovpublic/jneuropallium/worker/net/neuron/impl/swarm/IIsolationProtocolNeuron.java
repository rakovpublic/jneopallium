package com.rakovpublic.jneuropallium.worker.net.neuron.impl.swarm;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm.AgentAnomalySignal;

public interface IIsolationProtocolNeuron extends IModulatableNeuron {
    /** k-witness corroboration threshold; minimum 3 for f=1 Byzantine tolerance on a small swarm. */
    void setWitnessThreshold(int k);
    int getWitnessThreshold();
    void setIsolationTicks(long t);
    long getIsolationTicks();
    /** Receive an anomaly report; returns true iff isolation was applied (i.e. threshold met). */
    boolean onReport(AgentAnomalySignal report, long currentTick);
    boolean isIsolated(String peerId, long currentTick);
    /** Reap expired isolations and return the number released. */
    int tick(long currentTick);
    int activeIsolations(long currentTick);
}
