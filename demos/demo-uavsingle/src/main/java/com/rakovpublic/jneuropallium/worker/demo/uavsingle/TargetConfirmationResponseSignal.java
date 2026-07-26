package com.rakovpublic.jneuropallium.worker.demo.uavsingle;

public class TargetConfirmationResponseSignal extends UavSingleSignal {
    private String requestId;
    private String targetId;
    private UavActionType action;
    private ConfirmationDecision decision;

    public TargetConfirmationResponseSignal() {
        setEventType("TARGET_CONFIRMATION_RESPONSE");
    }

    public TargetConfirmationResponseSignal(String missionId, String uavId, long tick, String requestId,
                                            String targetId, UavActionType action, ConfirmationDecision decision) {
        this();
        setMissionId(missionId);
        setUavId(uavId);
        setTick(tick);
        this.requestId = requestId;
        this.targetId = targetId;
        this.action = action;
        this.decision = decision;
    }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }
    public UavActionType getAction() { return action; }
    public void setAction(UavActionType action) { this.action = action; }
    public ConfirmationDecision getDecision() { return decision; }
    public void setDecision(ConfirmationDecision decision) { this.decision = decision; }
}

