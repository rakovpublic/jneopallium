package com.rakovpublic.jneuropallium.worker.net.neuron.impl.security;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.SyscallSignal;

public interface ISyscallIngestNeuron extends IModulatableNeuron {
    SyscallSignal ingest(int syscallNum, int pid, String procName, long[] args);
    long getAccepted();
}
