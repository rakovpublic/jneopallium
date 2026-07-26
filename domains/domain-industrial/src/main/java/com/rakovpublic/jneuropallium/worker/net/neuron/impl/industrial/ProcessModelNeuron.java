/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;

import java.util.HashMap;
import java.util.Map;

/**
 * Layer 3 first-order-plus-dead-time process model. {@code predict}
 * returns the value {@code horizonTicks} ahead under a sustained
 * {@code controlInput}. Loop=2 / Epoch=1.
 */
public class ProcessModelNeuron extends ModulatableNeuron implements IProcessModelNeuron {

    private static final class Params { double k = 1.0; int deadTicks; double tauTicks = 1.0; }

    private final Map<String, Params> byTag = new HashMap<>();

    public ProcessModelNeuron() { super(); }
    public ProcessModelNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    private Params p(String tag) { return byTag.computeIfAbsent(tag, k -> new Params()); }

    @Override public void setGain(String tag, double k) { if (tag != null) p(tag).k = k; }
    @Override public void setDeadTimeTicks(String tag, int dtTicks) { if (tag != null) p(tag).deadTicks = Math.max(0, dtTicks); }
    @Override public void setTimeConstantTicks(String tag, double tauTicks) { if (tag != null) p(tag).tauTicks = Math.max(1e-6, tauTicks); }

    @Override
    public double predict(String tag, double controlInput, int horizonTicks) {
        Params pp = byTag.get(tag);
        if (pp == null) return controlInput;
        int after = Math.max(0, horizonTicks - pp.deadTicks);
        double response = 1.0 - Math.exp(-after / pp.tauTicks);
        return pp.k * controlInput * response;
    }
}
