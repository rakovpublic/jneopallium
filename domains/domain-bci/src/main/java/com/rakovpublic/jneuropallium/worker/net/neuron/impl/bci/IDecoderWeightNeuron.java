package com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;

public interface IDecoderWeightNeuron extends IModulatableNeuron {
    void init(int dim);
    double[] getWeights();
    void update(double[] rates, double error);
    double predict(double[] rates);
    void setLearningRate(double lr);
    void setWeightDecay(double wd);
}
