/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/** Outcome of an auction. ProcessingFrequency: loop=2, epoch=1. */
public class TaskAssignmentSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(1L, 2);

    private String taskId;
    private String assigneeId;
    private String witnessId;

    public TaskAssignmentSignal() { super(); this.loop = 2; this.epoch = 1L; this.timeAlive = 500; }

    public TaskAssignmentSignal(String taskId, String assigneeId, String witnessId) {
        this();
        this.taskId = taskId;
        this.assigneeId = assigneeId;
        this.witnessId = witnessId;
    }

    public String getTaskId() { return taskId; }
    public void setTaskId(String t) { this.taskId = t; }
    public String getAssigneeId() { return assigneeId; }
    public void setAssigneeId(String a) { this.assigneeId = a; }
    public String getWitnessId() { return witnessId; }
    public void setWitnessId(String w) { this.witnessId = w; }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return TaskAssignmentSignal.class; }
    @Override public String getDescription() { return "TaskAssignmentSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        TaskAssignmentSignal c = new TaskAssignmentSignal(taskId, assigneeId, witnessId);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
