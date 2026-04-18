/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.affect;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.affect.InteroceptiveSignal;

/**
 * Anterior-insula-like integrator for body-state streams.
 * Layer 2, loop=1 / epoch=2.
 * <p>Biological analogue: anterior insular cortex integrates
 * interoceptive signals into a unified body-state percept (Craig 2009).
 */
public class AnteriorInsulaNeuron extends ModulatableNeuron implements IInteroceptive {

    private double homeostaticError;
    private double energyBudget;
    private double averagedPain;
    private int sampleCount;

    public AnteriorInsulaNeuron() {
        super();
        this.energyBudget = 1.0;
    }

    public AnteriorInsulaNeuron(Long neuronId, ISignalChain chain, Long run) {
        super(neuronId, chain, run);
        this.energyBudget = 1.0;
    }

    /**
     * Integrate a new interoceptive sample with exponential moving average.
     *
     * @param s interoceptive signal
     */
    public void integrate(InteroceptiveSignal s) {
        if (s == null) return;
        double alpha = 0.2;
        this.homeostaticError = (1 - alpha) * this.homeostaticError + alpha * s.getHomeostaticError();
        this.energyBudget = (1 - alpha) * this.energyBudget + alpha * s.getEnergyBudget();
        this.averagedPain = (1 - alpha) * this.averagedPain + alpha * s.getPainMagnitude();
        sampleCount++;
    }

    @Override
    public double readHomeostaticError() { return homeostaticError; }

    @Override
    public double readEnergyBudget() { return energyBudget; }

    public double getAveragedPain() { return averagedPain; }
    public int getSampleCount() { return sampleCount; }
}
