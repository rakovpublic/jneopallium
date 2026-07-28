package com.rakovpublic.jneuropallium.worker.net.neuron.impl.swarm;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm.TaskAnnouncementSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm.TaskAssignmentSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm.TaskBidSignal;

import java.util.Set;

public interface ITaskRegistryNeuron extends IModulatableNeuron {
    void register(TaskAnnouncementSignal a);
    void assign(TaskAssignmentSignal a);
    Set<String> activeTasks();
    String assigneeOf(String taskId);
    boolean isOpen(String taskId);

    /**
     * Observation channel: record that a bid arrived for a known task —
     * a deployment can wire this into a winner-selection step. Default
     * is a no-op so existing implementations stay unchanged.
     */
    default void observeBid(TaskBidSignal b) { /* no-op by default */ }
}
