package com.rakovpublic.jneuropallium.worker.demo.uavsingle;

import java.util.ArrayList;
import java.util.List;

public class SimUavMissionSupervisor {
    public SupervisorDecision validate(UavIntentSignal intent, SimUavSupervisorContext context) {
        List<String> reasons = new ArrayList<>();
        UavSingleConfig config = context.config;
        if (!config.simulatorOnly) {
            reasons.add("SIMULATOR_ONLY_REQUIRED");
        }
        if (!config.vehicleAllowlist.contains(config.vehicleSystemId)) {
            reasons.add("VEHICLE_SYSTEM_ID_NOT_ALLOWLISTED");
        }
        if (!context.jneopalliumHeartbeatHealthy) {
            reasons.add("JNEOPALLIUM_HEARTBEAT_STALE");
        }
        if (!context.autopilotHeartbeatHealthy) {
            reasons.add("AUTOPILOT_HEARTBEAT_STALE");
        }
        if (intent.getExpiresAtTick() < context.tick) {
            reasons.add("COMMAND_EXPIRED");
        }
        if (!config.missionId.equals(intent.getMissionId()) || !config.missionId.equals(context.missionId)) {
            reasons.add("MISSION_ID_MISMATCH");
        }
        if (!config.geofence.contains(intent.getDestinationX(), intent.getDestinationY())) {
            reasons.add("GEOFENCE_REJECTED");
        }
        for (NoGoZone zone : config.noGoZones) {
            if (zone.contains(intent.getDestinationX(), intent.getDestinationY())) {
                reasons.add("NO_GO_ZONE_REJECTED:" + zone.zoneId);
            }
        }
        if (intent.getAltitudeMeters() < 0.0 || intent.getAltitudeMeters() > config.maximumAltitudeMeters) {
            reasons.add("ALTITUDE_LIMIT_REJECTED");
        }
        if (intent.getSpeedMetersPerSecond() < 0.0
                || intent.getSpeedMetersPerSecond() > config.maximumSpeedMetersPerSecond) {
            reasons.add("SPEED_LIMIT_REJECTED");
        }
        if (context.pose.batteryFraction < config.batteryReserveFraction) {
            reasons.add("BATTERY_RESERVE_REJECTED");
        }
        if (context.pose.localizationConfidence < 0.55) {
            reasons.add("LOCALIZATION_REJECTED");
        }
        if (isTargetSpecific(intent.getActionType())) {
            double distance = context.pose.distance2d(intent.getDestinationX(), intent.getDestinationY());
            if (distance < config.prohibitedMinimumTargetDistanceMeters) {
                reasons.add("MINIMUM_TARGET_DISTANCE_REJECTED");
            }
        }
        if (context.operatorOverrideActive) {
            reasons.add("OPERATOR_OVERRIDE_ACTIVE");
        }
        if (context.harmVetoActive) {
            reasons.add("HARM_VETO_ACTIVE");
        }
        return reasons.isEmpty() ? SupervisorDecision.accepted() : SupervisorDecision.rejected(reasons);
    }

    private static boolean isTargetSpecific(UavActionType actionType) {
        return actionType == UavActionType.APPROACH_OBSERVATION_POINT
                || actionType == UavActionType.OBSERVE_TARGET
                || actionType == UavActionType.ORBIT_TARGET
                || actionType == UavActionType.PHOTOGRAPH_TARGET;
    }
}

