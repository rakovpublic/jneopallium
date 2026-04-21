package com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;

public interface ILatentDynamicsNeuron extends IModulatableNeuron {
    double[] step(double[] rates);
    double[] getLatent();
    int getLatentDim();
    void setLatentDim(int d);
    void setLeak(double l);
    void setInputGain(double g);
    void reset();
}
