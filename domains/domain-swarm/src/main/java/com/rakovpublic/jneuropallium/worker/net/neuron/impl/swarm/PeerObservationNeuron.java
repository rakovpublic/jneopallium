/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.swarm;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm.PeerObservationSignal;

/**
 * Layer 0 peer-detection neuron. Drops observations whose link
 * quality is below {@link #getMinLinkQuality()} so downstream consumers
 * don't waste cycles on noise. Loop=1 / Epoch=2.
 */
public class PeerObservationNeuron extends ModulatableNeuron implements IPeerObservationNeuron {

    private long observationCount;
    private double minLinkQuality;

    public PeerObservationNeuron() { super(); }
    public PeerObservationNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    @Override
    public PeerObservationSignal observe(String peerId, double[] positionLocal,
                                          double[] velocityLocal, double linkQuality) {
        if (peerId == null || linkQuality < minLinkQuality) return null;
        observationCount++;
        return new PeerObservationSignal(peerId, positionLocal, velocityLocal, linkQuality);
    }

    @Override public long getObservationCount() { return observationCount; }
    @Override public void setMinLinkQuality(double q) { this.minLinkQuality = Math.max(0.0, Math.min(1.0, q)); }
    @Override public double getMinLinkQuality() { return minLinkQuality; }
}
