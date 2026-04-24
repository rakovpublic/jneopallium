/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.security;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;

/**
 * Layer 7 energy budget. Prevents runaway rule evaluation during a
 * DDoS by maintaining a token-bucket-style energy pool. Loop=2 / Epoch=1.
 */
public class ImmuneExhaustionNeuron extends ModulatableNeuron implements IImmuneExhaustionNeuron {

    private double energy = 1.0;
    private double budgetPerTick = 0.0005; // cost per evaluation
    private double recoveryRate = 0.001;
    private long lastTick = Long.MIN_VALUE;

    public ImmuneExhaustionNeuron() { super(); }
    public ImmuneExhaustionNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    @Override
    public void consume(long tick) {
        if (lastTick != Long.MIN_VALUE && tick > lastTick) {
            energy = Math.min(1.0, energy + recoveryRate * (tick - lastTick));
        }
        energy -= budgetPerTick;
        if (energy < 0) energy = 0;
        lastTick = tick;
    }

    @Override public boolean isExhausted() { return energy <= 0.05; }
    @Override public double energyLevel() { return energy; }
    @Override public void setBudgetPerTick(double budget) { this.budgetPerTick = Math.max(0.0, budget); }
    @Override public double getBudgetPerTick() { return budgetPerTick; }
    @Override public void setRecoveryRate(double r) { this.recoveryRate = Math.max(0.0, r); }
    @Override public double getRecoveryRate() { return recoveryRate; }
}
