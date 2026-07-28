package com.rakovpublic.jneuropallium.worker.demo.uavsingle;

public class TargetDetectionSignal extends UavSingleSignal {
    private String targetId;
    private TargetClassification classification = TargetClassification.UNKNOWN_OBJECT;
    private double confidence;

    public TargetDetectionSignal() {
        setEventType("TARGET_DETECTION");
    }

    public TargetDetectionSignal(String missionId, String uavId, long tick, ObservationTarget target) {
        this();
        setMissionId(missionId);
        setUavId(uavId);
        setTick(tick);
        this.targetId = target.targetId;
        this.classification = target.classification;
        this.confidence = target.confidence;
    }

    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }
    public TargetClassification getClassification() { return classification; }
    public void setClassification(TargetClassification classification) { this.classification = classification; }
    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
}

