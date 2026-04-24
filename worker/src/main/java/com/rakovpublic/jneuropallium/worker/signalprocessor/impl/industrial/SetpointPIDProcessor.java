/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.industrial;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.IPIDNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.SetpointSignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor: forwards a new {@link SetpointSignal} to the
 * PID neuron. The PID holds the setpoint until the next measurement
 * triggers a step.
 */
public class SetpointPIDProcessor implements ISignalProcessor<SetpointSignal, IPIDNeuron> {

    private static final String DESCRIPTION = "PID setpoint update";

    @Override
    public <I extends ISignal> List<I> process(SetpointSignal input, IPIDNeuron neuron) {
        if (input != null && neuron != null) neuron.setSetpoint(input);
        return new LinkedList<>();
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return SetpointPIDProcessor.class; }
    @Override public Class<IPIDNeuron> getNeuronClass() { return IPIDNeuron.class; }
    @Override public Class<SetpointSignal> getSignalClass() { return SetpointSignal.class; }
}
