package com.rakovpublic.jneuropallium.ai.model;

public class AlternativeAction {
    private String actionId;
    private String description;
    private double estimatedHarmScore; // must be below HARMFUL threshold

    public AlternativeAction() {}
    public AlternativeAction(String actionId, String description, double estimatedHarmScore) {
        this.actionId = actionId; this.description = description; this.estimatedHarmScore = estimatedHarmScore;
    }

    public String getActionId() { return actionId; }
    public void setActionId(String actionId) { this.actionId = actionId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public double getEstimatedHarmScore() { return estimatedHarmScore; }
    public void setEstimatedHarmScore(double estimatedHarmScore) { this.estimatedHarmScore = estimatedHarmScore; }
}
