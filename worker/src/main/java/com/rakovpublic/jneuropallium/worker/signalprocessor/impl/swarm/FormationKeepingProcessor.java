/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.swarm;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.swarm.IFormationKeepingNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm.FormationSignal;

import java.util.LinkedList;
import java.util.List;

/** Stateless processor: assigns a new formation slot to the formation-keeper. */
public class FormationKeepingProcessor implements ISignalProcessor<FormationSignal, IFormationKeepingNeuron> {

    private static final String DESCRIPTION = "Assigns formation slot to formation-keeper";

    @Override
    public <I extends ISignal> List<I> process(FormationSignal input, IFormationKeepingNeuron neuron) {
        if (input != null && neuron != null) neuron.setSlot(input);
        return new LinkedList<>();
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return FormationKeepingProcessor.class; }
    @Override public Class<IFormationKeepingNeuron> getNeuronClass() { return IFormationKeepingNeuron.class; }
    @Override public Class<FormationSignal> getSignalClass() { return FormationSignal.class; }
}
