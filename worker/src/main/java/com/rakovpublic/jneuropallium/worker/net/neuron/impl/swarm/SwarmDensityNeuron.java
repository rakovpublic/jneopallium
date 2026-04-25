/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.swarm;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;

/** Layer 7 density homeostasis. Loop=2 / Epoch=2. */
public class SwarmDensityNeuron extends ModulatableNeuron implements ISwarmDensityNeuron {

    private int neighbours;
    private int minNeighbours = 2;
    private int maxNeighbours = 8;

    public SwarmDensityNeuron() { super(); }
    public SwarmDensityNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    @Override public void observeNeighbour(double[] relativePosition) { neighbours++; }
    @Override public int neighbourCount() { return neighbours; }

    @Override
    public double densityBias() {
        if (neighbours > maxNeighbours) return (neighbours - maxNeighbours) / (double) Math.max(1, maxNeighbours);
        if (neighbours < minNeighbours) return -(minNeighbours - neighbours) / (double) Math.max(1, minNeighbours);
        return 0.0;
    }

    @Override public void setMinNeighbours(int n) { this.minNeighbours = Math.max(0, n); }
    @Override public void setMaxNeighbours(int n) { this.maxNeighbours = Math.max(this.minNeighbours, n); }
    @Override public void clear() { neighbours = 0; }
}
