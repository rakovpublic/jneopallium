package com.rakovpublic.jneuropallium.ai.model;

import com.rakovpublic.jneuropallium.ai.enums.GoalState;
import com.rakovpublic.jneuropallium.ai.signals.fast.MotorCommandSignal;

import java.util.List;

public class ActionPlan {
    private String planId;
    private List<MotorCommandSignal> steps;
    private double expectedValue;
    private GoalState targetGoalState;

    public ActionPlan() {}
    public ActionPlan(String planId, List<MotorCommandSignal> steps, double expectedValue, GoalState targetGoalState) {
        this.planId = planId; this.steps = steps;
        this.expectedValue = expectedValue; this.targetGoalState = targetGoalState;
    }

    public String getPlanId() { return planId; }
    public void setPlanId(String planId) { this.planId = planId; }
    public List<MotorCommandSignal> getSteps() { return steps; }
    public void setSteps(List<MotorCommandSignal> steps) { this.steps = steps; }
    public double getExpectedValue() { return expectedValue; }
    public void setExpectedValue(double expectedValue) { this.expectedValue = expectedValue; }
    public GoalState getTargetGoalState() { return targetGoalState; }
    public void setTargetGoalState(GoalState targetGoalState) { this.targetGoalState = targetGoalState; }
}
