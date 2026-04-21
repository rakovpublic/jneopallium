package com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;

public interface ILatentDynamicsNeuron extends IModulatableNeuron {
    double[] step(double[] rates);
    double[] getLatent();
    int getLatentDim();
    void setLatentDim(int d);
    void setLeak(double l);
    void setInputGain(double g);
    void reset();
}
