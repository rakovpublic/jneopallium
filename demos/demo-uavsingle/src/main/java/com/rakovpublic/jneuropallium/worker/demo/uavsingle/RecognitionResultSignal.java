package com.rakovpublic.jneuropallium.worker.demo.uavsingle;

import java.util.LinkedHashMap;
import java.util.Map;

public class RecognitionResultSignal extends UavSingleSignal {
    private String frameId;
    private String targetId;
    private TargetClassification classification = TargetClassification.UNKNOWN_OBJECT;
    private double confidence;
    private double x;
    private double y;
    private Map<String, Double> imageFeatures = new LinkedHashMap<>();

    public RecognitionResultSignal() {
        setEventType("RECOGNITION_RESULT");
    }

    public String getFrameId() { return frameId; }
    public void setFrameId(String frameId) { this.frameId = frameId; }
    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }
    public TargetClassification getClassification() { return classification; }
    public void setClassification(TargetClassification classification) { this.classification = classification; }
    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
    public double getX() { return x; }
    public void setX(double x) { this.x = x; }
    public double getY() { return y; }
    public void setY(double y) { this.y = y; }
    public Map<String, Double> getImageFeatures() { return imageFeatures; }
    public void setImageFeatures(Map<String, Double> imageFeatures) {
        this.imageFeatures = imageFeatures == null ? new LinkedHashMap<>() : new LinkedHashMap<>(imageFeatures);
    }
}
