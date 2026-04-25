package com.rakovpublic.jneuropallium.worker.net.neuron.impl.swarm;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;

public interface ISwarmDensityNeuron extends IModulatableNeuron {
    void observeNeighbour(double[] relativePosition);
    int neighbourCount();
    /** Returns &gt;0 if too dense (disperse), &lt;0 if too sparse (aggregate), 0 if balanced. */
    double densityBias();
    void setMinNeighbours(int n);
    void setMaxNeighbours(int n);
    void clear();
}
