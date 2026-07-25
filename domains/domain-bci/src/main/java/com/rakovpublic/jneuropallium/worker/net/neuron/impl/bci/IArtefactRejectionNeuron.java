package com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;

public interface IArtefactRejectionNeuron extends IModulatableNeuron {
    boolean check(int channelId, double[] window);
    boolean isMasked(int channelId);
    void unmask(int channelId);
    void setAbsAmplitudeLimitUV(double v);
    int maskedCount();
}
