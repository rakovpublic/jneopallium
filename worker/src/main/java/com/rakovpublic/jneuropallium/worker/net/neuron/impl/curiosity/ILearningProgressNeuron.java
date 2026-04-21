package com.rakovpublic.jneuropallium.worker.net.neuron.impl.curiosity;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.curiosity.LearningProgressSignal;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

public interface ILearningProgressNeuron extends IModulatableNeuron {
    LearningProgressSignal recordError(String domain, double error);
    double intrinsicReward(String domain);
    int getWindowTicks();
    void setWindowTicks(int windowTicks);
    int domainCount();
}
