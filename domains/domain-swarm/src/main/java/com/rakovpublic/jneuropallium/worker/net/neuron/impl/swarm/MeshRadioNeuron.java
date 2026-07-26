/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.swarm;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/**
 * Layer 0 mesh radio. Drops outbound signals when link quality falls
 * below the configured threshold so downstream consumers see realistic
 * loss. Loop=1 / Epoch=1.
 */
public class MeshRadioNeuron extends ModulatableNeuron implements IMeshRadioNeuron {

    private double lossThreshold = 0.2;
    private long sent;
    private long dropped;

    public MeshRadioNeuron() { super(); }
    public MeshRadioNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    @Override
    public boolean send(ISignal s, double linkQuality) {
        if (s == null) return false;
        if (linkQuality < lossThreshold) { dropped++; return false; }
        sent++;
        return true;
    }

    @Override public long getSent() { return sent; }
    @Override public long getDropped() { return dropped; }
    @Override public void setLossThreshold(double t) { this.lossThreshold = Math.max(0.0, Math.min(1.0, t)); }
    @Override public double getLossThreshold() { return lossThreshold; }
}
