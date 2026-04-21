package com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;

public interface IUserStateNeuron extends IModulatableNeuron {
    State classify(double fatigue, double confusion, double distress);
    State getState();
    void setFatigueThreshold(double v);
    void setConfusionThreshold(double v);
    void setDistressThreshold(double v);
}
