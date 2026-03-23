package com.rakovpublic.jneuropallium.ai.signals.slow;

import com.rakovpublic.jneuropallium.ai.enums.GoalState;
import com.rakovpublic.jneuropallium.ai.signals.BaseSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

public class GoalUpdateSignal extends BaseSignal {
    private String goalId;
    private double priority;
    private GoalState state;

    public GoalUpdateSignal() { super(); this.loop = 2; this.epoch = 1L; }
    public GoalUpdateSignal(String goalId, double priority, GoalState state) {
        this(); this.goalId = goalId; this.priority = priority; this.state = state;
    }

    public String getGoalId() { return goalId; }
    public void setGoalId(String goalId) { this.goalId = goalId; }
    public double getPriority() { return priority; }
    public void setPriority(double priority) { this.priority = priority; }
    public GoalState getState() { return state; }
    public void setState(GoalState state) { this.state = state; }

    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return GoalUpdateSignal.class; }
    @Override public String getDescription() { return "GoalUpdateSignal"; }
    @Override public <K extends ISignal<Void>> K copySignal() {
        GoalUpdateSignal c = new GoalUpdateSignal(goalId, priority, state);
        c.sourceLayer = this.sourceLayer; c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop; c.epoch = this.epoch; c.name = this.name;
        return (K) c;
    }
}
