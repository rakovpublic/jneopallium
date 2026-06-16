package com.rakovpublic.jneuropallium.worker.demo.uavsingle;

public class TargetCandidateSignal extends UavSingleSignal {
    private String targetId;
    private double priorityScore;

    public TargetCandidateSignal() {
        setEventType("TARGET_CANDIDATE");
    }

    public TargetCandidateSignal(String missionId, String uavId, long tick, TargetPriority priority) {
        this();
        setMissionId(missionId);
        setUavId(uavId);
        setTick(tick);
        this.targetId = priority.targetId;
        this.priorityScore = priority.score;
    }

    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }
    public double getPriorityScore() { return priorityScore; }
    public void setPriorityScore(double priorityScore) { this.priorityScore = priorityScore; }
}

