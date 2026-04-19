/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.tutoring;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.tutoring.ResponseSignal;

/**
 * Layer 0 sensing neuron. Converts a {@link ResponseSignal} into a binary
 * correctness spike and a continuous error delta.
 * Loop=1 / Epoch=1.
 */
public class ResponseObserverNeuron extends ModulatableNeuron {

    private int totalResponses;
    private int totalCorrect;
    private double lastError;
    private long lastLatencyMs;

    public ResponseObserverNeuron() { super(); }
    public ResponseObserverNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    /**
     * Consume a response observation.
     *
     * @param s response signal with correctness and latency
     * @return error delta in [-1,1]: +1 when correct, -1 when incorrect
     */
    public double observe(ResponseSignal s) {
        if (s == null) return 0.0;
        totalResponses++;
        if (s.isCorrect()) totalCorrect++;
        lastLatencyMs = s.getLatencyMs();
        lastError = s.isCorrect() ? 1.0 : -1.0;
        return lastError;
    }

    public int getTotalResponses() { return totalResponses; }
    public int getTotalCorrect() { return totalCorrect; }
    public double getAccuracy() { return totalResponses == 0 ? 0.0 : ((double) totalCorrect) / totalResponses; }
    public double getLastError() { return lastError; }
    public long getLastLatencyMs() { return lastLatencyMs; }
}
