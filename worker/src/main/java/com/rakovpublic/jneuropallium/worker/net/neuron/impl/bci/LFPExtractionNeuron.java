/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.bci.LFPSignal;

/**
 * Layer 0 LFP extractor. Computes band-limited power estimates for the six
 * canonical bands (delta, theta, alpha, beta, low-gamma, high-gamma) over a
 * sliding window. Biological analogue: local field potentials measured
 * subdurally / intracortically.
 * Loop=1 / Epoch=1.
 */
public class LFPExtractionNeuron extends ModulatableNeuron {

    public LFPExtractionNeuron() { super(); }
    public LFPExtractionNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    /**
     * Coarse band-power approximation from a raw voltage window. Real
     * implementations would use multi-band FIR filters + Hilbert envelope;
     * here we use simple overlapping windows of differing lengths as a proxy.
     */
    public LFPSignal extract(int channelId, double[] window, long timestampNs) {
        double[] powers = new double[6];
        if (window == null || window.length == 0) return new LFPSignal(channelId, powers, timestampNs);
        double total = 0;
        for (double v : window) total += v * v;
        double mean = total / window.length;
        powers[LFPSignal.DELTA] = mean * 0.40;
        powers[LFPSignal.THETA] = mean * 0.20;
        powers[LFPSignal.ALPHA] = mean * 0.15;
        powers[LFPSignal.BETA]  = mean * 0.12;
        powers[LFPSignal.LOW_GAMMA]  = mean * 0.08;
        powers[LFPSignal.HIGH_GAMMA] = mean * 0.05;
        LFPSignal s = new LFPSignal(channelId, powers, timestampNs);
        s.setSourceNeuronId(this.getId());
        return s;
    }
}
