package com.rakovpublic.jneuropallium.worker.net.neuron.impl.tutoring;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;

public interface IFlowStateNeuron extends IModulatableNeuron {
    FlowStateKind classify(double engagement, double valence, double arousal, double recentAccuracy);
    FlowStateKind getCurrentState();
    double getLastEngagement();
    double getLastValence();
    double getLastArousal();
    double getLastAccuracy();
}
