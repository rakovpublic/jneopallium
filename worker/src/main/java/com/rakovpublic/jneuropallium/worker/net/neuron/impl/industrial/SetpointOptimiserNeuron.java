/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.EfficiencySignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.SetpointSignal;

import java.util.HashMap;
import java.util.Map;

/**
 * Layer 2 setpoint optimiser. Nudges a setpoint up / down in response
 * to an efficiency signal deviation from baseline, clamped by
 * configurable range constraints. Loop=1 / Epoch=3.
 */
public class SetpointOptimiserNeuron extends ModulatableNeuron implements ISetpointOptimiserNeuron {

    private static final class Range { double min, max; Range(double l,double h){min=l;max=h;} }
    private final Map<String, Range> constraints = new HashMap<>();
    private double step = 0.01;

    public SetpointOptimiserNeuron() { super(); }
    public SetpointOptimiserNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    @Override public void setConstraint(String tag, double min, double max) {
        if (tag != null) constraints.put(tag, new Range(Math.min(min, max), Math.max(min, max)));
    }
    @Override public void setStep(double step) { this.step = Math.max(0.0, step); }

    @Override
    public SetpointSignal optimise(EfficiencySignal eff, String tag, double currentSetpoint) {
        if (eff == null || tag == null) return null;
        double delta = eff.getEfficiency() - eff.getBaseline();
        double proposed = currentSetpoint + Math.signum(delta) * step;
        Range r = constraints.get(tag);
        if (r != null) {
            if (proposed < r.min) proposed = r.min;
            if (proposed > r.max) proposed = r.max;
        }
        return new SetpointSignal(tag, proposed, 0.0, "optimiser");
    }
}
