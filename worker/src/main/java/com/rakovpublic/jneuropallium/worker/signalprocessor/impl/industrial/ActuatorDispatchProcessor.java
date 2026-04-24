/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.industrial;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.IActuatorNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.ActuatorCommandSignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor: dispatches a gated {@link ActuatorCommandSignal}
 * to the field. Operator overrides held on the actuator neuron take
 * precedence over the command (spec §5: override always wins).
 */
public class ActuatorDispatchProcessor implements ISignalProcessor<ActuatorCommandSignal, IActuatorNeuron> {

    private static final String DESCRIPTION = "Field-layer actuator dispatch (override-aware)";

    @Override
    public <I extends ISignal> List<I> process(ActuatorCommandSignal input, IActuatorNeuron neuron) {
        if (input != null && neuron != null) neuron.dispatch(input);
        return new LinkedList<>();
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return ActuatorDispatchProcessor.class; }
    @Override public Class<IActuatorNeuron> getNeuronClass() { return IActuatorNeuron.class; }
    @Override public Class<ActuatorCommandSignal> getSignalClass() { return ActuatorCommandSignal.class; }
}
