package com.rakovpublic.jneuropallium.worker.demo.uavsingle;

public class PhotographRequestSignal extends UavSingleSignal {
    private String photographRequestId;
    private String targetId;

    public PhotographRequestSignal() {
        setEventType("PHOTOGRAPH_REQUEST");
    }

    public PhotographRequestSignal(String missionId, String uavId, long tick, String photographRequestId, String targetId) {
        this();
        setMissionId(missionId);
        setUavId(uavId);
        setTick(tick);
        this.photographRequestId = photographRequestId;
        this.targetId = targetId;
    }

    public String getPhotographRequestId() { return photographRequestId; }
    public void setPhotographRequestId(String photographRequestId) { this.photographRequestId = photographRequestId; }
    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }
}

