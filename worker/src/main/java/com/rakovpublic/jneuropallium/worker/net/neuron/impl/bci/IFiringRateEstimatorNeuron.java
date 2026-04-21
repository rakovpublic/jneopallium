package com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;

public interface IFiringRateEstimatorNeuron extends IModulatableNeuron {
    double onSpike(int unitId, long tsNs);
    double rateFor(int unitId);
    int trackedUnits();
    void setTauSeconds(double tau);
}
