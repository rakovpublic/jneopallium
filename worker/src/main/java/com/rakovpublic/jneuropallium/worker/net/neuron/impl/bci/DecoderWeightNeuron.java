/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;

/**
 * Layer 3 decoder-weight adaptation. Performs online Hebbian-style updates
 * on the decoder weight vector given an intent/outcome error signal. Biological
 * analogue: long-horizon cortical remapping during closed-loop decoder adaptation
 * (Orsborn et al. 2014).
 * Loop=2 / Epoch=3.
 */
public class DecoderWeightNeuron extends ModulatableNeuron {

    private double[] weights;
    private double learningRate = 0.01;
    private double weightDecay = 1e-4;

    public DecoderWeightNeuron() { super(); this.weights = new double[0]; }
    public DecoderWeightNeuron(Long neuronId, ISignalChain chain, Long run) {
        super(neuronId, chain, run);
        this.weights = new double[0];
    }

    public void init(int dim) { this.weights = new double[Math.max(0, dim)]; }
    public double[] getWeights() { return weights.clone(); }

    /**
     * Update weights with presynaptic {@code rates} and scalar error. Rule:
     * w_i += lr * (error * rate_i) - decay * w_i.
     */
    public void update(double[] rates, double error) {
        if (rates == null) return;
        if (weights.length != rates.length) weights = new double[rates.length];
        for (int i = 0; i < weights.length; i++) {
            weights[i] += learningRate * error * rates[i] - weightDecay * weights[i];
        }
    }

    public double predict(double[] rates) {
        if (rates == null) return 0;
        int n = Math.min(rates.length, weights.length);
        double s = 0;
        for (int i = 0; i < n; i++) s += weights[i] * rates[i];
        return s;
    }

    public void setLearningRate(double lr) { this.learningRate = Math.max(0, lr); }
    public void setWeightDecay(double wd) { this.weightDecay = Math.max(0, wd); }
}
