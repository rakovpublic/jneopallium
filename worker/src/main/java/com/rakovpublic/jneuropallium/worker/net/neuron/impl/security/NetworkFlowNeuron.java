/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.security;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.PacketSignal;

import java.util.HashMap;
import java.util.Map;

/** Layer 1 flow aggregator: bytes + packets per 5-tuple. Loop=1 / Epoch=2. */
public class NetworkFlowNeuron extends ModulatableNeuron implements INetworkFlowNeuron {

    private static final class Agg {
        long bytes;
        long packets;
    }

    private final Map<NetworkTuple, Agg> flows = new HashMap<>();

    public NetworkFlowNeuron() { super(); }
    public NetworkFlowNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    @Override
    public boolean accumulate(PacketSignal p) {
        if (p == null || p.getTuple() == null) return false;
        Agg existing = flows.get(p.getTuple());
        boolean fresh = existing == null;
        if (fresh) { existing = new Agg(); flows.put(p.getTuple(), existing); }
        existing.packets++;
        existing.bytes += p.getSummary() == null ? 0 : p.getSummary().length;
        return fresh;
    }

    @Override public int openFlows() { return flows.size(); }

    @Override
    public long bytesFor(NetworkTuple t) {
        Agg a = flows.get(t);
        return a == null ? 0 : a.bytes;
    }

    @Override
    public long packetsFor(NetworkTuple t) {
        Agg a = flows.get(t);
        return a == null ? 0 : a.packets;
    }

    @Override public void reset() { flows.clear(); }
}
