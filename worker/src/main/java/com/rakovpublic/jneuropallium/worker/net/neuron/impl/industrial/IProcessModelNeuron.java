package com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;

public interface IProcessModelNeuron extends IModulatableNeuron {
    /** Forward-predict the response of {@code tag} to {@code controlInput} after {@code horizonTicks}. */
    double predict(String tag, double controlInput, int horizonTicks);
    void setGain(String tag, double k);
    void setDeadTimeTicks(String tag, int dtTicks);
    void setTimeConstantTicks(String tag, double tauTicks);
}
