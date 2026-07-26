package com.rakovpublic.jneuropallium.worker.net.neuron.impl.tutoring;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;

import java.util.Set;

public interface IFairnessNeuron extends IModulatableNeuron {
    void addAccommodation(String flag);
    void removeAccommodation(String flag);
    boolean hasAccommodation(String flag);
    Set<String> accommodations();
    double adjustScoreForLatency(double rawScore, long latencyMs, long expectedMs);
    boolean isResponseTimePenaltyEnabled();
    boolean wouldModifyEthicalPriority();
}
