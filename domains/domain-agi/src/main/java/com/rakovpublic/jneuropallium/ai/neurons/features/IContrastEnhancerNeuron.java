package com.rakovpublic.jneuropallium.ai.neurons.features;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;

public interface IContrastEnhancerNeuron extends IModulatableNeuron {
    double getExcitatory();
    void setExcitatory(double excitatory);
    double getInhibitory();
    void setInhibitory(double inhibitory);
}
