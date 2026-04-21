package com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;

public interface IPowerBudgetNeuron extends IModulatableNeuron {
    void setCapacityMAh(double c);
    void setRemainingMAh(double r);
    void drain(double mAh);
    double stateOfChargeFrac();
    PowerBudgetNeuron.PowerMode getMode();
    boolean stimAllowed();
    void setThresholds(double conserve, double emergency);
}
