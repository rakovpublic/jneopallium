package com.rakovpublic.jneuropallium.worker.net.neuron.impl.tutoring;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.tutoring.HintSignal;

public interface IHintGenerationNeuron extends IModulatableNeuron {
    HintSignal nextHint(String itemId, String conceptId);
    int hintsIssuedFor(String itemId);
    void reset(String itemId);
    void setMaxLevels(int n);
    int getMaxLevels();
}
