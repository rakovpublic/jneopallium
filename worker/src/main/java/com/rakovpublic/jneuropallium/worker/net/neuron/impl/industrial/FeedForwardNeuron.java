/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.ActuatorCommandSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MeasurementSignal;

/**
 * Layer 1 feed-forward compensator. Adds a bias proportional to a
 * disturbance measurement onto the downstream actuator. Loop=1 / Epoch=1.
 */
public class FeedForwardNeuron extends ModulatableNeuron implements IFeedForwardNeuron {

    private double gain;

    public FeedForwardNeuron() { super(); }
    public FeedForwardNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    @Override public void setGain(double k) { this.gain = k; }
    @Override public double getGain() { return gain; }

    @Override
    public ActuatorCommandSignal compensate(MeasurementSignal disturbance, String targetTag, double currentValue) {
        if (disturbance == null) return null;
        double target = currentValue + gain * disturbance.getMeasurement();
        return new ActuatorCommandSignal(targetTag, target, currentValue, true);
    }
}
