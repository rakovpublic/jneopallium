package com.rakovpublic.jneuropallium.worker.net.neuron.impl.swarm;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm.PeerStateSignal;

public interface IEnergyCoordinatorNeuron extends IModulatableNeuron {
    void setOwnBattery(double f);
    double getOwnBattery();
    void onPeerState(PeerStateSignal s);
    /**
     * Decision helper: should this agent defer the named task to a
     * higher-battery peer? Returns the suggested peerId, or null if
     * this agent should keep the task.
     */
    String shouldDeferTo(String taskId);
}
