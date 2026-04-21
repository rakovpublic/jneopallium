package com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import java.util.HashMap;
import java.util.Map;

public interface IPersonalMotorLexiconNeuron extends IModulatableNeuron {
    void register(String name, double[] template);
    boolean forget(String name);
    int size();
    String match(double[] query);
    void setMatchThreshold(double t);
}
