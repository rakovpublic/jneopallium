/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;

/**
 * Layer 3 product-quality model. Compliance probability is derived
 * from the distance between a summary statistic of {@code conditions}
 * and the configured {@code target}. Loop=2 / Epoch=2.
 */
public class ProductQualityModelNeuron extends ModulatableNeuron implements IProductQualityModelNeuron {

    private double target;
    private double tolerance = 1.0;

    public ProductQualityModelNeuron() { super(); }
    public ProductQualityModelNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    @Override public void setTarget(double target) { this.target = target; }
    @Override public void setTolerance(double tol) { this.tolerance = Math.max(1e-6, tol); }

    @Override
    public double predictCompliance(double[] conditions) {
        if (conditions == null || conditions.length == 0) return 0.0;
        double mean = 0.0;
        for (double v : conditions) mean += v;
        mean /= conditions.length;
        double dev = Math.abs(mean - target) / tolerance;
        return Math.max(0.0, Math.min(1.0, 1.0 - dev));
    }

    @Override public boolean inSpec(double[] conditions) { return predictCompliance(conditions) >= 0.5; }
}
