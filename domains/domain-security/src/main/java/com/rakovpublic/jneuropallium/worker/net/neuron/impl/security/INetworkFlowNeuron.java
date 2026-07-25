package com.rakovpublic.jneuropallium.worker.net.neuron.impl.security;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.PacketSignal;

public interface INetworkFlowNeuron extends IModulatableNeuron {
    /** Incorporate a packet into the per-flow aggregate. Returns true if this opened a new flow. */
    boolean accumulate(PacketSignal p);
    int openFlows();
    long bytesFor(NetworkTuple t);
    long packetsFor(NetworkTuple t);
    void reset();
}
