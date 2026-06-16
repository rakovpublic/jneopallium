package com.rakovpublic.jneuropallium.worker.demo.uavsingle;

public class SafetyConstraintSignal extends UavSingleSignal {
    private String constraint;
    private boolean veto;
    private String reason;

    public SafetyConstraintSignal() {
        setEventType("SAFETY_CONSTRAINT");
    }

    public SafetyConstraintSignal(String missionId, String uavId, long tick, String constraint, boolean veto, String reason) {
        this();
        setMissionId(missionId);
        setUavId(uavId);
        setTick(tick);
        this.constraint = constraint;
        this.veto = veto;
        this.reason = reason;
    }

    public String getConstraint() { return constraint; }
    public void setConstraint(String constraint) { this.constraint = constraint; }
    public boolean isVeto() { return veto; }
    public void setVeto(boolean veto) { this.veto = veto; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}

