package com.rakovpublic.jneuropallium.worker.net.neuron.impl.security;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;

public interface IImmuneExhaustionNeuron extends IModulatableNeuron {
    /** Record that one evaluation happened at {@code tick}. */
    void consume(long tick);
    /** Is the system currently over-budget and therefore must cap rule evaluation? */
    boolean isExhausted();
    double energyLevel();
    void setBudgetPerTick(double budget);
    double getBudgetPerTick();
    void setRecoveryRate(double r);
    double getRecoveryRate();
}
