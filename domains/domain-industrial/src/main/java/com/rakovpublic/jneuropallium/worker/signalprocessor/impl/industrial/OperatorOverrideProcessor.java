/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.industrial;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.IActuatorNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.OperatorOverrideSignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor: registers an {@link OperatorOverrideSignal} on
 * the actuator neuron. Per spec §5 override always wins for regulatory
 * control.
 */
public class OperatorOverrideProcessor implements ISignalProcessor<OperatorOverrideSignal, IActuatorNeuron> {

    private static final String DESCRIPTION = "Operator-override registration on the actuator";

    @Override
    public <I extends ISignal> List<I> process(OperatorOverrideSignal input, IActuatorNeuron neuron) {
        if (input != null && neuron != null) neuron.onOverride(input);
        return new LinkedList<>();
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return OperatorOverrideProcessor.class; }
    @Override public Class<IActuatorNeuron> getNeuronClass() { return IActuatorNeuron.class; }
    @Override public Class<OperatorOverrideSignal> getSignalClass() { return OperatorOverrideSignal.class; }
}
