package com.rakovpublic.jneuropallium.ai.neurons.memory;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;

public interface IPredictionErrorNeuron extends IModulatableNeuron {
    double getThetaPositive();
    void setThetaPositive(double thetaPositive);
    double getThetaNegative();
    void setThetaNegative(double thetaNegative);
    String getPlanningNeuronId();
    void setPlanningNeuronId(String planningNeuronId);
}
