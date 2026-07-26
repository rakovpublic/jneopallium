package com.rakovpublic.jneuropallium.worker.demo.uavsingle;

public class PhotographResultSignal extends UavSingleSignal {
    private String photographId;
    private String targetId;
    private boolean accepted;
    private double qualityScore;
    private String reason;

    public PhotographResultSignal() {
        setEventType("PHOTOGRAPH_RESULT");
    }

    public String getPhotographId() { return photographId; }
    public void setPhotographId(String photographId) { this.photographId = photographId; }
    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }
    public boolean isAccepted() { return accepted; }
    public void setAccepted(boolean accepted) { this.accepted = accepted; }
    public double getQualityScore() { return qualityScore; }
    public void setQualityScore(double qualityScore) { this.qualityScore = qualityScore; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}

