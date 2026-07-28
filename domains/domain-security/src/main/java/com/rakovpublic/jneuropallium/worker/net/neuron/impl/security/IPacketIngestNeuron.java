package com.rakovpublic.jneuropallium.worker.net.neuron.impl.security;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.PacketSignal;

public interface IPacketIngestNeuron extends IModulatableNeuron {
    PacketSignal ingest(byte[] summary, NetworkTuple tuple, long timestamp);
    long getDropped();
    long getAccepted();
    void setRateLimitPerSec(long r);
    long getRateLimitPerSec();
}
