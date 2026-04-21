package com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;

public interface IDecoderWeightNeuron extends IModulatableNeuron {
    void init(int dim);
    double[] getWeights();
    void update(double[] rates, double error);
    double predict(double[] rates);
    void setLearningRate(double lr);
    void setWeightDecay(double wd);
}
