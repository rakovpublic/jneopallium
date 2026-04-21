/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;

/**
 * Layer 7 power budget manager. Tracks implant battery state of charge and
 * gates features (stim, high-rate decoding) as the budget drops.
 * Loop=2 / Epoch=1.
 */
public class PowerBudgetNeuron extends ModulatableNeuron implements IPowerBudgetNeuron {

    public enum PowerMode { NORMAL, CONSERVE, EMERGENCY }

    private double capacityMAh = 500.0;
    private double remainingMAh = 500.0;
    private PowerMode mode = PowerMode.NORMAL;
    private double conserveFrac = 0.30;
    private double emergencyFrac = 0.10;

    public PowerBudgetNeuron() { super(); }
    public PowerBudgetNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    public void setCapacityMAh(double c) { this.capacityMAh = Math.max(1e-6, c); }
    public void setRemainingMAh(double r) { this.remainingMAh = Math.max(0, Math.min(capacityMAh, r)); classify(); }

    public void drain(double mAh) {
        remainingMAh = Math.max(0, remainingMAh - mAh);
        classify();
    }

    private void classify() {
        double frac = remainingMAh / capacityMAh;
        if (frac <= emergencyFrac) mode = PowerMode.EMERGENCY;
        else if (frac <= conserveFrac) mode = PowerMode.CONSERVE;
        else mode = PowerMode.NORMAL;
    }

    public double stateOfChargeFrac() { return remainingMAh / capacityMAh; }
    public PowerMode getMode() { return mode; }
    public boolean stimAllowed() { return mode != PowerMode.EMERGENCY; }
    public void setThresholds(double conserve, double emergency) {
        this.conserveFrac = conserve;
        this.emergencyFrac = emergency;
    }
}
