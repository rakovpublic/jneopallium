package com.rakovpublic.jneuropallium.worker.demo.uavsingle;

public class SimUavSupervisorContext {
    public UavSingleConfig config;
    public UavPose pose;
    public boolean jneopalliumHeartbeatHealthy = true;
    public boolean autopilotHeartbeatHealthy = true;
    public boolean operatorOverrideActive = false;
    public boolean harmVetoActive = false;
    public long tick;
    public String missionId;

    public SimUavSupervisorContext(UavSingleConfig config, UavPose pose, long tick) {
        this.config = config;
        this.pose = pose;
        this.tick = tick;
        this.missionId = config.missionId;
    }
}

