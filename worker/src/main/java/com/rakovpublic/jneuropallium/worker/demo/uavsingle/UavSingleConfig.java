package com.rakovpublic.jneuropallium.worker.demo.uavsingle;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class UavSingleConfig {
    public int schemaVersion = 1;
    public UavOperatingMode mode = UavOperatingMode.FULLY_AUTONOMOUS;
    public boolean simulatorOnly = true;
    public String missionId = "mission-uav-single";
    public String uavId = "uav-1";
    public int vehicleSystemId = 1;
    public List<Integer> vehicleAllowlist = new ArrayList<>(List.of(1));
    public String mavlinkEndpoint = "udp://127.0.0.1:14550";
    public String ros2Endpoint = "rosbridge://127.0.0.1:9090";
    public double targetObservationDistanceMeters = 35.0;
    public double prohibitedMinimumTargetDistanceMeters = 20.0;
    public double maximumAltitudeMeters = 120.0;
    public double maximumSpeedMetersPerSecond = 12.0;
    public double batteryReserveFraction = 0.20;
    public long confirmationTimeoutTicks = 4L;
    public int photographRetryLimit = 2;
    public long deterministicSeed = 24703042047L;
    public Geofence geofence = new Geofence();
    public List<NoGoZone> noGoZones = new ArrayList<>();
    public Map<String, Double> priorityWeights = defaultPriorityWeights();

    public static Map<String, Double> defaultPriorityWeights() {
        Map<String, Double> weights = new LinkedHashMap<>();
        weights.put("missionRelevance", 0.25);
        weights.put("confidence", 0.20);
        weights.put("urgency", 0.15);
        weights.put("informationValue", 0.10);
        weights.put("proximityBenefit", 0.10);
        weights.put("routeEfficiency", 0.10);
        weights.put("communicationValue", 0.10);
        weights.put("safetyRisk", -0.20);
        weights.put("energyCost", -0.15);
        weights.put("duplicationPenalty", -0.10);
        return weights;
    }

    public void validate() {
        if (schemaVersion != 1) {
            throw new IllegalArgumentException("Unsupported UAV single config schemaVersion=" + schemaVersion);
        }
        if (!simulatorOnly) {
            throw new IllegalArgumentException("UAV single demo is simulation-only; simulatorOnly must be true");
        }
        if (missionId == null || missionId.isBlank()) {
            throw new IllegalArgumentException("missionId is required");
        }
        if (uavId == null || uavId.isBlank()) {
            throw new IllegalArgumentException("uavId is required");
        }
        if (vehicleAllowlist == null || !vehicleAllowlist.contains(vehicleSystemId)) {
            throw new IllegalArgumentException("vehicleSystemId must be in vehicleAllowlist");
        }
        if (targetObservationDistanceMeters <= prohibitedMinimumTargetDistanceMeters) {
            throw new IllegalArgumentException("targetObservationDistanceMeters must exceed prohibitedMinimumTargetDistanceMeters");
        }
        if (maximumAltitudeMeters <= 0.0 || maximumSpeedMetersPerSecond <= 0.0) {
            throw new IllegalArgumentException("maximum altitude and speed must be positive");
        }
        if (batteryReserveFraction < 0.0 || batteryReserveFraction >= 1.0) {
            throw new IllegalArgumentException("batteryReserveFraction must be in [0,1)");
        }
        if (confirmationTimeoutTicks <= 0L) {
            throw new IllegalArgumentException("confirmationTimeoutTicks must be positive");
        }
        if (photographRetryLimit < 0) {
            throw new IllegalArgumentException("photographRetryLimit must be non-negative");
        }
        if (priorityWeights == null) {
            priorityWeights = defaultPriorityWeights();
        }
        for (String key : defaultPriorityWeights().keySet()) {
            if (!priorityWeights.containsKey(key)) {
                throw new IllegalArgumentException("priorityWeights missing required factor " + key);
            }
        }
        if (geofence == null) {
            geofence = new Geofence();
        }
        if (noGoZones == null) {
            noGoZones = new ArrayList<>();
        }
    }
}

