/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.bci.IntentSignal;

/**
 * Layer 2 intent fusion. Combines spike-based decoder output (fast, noisy)
 * with LFP-based decoder output (slower, smoother) using a confidence-weighted
 * average. Biological analogue: premotor / PPC integration of multiple
 * cortical streams (Andersen & Buneo 2002).
 * Loop=1 / Epoch=2.
 */
public class IntentFusionNeuron extends ModulatableNeuron {

    private double spikeWeight = 0.65;
    private double lfpWeight = 0.35;

    public IntentFusionNeuron() { super(); }
    public IntentFusionNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    /**
     * Fuse two candidate intents. If kinds disagree, the higher-confidence
     * stream wins; parameters are blended proportionally.
     */
    public IntentSignal fuse(IntentSignal spikeIntent, IntentSignal lfpIntent) {
        if (spikeIntent == null && lfpIntent == null) return null;
        if (spikeIntent == null) return lfpIntent;
        if (lfpIntent == null) return spikeIntent;
        double ws = spikeWeight * spikeIntent.getConfidence();
        double wl = lfpWeight * lfpIntent.getConfidence();
        double total = ws + wl;
        if (total <= 0) return spikeIntent;
        ws /= total;
        wl /= total;
        IntentKind kind = spikeIntent.getKind();
        if (spikeIntent.getKind() != lfpIntent.getKind())
            kind = ws >= wl ? spikeIntent.getKind() : lfpIntent.getKind();
        double[] sp = spikeIntent.getParameters() == null ? new double[0] : spikeIntent.getParameters();
        double[] lp = lfpIntent.getParameters() == null ? new double[0] : lfpIntent.getParameters();
        int n = Math.max(sp.length, lp.length);
        double[] fused = new double[n];
        for (int i = 0; i < n; i++) {
            double sv = i < sp.length ? sp[i] : 0;
            double lv = i < lp.length ? lp[i] : 0;
            fused[i] = ws * sv + wl * lv;
        }
        double conf = ws * spikeIntent.getConfidence() + wl * lfpIntent.getConfidence();
        return new IntentSignal(kind, fused, conf);
    }

    public void setSpikeWeight(double w) { this.spikeWeight = Math.max(0, w); }
    public void setLfpWeight(double w) { this.lfpWeight = Math.max(0, w); }
    public double getSpikeWeight() { return spikeWeight; }
    public double getLfpWeight() { return lfpWeight; }
}
