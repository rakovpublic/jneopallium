/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.swarm;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm.TaskAnnouncementSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm.TaskAssignmentSignal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Layer 3 active-task registry, reconciled via consensus. Loop=2 / Epoch=1. */
public class TaskRegistryNeuron extends ModulatableNeuron implements ITaskRegistryNeuron {

    private final Set<String> open = new HashSet<>();
    private final Map<String, String> assignedTo = new HashMap<>();

    public TaskRegistryNeuron() { super(); }
    public TaskRegistryNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    @Override
    public void register(TaskAnnouncementSignal a) {
        if (a == null || a.getTaskId() == null) return;
        open.add(a.getTaskId());
    }

    @Override
    public void assign(TaskAssignmentSignal a) {
        if (a == null || a.getTaskId() == null) return;
        if (a.getAssigneeId() != null) assignedTo.put(a.getTaskId(), a.getAssigneeId());
        open.remove(a.getTaskId());
    }

    @Override public Set<String> activeTasks() { return new HashSet<>(open); }
    @Override public String assigneeOf(String taskId) { return assignedTo.get(taskId); }
    @Override public boolean isOpen(String taskId) { return open.contains(taskId); }
}
