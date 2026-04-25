/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.swarm.TaskKind;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/**
 * Announcement of a new task that any agent can bid on.
 * ProcessingFrequency: loop=2, epoch=1.
 */
public class TaskAnnouncementSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(1L, 2);

    private String taskId;
    private TaskKind kind;
    private double[] locationGlobal;
    private double reward;
    private long deadlineTick;

    public TaskAnnouncementSignal() {
        super();
        this.loop = 2;
        this.epoch = 1L;
        this.timeAlive = 1000;
        this.kind = TaskKind.EXPLORE;
    }

    public TaskAnnouncementSignal(String taskId, TaskKind kind, double[] locationGlobal,
                                  double reward, long deadlineTick) {
        this();
        this.taskId = taskId;
        this.kind = kind == null ? TaskKind.EXPLORE : kind;
        this.locationGlobal = locationGlobal == null ? null : locationGlobal.clone();
        this.reward = reward;
        this.deadlineTick = deadlineTick;
    }

    public String getTaskId() { return taskId; }
    public void setTaskId(String t) { this.taskId = t; }
    public TaskKind getKind() { return kind; }
    public void setKind(TaskKind k) { this.kind = k == null ? TaskKind.EXPLORE : k; }
    public double[] getLocationGlobal() { return locationGlobal == null ? null : locationGlobal.clone(); }
    public void setLocationGlobal(double[] v) { this.locationGlobal = v == null ? null : v.clone(); }
    public double getReward() { return reward; }
    public void setReward(double r) { this.reward = r; }
    public long getDeadlineTick() { return deadlineTick; }
    public void setDeadlineTick(long d) { this.deadlineTick = d; }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return TaskAnnouncementSignal.class; }
    @Override public String getDescription() { return "TaskAnnouncementSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        TaskAnnouncementSignal c = new TaskAnnouncementSignal(taskId, kind, locationGlobal, reward, deadlineTick);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
