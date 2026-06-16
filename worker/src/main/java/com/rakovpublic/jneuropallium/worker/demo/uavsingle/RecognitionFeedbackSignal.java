package com.rakovpublic.jneuropallium.worker.demo.uavsingle;

import java.util.LinkedHashMap;
import java.util.Map;

public class RecognitionFeedbackSignal extends UavSingleSignal {
    private String frameId;
    private String targetId;
    private TargetClassification predictedClassification = TargetClassification.UNKNOWN_OBJECT;
    private TargetClassification expectedClassification = TargetClassification.UNKNOWN_OBJECT;
    private RecognitionFeedbackOutcome outcome = RecognitionFeedbackOutcome.PHOTO_REJECTED;
    private double reward;
    private double learningRate;
    private String reason;
    private Map<String, Double> imageFeatures = new LinkedHashMap<>();

    public RecognitionFeedbackSignal() {
        setEventType("RECOGNITION_FEEDBACK");
    }

    public String getFrameId() { return frameId; }
    public void setFrameId(String frameId) { this.frameId = frameId; }
    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }
    public TargetClassification getPredictedClassification() { return predictedClassification; }
    public void setPredictedClassification(TargetClassification predictedClassification) {
        this.predictedClassification = predictedClassification == null
                ? TargetClassification.UNKNOWN_OBJECT : predictedClassification;
    }
    public TargetClassification getExpectedClassification() { return expectedClassification; }
    public void setExpectedClassification(TargetClassification expectedClassification) {
        this.expectedClassification = expectedClassification == null
                ? TargetClassification.UNKNOWN_OBJECT : expectedClassification;
    }
    public RecognitionFeedbackOutcome getOutcome() { return outcome; }
    public void setOutcome(RecognitionFeedbackOutcome outcome) {
        this.outcome = outcome == null ? RecognitionFeedbackOutcome.PHOTO_REJECTED : outcome;
    }
    public double getReward() { return reward; }
    public void setReward(double reward) { this.reward = Math.max(-1.0, Math.min(1.0, reward)); }
    public double getLearningRate() { return learningRate; }
    public void setLearningRate(double learningRate) {
        this.learningRate = TargetPriorityProcessor.clamp(learningRate);
    }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Map<String, Double> getImageFeatures() { return imageFeatures; }
    public void setImageFeatures(Map<String, Double> imageFeatures) {
        this.imageFeatures = imageFeatures == null ? new LinkedHashMap<>() : new LinkedHashMap<>(imageFeatures);
    }
}
