/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.industrial;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.ISetpointOptimiserNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.EfficiencySignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.SetpointSignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor: turns an {@link EfficiencySignal} into a proposed
 * {@link SetpointSignal} nudge via the setpoint optimiser. The target
 * tag and the current setpoint must be supplied by the caller; this
 * implementation uses the unit id as tag and a current setpoint of zero
 * as a neutral default (a real deployment injects a factory).
 */
public class EfficiencyOptimiserProcessor implements ISignalProcessor<EfficiencySignal, ISetpointOptimiserNeuron> {

    private static final String DESCRIPTION = "Efficiency-driven setpoint optimiser";

    private double currentSetpoint;

    public void setCurrentSetpoint(double v) { this.currentSetpoint = v; }
    public double getCurrentSetpoint() { return currentSetpoint; }

    @Override
    @SuppressWarnings("unchecked")
    public <I extends ISignal> List<I> process(EfficiencySignal input, ISetpointOptimiserNeuron neuron) {
        List<I> out = new LinkedList<>();
        if (input == null || neuron == null || input.getUnitId() == null) return out;
        SetpointSignal sp = neuron.optimise(input, input.getUnitId(), currentSetpoint);
        if (sp != null) out.add((I) sp);
        return out;
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return EfficiencyOptimiserProcessor.class; }
    @Override public Class<ISetpointOptimiserNeuron> getNeuronClass() { return ISetpointOptimiserNeuron.class; }
    @Override public Class<EfficiencySignal> getSignalClass() { return EfficiencySignal.class; }
}
