/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.swarm;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.swarm.ITaskRegistryNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm.TaskAssignmentSignal;

import java.util.LinkedList;
import java.util.List;

/** Stateless processor: closes the open-task entry on assignment. */
public class TaskAssignmentRegistryProcessor implements ISignalProcessor<TaskAssignmentSignal, ITaskRegistryNeuron> {

    private static final String DESCRIPTION = "Task-registry update from auction outcome";

    @Override
    public <I extends ISignal> List<I> process(TaskAssignmentSignal input, ITaskRegistryNeuron neuron) {
        if (input != null && neuron != null) neuron.assign(input);
        return new LinkedList<>();
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return TaskAssignmentRegistryProcessor.class; }
    @Override public Class<ITaskRegistryNeuron> getNeuronClass() { return ITaskRegistryNeuron.class; }
    @Override public Class<TaskAssignmentSignal> getSignalClass() { return TaskAssignmentSignal.class; }
}
