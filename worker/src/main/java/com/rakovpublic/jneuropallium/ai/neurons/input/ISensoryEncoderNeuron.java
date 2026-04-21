package com.rakovpublic.jneuropallium.ai.neurons.input;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;

public interface ISensoryEncoderNeuron extends IModulatableNeuron {
    double getPreferredValue();
    void setPreferredValue(double preferredValue);
    double getSigma();
    void setSigma(double sigma);
}
