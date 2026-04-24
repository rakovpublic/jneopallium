/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.security;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.LogEventSignal;

import java.util.Map;

/** Layer 0 log normaliser. Loop=1 / Epoch=2. */
public class LogIngestNeuron extends ModulatableNeuron implements ILogIngestNeuron {

    private long accepted;

    public LogIngestNeuron() { super(); }
    public LogIngestNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    @Override
    public LogEventSignal ingest(String source, LogLevel level, Map<String, String> fields, long timestamp) {
        accepted++;
        return new LogEventSignal(source, level, fields, timestamp);
    }

    @Override public long getAccepted() { return accepted; }
}
