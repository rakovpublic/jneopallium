/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.security;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.SyscallSignal;

/** Layer 0 syscall ingestion (eBPF/ETW). Loop=1 / Epoch=1. */
public class SyscallIngestNeuron extends ModulatableNeuron implements ISyscallIngestNeuron {

    private long accepted;

    public SyscallIngestNeuron() { super(); }
    public SyscallIngestNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    @Override
    public SyscallSignal ingest(int syscallNum, int pid, String procName, long[] args) {
        accepted++;
        return new SyscallSignal(syscallNum, pid, procName, args);
    }

    @Override public long getAccepted() { return accepted; }
}
