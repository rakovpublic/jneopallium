package com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;

import java.util.Map;

public interface IPopulationVectorNeuron extends IModulatableNeuron {
    void tunePreferredDirection(int unitId, double[] direction);
    int tunedUnitCount();
    double[] decode(Map<Integer, Double> rates);
}
