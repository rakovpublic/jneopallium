/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.swarm;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.swarm.ITaskRegistryNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm.TaskBidSignal;

import java.util.LinkedList;
import java.util.List;

/** Stateless processor: forwards an incoming bid into the registry's bid-observation channel. */
public class TaskBidRegistryProcessor implements ISignalProcessor<TaskBidSignal, ITaskRegistryNeuron> {

    private static final String DESCRIPTION = "Records auction bids on the task registry";

    @Override
    public <I extends ISignal> List<I> process(TaskBidSignal input, ITaskRegistryNeuron neuron) {
        if (input != null && neuron != null) neuron.observeBid(input);
        return new LinkedList<>();
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return TaskBidRegistryProcessor.class; }
    @Override public Class<ITaskRegistryNeuron> getNeuronClass() { return ITaskRegistryNeuron.class; }
    @Override public Class<TaskBidSignal> getSignalClass() { return TaskBidSignal.class; }
}
