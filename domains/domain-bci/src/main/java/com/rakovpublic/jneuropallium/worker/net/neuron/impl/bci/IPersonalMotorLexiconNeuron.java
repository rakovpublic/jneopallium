package com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;

public interface IPersonalMotorLexiconNeuron extends IModulatableNeuron {
    void register(String name, double[] template);
    boolean forget(String name);
    int size();
    String match(double[] query);
    void setMatchThreshold(double t);
}
