/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.security;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.PacketSignal;

/**
 * Layer 0 packet ingestion. Emits a {@link PacketSignal} per accepted
 * capture up to a configurable per-second rate cap. Biological analogue:
 * pattern-recognition receptors at epithelial surfaces.
 * Loop=1 / Epoch=1.
 */
public class PacketIngestNeuron extends ModulatableNeuron implements IPacketIngestNeuron {

    private long rateLimitPerSec = 1_000_000L;
    private long windowSec = Long.MIN_VALUE;
    private long inWindow;
    private long accepted;
    private long dropped;

    public PacketIngestNeuron() { super(); }
    public PacketIngestNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    @Override
    public PacketSignal ingest(byte[] summary, NetworkTuple tuple, long timestamp) {
        long sec = timestamp / 1_000_000_000L;
        if (sec != windowSec) { windowSec = sec; inWindow = 0; }
        if (inWindow >= rateLimitPerSec) { dropped++; return null; }
        inWindow++;
        accepted++;
        return new PacketSignal(summary, tuple, timestamp);
    }

    @Override public long getDropped() { return dropped; }
    @Override public long getAccepted() { return accepted; }
    @Override public void setRateLimitPerSec(long r) { this.rateLimitPerSec = Math.max(1L, r); }
    @Override public long getRateLimitPerSec() { return rateLimitPerSec; }
}
