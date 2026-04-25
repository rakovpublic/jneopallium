/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/** Per spec §4 — auction bid for a task. ProcessingFrequency: loop=2, epoch=1. */
public class TaskBidSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(1L, 2);

    private String taskId;
    private String bidderId;
    private double estimatedCost;
    private double confidence;

    public TaskBidSignal() { super(); this.loop = 2; this.epoch = 1L; this.timeAlive = 200; }

    public TaskBidSignal(String taskId, String bidderId, double estimatedCost, double confidence) {
        this();
        this.taskId = taskId;
        this.bidderId = bidderId;
        this.estimatedCost = estimatedCost;
        this.confidence = Math.max(0.0, Math.min(1.0, confidence));
    }

    public String getTaskId() { return taskId; }
    public void setTaskId(String t) { this.taskId = t; }
    public String getBidderId() { return bidderId; }
    public void setBidderId(String b) { this.bidderId = b; }
    public double getEstimatedCost() { return estimatedCost; }
    public void setEstimatedCost(double c) { this.estimatedCost = c; }
    public double getConfidence() { return confidence; }
    public void setConfidence(double c) { this.confidence = Math.max(0.0, Math.min(1.0, c)); }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return TaskBidSignal.class; }
    @Override public String getDescription() { return "TaskBidSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        TaskBidSignal c = new TaskBidSignal(taskId, bidderId, estimatedCost, confidence);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
