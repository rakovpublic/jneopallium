/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.swarm;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.swarm.ITaskRegistryNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm.TaskAnnouncementSignal;

import java.util.LinkedList;
import java.util.List;

/** Stateless processor: registers a task announcement on the active-task list. */
public class TaskAnnouncementRegistryProcessor implements ISignalProcessor<TaskAnnouncementSignal, ITaskRegistryNeuron> {

    private static final String DESCRIPTION = "Task-registry update from task announcement";

    @Override
    public <I extends ISignal> List<I> process(TaskAnnouncementSignal input, ITaskRegistryNeuron neuron) {
        if (input != null && neuron != null) neuron.register(input);
        return new LinkedList<>();
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return TaskAnnouncementRegistryProcessor.class; }
    @Override public Class<ITaskRegistryNeuron> getNeuronClass() { return ITaskRegistryNeuron.class; }
    @Override public Class<TaskAnnouncementSignal> getSignalClass() { return TaskAnnouncementSignal.class; }
}
