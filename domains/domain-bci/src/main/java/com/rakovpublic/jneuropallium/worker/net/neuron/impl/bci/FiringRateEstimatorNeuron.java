/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;

import java.util.HashMap;
import java.util.Map;

/**
 * Layer 1 per-unit rolling firing-rate estimator. Exponential moving average
 * over spike counts; rate reported in spikes / second.
 * Loop=1 / Epoch=1.
 */
public class FiringRateEstimatorNeuron extends ModulatableNeuron implements IFiringRateEstimatorNeuron {

    private final Map<Integer, Double> rates = new HashMap<>();
    private double tauSeconds = 0.1;
    private long lastTsNs;

    public FiringRateEstimatorNeuron() { super(); }
    public FiringRateEstimatorNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    /**
     * Update rate for {@code unitId} on a new spike at timestamp {@code tsNs}.
     */
    public double onSpike(int unitId, long tsNs) {
        double dt = lastTsNs == 0 ? 0 : (tsNs - lastTsNs) / 1_000_000_000.0;
        lastTsNs = tsNs;
        double alpha = dt <= 0 ? 1.0 : 1.0 - Math.exp(-dt / tauSeconds);
        double impulse = dt <= 0 ? 1.0 : 1.0 / dt;
        double prev = rates.getOrDefault(unitId, 0.0);
        double updated = (1 - alpha) * prev + alpha * impulse;
        rates.put(unitId, updated);
        return updated;
    }

    public double rateFor(int unitId) { return rates.getOrDefault(unitId, 0.0); }
    public int trackedUnits() { return rates.size(); }
    public void setTauSeconds(double tau) { this.tauSeconds = Math.max(1e-3, tau); }
}
