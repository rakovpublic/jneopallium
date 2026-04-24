package com.rakovpublic.jneuropallium.worker.net.neuron.impl.security;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.AnomalyScoreSignal;

public interface ILateralMovementNeuron extends IModulatableNeuron {
    /** Record an authentication from {@code user} to {@code host} at {@code tick}. */
    AnomalyScoreSignal recordAuth(String user, String host, long tick);
    int hostsFor(String user);
    void setFanoutThreshold(int t);
    int getFanoutThreshold();
}
