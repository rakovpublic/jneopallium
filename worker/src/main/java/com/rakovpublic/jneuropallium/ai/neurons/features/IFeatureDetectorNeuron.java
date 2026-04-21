package com.rakovpublic.jneuropallium.ai.neurons.features;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;

public interface IFeatureDetectorNeuron extends IModulatableNeuron {
    double[] getWeightedTemplate();
    void setWeightedTemplate(double[] weightedTemplate);
    double getThreshold();
    void setThreshold(double detectionThreshold);
}
