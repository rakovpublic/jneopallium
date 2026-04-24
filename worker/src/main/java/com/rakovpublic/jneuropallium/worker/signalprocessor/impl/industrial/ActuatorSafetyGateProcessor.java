/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.industrial;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.ISafetyGateNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.ActuatorCommandSignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor: every proposed {@link ActuatorCommandSignal}
 * passes through the safety gate, which downgrades execute=false in
 * SHADOW mode and passes unchanged otherwise.
 */
public class ActuatorSafetyGateProcessor implements ISignalProcessor<ActuatorCommandSignal, ISafetyGateNeuron> {

    private static final String DESCRIPTION = "Per-loop safety gate (shadow / advisory / autonomous)";

    @Override
    @SuppressWarnings("unchecked")
    public <I extends ISignal> List<I> process(ActuatorCommandSignal input, ISafetyGateNeuron neuron) {
        List<I> out = new LinkedList<>();
        if (input == null || neuron == null) return out;
        ActuatorCommandSignal gated = neuron.gate(input);
        if (gated != null) out.add((I) gated);
        return out;
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return ActuatorSafetyGateProcessor.class; }
    @Override public Class<ISafetyGateNeuron> getNeuronClass() { return ISafetyGateNeuron.class; }
    @Override public Class<ActuatorCommandSignal> getSignalClass() { return ActuatorCommandSignal.class; }
}
