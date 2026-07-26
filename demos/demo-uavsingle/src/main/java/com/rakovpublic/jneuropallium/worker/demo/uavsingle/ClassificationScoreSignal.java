package com.rakovpublic.jneuropallium.worker.demo.uavsingle;

public class ClassificationScoreSignal extends UavSingleSignal {
    private String frameId;
    private TargetClassification classification;
    private double score;
    private double featureDistance;

    public ClassificationScoreSignal() {
        setEventType("CLASSIFICATION_SCORE");
    }

    public String getFrameId() { return frameId; }
    public void setFrameId(String frameId) { this.frameId = frameId; }
    public TargetClassification getClassification() { return classification; }
    public void setClassification(TargetClassification classification) { this.classification = classification; }
    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }
    public double getFeatureDistance() { return featureDistance; }
    public void setFeatureDistance(double featureDistance) { this.featureDistance = featureDistance; }
}
