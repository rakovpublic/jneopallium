package com.rakovpublic.jneuropallium.worker.demo.uavsingle;

public class UavMissionProcessor {
    public UavIntentSignal intent(UavSingleConfig config, long tick, UavActionType action, ObservationTarget target,
                                  UavPose destinationPose) {
        UavIntentSignal signal = new UavIntentSignal();
        signal.setMissionId(config.missionId);
        signal.setUavId(config.uavId);
        signal.setTick(tick);
        signal.setIntentId("intent-" + config.missionId + "-" + tick + "-" + action);
        signal.setActionType(action);
        signal.setTargetId(target == null ? null : target.targetId);
        signal.setDestinationX(destinationPose.x);
        signal.setDestinationY(destinationPose.y);
        signal.setAltitudeMeters(destinationPose.altitudeMeters);
        signal.setSpeedMetersPerSecond(Math.min(4.0, config.maximumSpeedMetersPerSecond));
        signal.setExpiresAtTick(tick + 2);
        return signal;
    }
}

