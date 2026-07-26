package com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;

public interface IProductQualityModelNeuron extends IModulatableNeuron {
    void setTarget(double target);
    void setTolerance(double tol);
    /** Predicted compliance probability in [0,1] for the current conditions. */
    double predictCompliance(double[] conditions);
    boolean inSpec(double[] conditions);
}
