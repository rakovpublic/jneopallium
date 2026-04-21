package com.rakovpublic.jneuropallium.worker.net.neuron.impl.tutoring;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.tutoring.HintSignal;
import java.util.HashMap;
import java.util.Map;

public interface IHintGenerationNeuron extends IModulatableNeuron {
    HintSignal nextHint(String itemId, String conceptId);
    int hintsIssuedFor(String itemId);
    void reset(String itemId);
    void setMaxLevels(int n);
    int getMaxLevels();
}
