package com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import java.util.HashSet;
import java.util.Set;

public interface IArtefactRejectionNeuron extends IModulatableNeuron {
    boolean check(int channelId, double[] window);
    boolean isMasked(int channelId);
    void unmask(int channelId);
    void setAbsAmplitudeLimitUV(double v);
    int maskedCount();
}
