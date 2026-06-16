package com.rakovpublic.jneuropallium.worker.demo.uavsingle;

public class ObservationTarget {
    public String targetId;
    public TargetClassification classification = TargetClassification.UNKNOWN_OBJECT;
    public double x;
    public double y;
    public double missionRelevance = 0.5;
    public double confidence = 0.5;
    public double urgency = 0.5;
    public double informationValue = 0.5;
    public double communicationValue = 0.5;
    public double safetyRisk = 0.1;
    public double visibility = 0.9;
    public boolean inFieldOfView = true;
    public double motionBlurEstimate = 0.1;
    public boolean active = true;

    public ObservationTarget() {
    }

    public ObservationTarget(String targetId, TargetClassification classification, double x, double y) {
        this.targetId = targetId;
        this.classification = classification == null ? TargetClassification.UNKNOWN_OBJECT : classification;
        this.x = x;
        this.y = y;
    }
}

