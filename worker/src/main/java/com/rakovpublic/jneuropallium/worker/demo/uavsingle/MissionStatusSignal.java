package com.rakovpublic.jneuropallium.worker.demo.uavsingle;

public class MissionStatusSignal extends UavSingleSignal {
    private UavMissionState state;

    public MissionStatusSignal() {
        setEventType("MISSION_STATUS");
    }

    public MissionStatusSignal(String missionId, String uavId, long tick, UavMissionState state) {
        this();
        setMissionId(missionId);
        setUavId(uavId);
        setTick(tick);
        this.state = state;
    }

    public UavMissionState getState() { return state; }
    public void setState(UavMissionState state) { this.state = state; }
}

