package com.rakovpublic.jneuropallium.ai.model;

import com.rakovpublic.jneuropallium.ai.enums.InterventionType;

public class ActiveIntervention {
    private String interventionId;
    private InterventionType type;
    private String targetId;
    private double originalValue;
    private int remainingTicks;
    private boolean restored;

    public ActiveIntervention() {}
    public ActiveIntervention(String interventionId, InterventionType type, String targetId, double originalValue, int remainingTicks) {
        this.interventionId = interventionId; this.type = type; this.targetId = targetId;
        this.originalValue = originalValue; this.remainingTicks = remainingTicks; this.restored = false;
    }

    public String getInterventionId() { return interventionId; }
    public void setInterventionId(String interventionId) { this.interventionId = interventionId; }
    public InterventionType getType() { return type; }
    public void setType(InterventionType type) { this.type = type; }
    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }
    public double getOriginalValue() { return originalValue; }
    public void setOriginalValue(double originalValue) { this.originalValue = originalValue; }
    public int getRemainingTicks() { return remainingTicks; }
    public void setRemainingTicks(int remainingTicks) { this.remainingTicks = remainingTicks; }
    public boolean isRestored() { return restored; }
    public void setRestored(boolean restored) { this.restored = restored; }
    public void decrementTick() { if (remainingTicks > 0) remainingTicks--; }
}
