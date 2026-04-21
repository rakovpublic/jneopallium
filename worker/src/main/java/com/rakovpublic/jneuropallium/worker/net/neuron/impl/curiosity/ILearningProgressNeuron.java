package com.rakovpublic.jneuropallium.worker.net.neuron.impl.curiosity;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.curiosity.LearningProgressSignal;

public interface ILearningProgressNeuron extends IModulatableNeuron {
    LearningProgressSignal recordError(String domain, double error);
    double intrinsicReward(String domain);
    int getWindowTicks();
    void setWindowTicks(int windowTicks);
    int domainCount();
}
