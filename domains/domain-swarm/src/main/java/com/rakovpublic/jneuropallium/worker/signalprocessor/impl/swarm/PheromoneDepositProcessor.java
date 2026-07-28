/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.swarm;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.swarm.IStigmergicMemoryNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm.PheromoneSignal;

import java.util.LinkedList;
import java.util.List;

/** Stateless processor: deposits an incoming pheromone into the local stigmergic cache. */
public class PheromoneDepositProcessor implements ISignalProcessor<PheromoneSignal, IStigmergicMemoryNeuron> {

    private static final String DESCRIPTION = "Pheromone deposit into stigmergic memory";

    @Override
    public <I extends ISignal> List<I> process(PheromoneSignal input, IStigmergicMemoryNeuron neuron) {
        if (input != null && neuron != null) neuron.deposit(input);
        return new LinkedList<>();
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return PheromoneDepositProcessor.class; }
    @Override public Class<IStigmergicMemoryNeuron> getNeuronClass() { return IStigmergicMemoryNeuron.class; }
    @Override public Class<PheromoneSignal> getSignalClass() { return PheromoneSignal.class; }
}
