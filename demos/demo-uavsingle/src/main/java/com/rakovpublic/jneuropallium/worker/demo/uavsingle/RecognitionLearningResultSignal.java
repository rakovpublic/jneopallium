package com.rakovpublic.jneuropallium.worker.demo.uavsingle;

public class RecognitionLearningResultSignal extends UavSingleSignal {
    private String frameId;
    private String targetId;
    private RecognitionFeedbackOutcome outcome;
    private double reward;
    private int updatedMatrices;
    private String reason;

    public RecognitionLearningResultSignal() {
        setEventType("RECOGNITION_LEARNING_RESULT");
    }

    public String getFrameId() { return frameId; }
    public void setFrameId(String frameId) { this.frameId = frameId; }
    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }
    public RecognitionFeedbackOutcome getOutcome() { return outcome; }
    public void setOutcome(RecognitionFeedbackOutcome outcome) { this.outcome = outcome; }
    public double getReward() { return reward; }
    public void setReward(double reward) { this.reward = reward; }
    public int getUpdatedMatrices() { return updatedMatrices; }
    public void setUpdatedMatrices(int updatedMatrices) { this.updatedMatrices = updatedMatrices; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
