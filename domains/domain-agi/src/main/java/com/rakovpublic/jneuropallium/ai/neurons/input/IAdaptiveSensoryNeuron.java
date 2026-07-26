package com.rakovpublic.jneuropallium.ai.neurons.input;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;

public interface IAdaptiveSensoryNeuron extends IModulatableNeuron {
    double getThreshold();
    void setThreshold(double threshold);
    double getDecayRate();
    void setDecayRate(double decayRate);
    double getAdaptationRate();
    void setAdaptationRate(double adaptationRate);
}
