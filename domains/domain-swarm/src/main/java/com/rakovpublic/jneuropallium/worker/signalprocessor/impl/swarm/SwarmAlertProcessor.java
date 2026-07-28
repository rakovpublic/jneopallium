/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.swarm;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.swarm.ISwarmHarmGateNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm.SwarmAlertSignal;

import java.util.LinkedList;
import java.util.List;

/** Stateless processor: routes a regional alert to the local swarm harm gate. */
public class SwarmAlertProcessor implements ISignalProcessor<SwarmAlertSignal, ISwarmHarmGateNeuron> {

    private static final String DESCRIPTION = "Routes a swarm alert to the local harm gate";

    @Override
    public <I extends ISignal> List<I> process(SwarmAlertSignal input, ISwarmHarmGateNeuron neuron) {
        if (input != null && neuron != null) neuron.onAlert(input);
        return new LinkedList<>();
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return SwarmAlertProcessor.class; }
    @Override public Class<ISwarmHarmGateNeuron> getNeuronClass() { return ISwarmHarmGateNeuron.class; }
    @Override public Class<SwarmAlertSignal> getSignalClass() { return SwarmAlertSignal.class; }
}
