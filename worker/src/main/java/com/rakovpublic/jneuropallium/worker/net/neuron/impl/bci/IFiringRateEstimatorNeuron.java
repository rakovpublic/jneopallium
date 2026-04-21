package com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import java.util.HashMap;
import java.util.Map;

public interface IFiringRateEstimatorNeuron extends IModulatableNeuron {
    double onSpike(int unitId, long tsNs);
    double rateFor(int unitId);
    int trackedUnits();
    void setTauSeconds(double tau);
}
