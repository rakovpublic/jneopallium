/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.bci;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci.IStimulationSafetyGateNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.bci.SeizureRiskSignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor: an arriving {@link SeizureRiskSignal} above a
 * configurable risk threshold triggers a seizure lockout on an
 * {@link IStimulationSafetyGateNeuron}. Emits no follow-up signals —
 * the lockout is a state change enforced on subsequent commands.
 */
public class SeizureLockoutProcessor implements ISignalProcessor<SeizureRiskSignal, IStimulationSafetyGateNeuron> {

    private static final String DESCRIPTION = "Applies seizure-triggered stimulation lockout";

    private double riskThreshold = 0.8;
    private long lockoutTicks = 60_000L;

    public void setRiskThreshold(double t) { this.riskThreshold = Math.max(0.0, Math.min(1.0, t)); }
    public double getRiskThreshold() { return riskThreshold; }
    public void setLockoutTicks(long t) { this.lockoutTicks = Math.max(0L, t); }
    public long getLockoutTicks() { return lockoutTicks; }

    @Override
    public <I extends ISignal> List<I> process(SeizureRiskSignal input, IStimulationSafetyGateNeuron neuron) {
        if (input == null || neuron == null) return new LinkedList<>();
        if (input.getRisk() >= riskThreshold) {
            neuron.triggerSeizureLockout(input.getEpoch() + lockoutTicks);
        }
        return new LinkedList<>();
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return SeizureLockoutProcessor.class; }
    @Override public Class<IStimulationSafetyGateNeuron> getNeuronClass() { return IStimulationSafetyGateNeuron.class; }
    @Override public Class<SeizureRiskSignal> getSignalClass() { return SeizureRiskSignal.class; }
}
