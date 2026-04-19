/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;

/**
 * Layer 1 latent-dynamics decoder (LFADS-style, Pandarinath et al. 2018).
 * Projects a high-dimensional rate vector onto a low-dimensional manifold
 * and evolves a linear dynamical system. This implementation is a scalar
 * stand-in; production deployments plug in a trained sequential VAE.
 * Loop=1 / Epoch=2.
 */
public class LatentDynamicsNeuron extends ModulatableNeuron {

    private double[] latent;
    private double leak = 0.9;
    private double inputGain = 0.1;

    public LatentDynamicsNeuron() { super(); this.latent = new double[4]; }
    public LatentDynamicsNeuron(Long neuronId, ISignalChain chain, Long run) {
        super(neuronId, chain, run);
        this.latent = new double[4];
    }

    /**
     * Advance the latent state by one step given the current rate vector.
     * Input rates are projected via a fixed random projection (identity-like
     * for the stand-in) and mixed into the latent via a leaky integrator.
     */
    public double[] step(double[] rates) {
        if (rates == null) return latent.clone();
        for (int i = 0; i < latent.length; i++) {
            double drive = 0;
            for (int j = i; j < rates.length; j += latent.length) drive += rates[j];
            latent[i] = leak * latent[i] + inputGain * drive;
        }
        return latent.clone();
    }

    public double[] getLatent() { return latent.clone(); }
    public int getLatentDim() { return latent.length; }
    public void setLatentDim(int d) { this.latent = new double[Math.max(1, d)]; }
    public void setLeak(double l) { this.leak = Math.max(0, Math.min(1, l)); }
    public void setInputGain(double g) { this.inputGain = g; }
    public void reset() { for (int i = 0; i < latent.length; i++) latent[i] = 0; }
}
