package com.rakovpublic.jneuropallium.worker.demo.uavsingle;

public class TargetConfirmationRequestSignal extends UavSingleSignal {
    private String requestId;
    private String targetId;
    private UavActionType action;
    private long expiresAtTick;

    public TargetConfirmationRequestSignal() {
        setEventType("TARGET_CONFIRMATION_REQUEST");
    }

    public TargetConfirmationRequestSignal(String missionId, String uavId, long tick, String requestId,
                                           String targetId, UavActionType action, long expiresAtTick) {
        this();
        setMissionId(missionId);
        setUavId(uavId);
        setTick(tick);
        this.requestId = requestId;
        this.targetId = targetId;
        this.action = action;
        this.expiresAtTick = expiresAtTick;
    }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }
    public UavActionType getAction() { return action; }
    public void setAction(UavActionType action) { this.action = action; }
    public long getExpiresAtTick() { return expiresAtTick; }
    public void setExpiresAtTick(long expiresAtTick) { this.expiresAtTick = expiresAtTick; }
}

