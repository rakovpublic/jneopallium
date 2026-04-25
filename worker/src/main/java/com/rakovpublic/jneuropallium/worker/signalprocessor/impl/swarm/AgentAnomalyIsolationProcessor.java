/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.swarm;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.swarm.IIsolationProtocolNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm.AgentAnomalySignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor: an arriving anomaly report is offered to the
 * local Byzantine-tolerance neuron; isolation is only applied once the
 * configured witness threshold is met.
 */
public class AgentAnomalyIsolationProcessor implements ISignalProcessor<AgentAnomalySignal, IIsolationProtocolNeuron> {

    private static final String DESCRIPTION = "k-witness isolation of misbehaving peers";

    @Override
    public <I extends ISignal> List<I> process(AgentAnomalySignal input, IIsolationProtocolNeuron neuron) {
        if (input != null && neuron != null) neuron.onReport(input, input.getEpoch());
        return new LinkedList<>();
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return AgentAnomalyIsolationProcessor.class; }
    @Override public Class<IIsolationProtocolNeuron> getNeuronClass() { return IIsolationProtocolNeuron.class; }
    @Override public Class<AgentAnomalySignal> getSignalClass() { return AgentAnomalySignal.class; }
}
