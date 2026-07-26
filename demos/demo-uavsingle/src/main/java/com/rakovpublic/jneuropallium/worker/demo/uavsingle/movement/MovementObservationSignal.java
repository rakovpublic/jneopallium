package com.rakovpublic.jneuropallium.worker.demo.uavsingle.movement;

import com.rakovpublic.jneuropallium.worker.demo.uavsingle.UavSingleSignal;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Public, simulator-agnostic observation handed to the movement policy each control tick.
 *
 * <p>The CARLA-Air bridge (and the in-memory demo) only expose information a real UAV could obtain
 * from its own state estimate plus on-board perception: position offset inside the search area,
 * the area bounds, the vector/range to the nearest still-unphotographed recognized target, the most
 * recent occlusion rejection, an obstacle-risk estimate per candidate action (from ray-casts), and
 * progress counters. The policy turns this into a {@link MotorCommand}.
 */
public class MovementObservationSignal extends UavSingleSignal {
    private long frame;
    private double elapsedSeconds;
    private double positionX;
    private double positionY;
    private double minX = -100.0;
    private double maxX = 100.0;
    private double minY = -100.0;
    private double maxY = 100.0;
    private boolean hasNearestTarget;
    private double nearestVectorX;
    private double nearestVectorY;
    private double nearestDistanceMeters;
    private boolean hasOcclusion;
    private double occlusionVectorX;
    private double occlusionVectorY;
    private final Map<String, Double> obstacleRiskByAction = new LinkedHashMap<>();
    private final Map<String, Double> lidarRiskByAction = new LinkedHashMap<>();
    private boolean lidarAvailable;
    private double lidarMinDistanceMeters = Double.POSITIVE_INFINITY;
    private double lidarFrontMinMeters = Double.POSITIVE_INFINITY;
    private double lidarLeftMinMeters = Double.POSITIVE_INFINITY;
    private double lidarRightMinMeters = Double.POSITIVE_INFINITY;
    private double lidarRearMinMeters = Double.POSITIVE_INFINITY;
    private int lidarPoints;
    private int photographedTargets;
    private int remainingTargets;
    private int totalTargets = 1;
    private double baseSpeedMetersPerSecond = 8.0;
    private double photoRadiusMeters = 1.0;
    private double cameraFootprintMeters = 45.0;
    private double headingYawDegrees;

    public MovementObservationSignal() {
        setEventType("MOVEMENT_OBSERVATION");
    }

    public long getFrame() { return frame; }
    public void setFrame(long frame) { this.frame = frame; }
    public double getElapsedSeconds() { return elapsedSeconds; }
    public void setElapsedSeconds(double elapsedSeconds) { this.elapsedSeconds = elapsedSeconds; }
    public double getPositionX() { return positionX; }
    public void setPositionX(double positionX) { this.positionX = positionX; }
    public double getPositionY() { return positionY; }
    public void setPositionY(double positionY) { this.positionY = positionY; }
    public double getMinX() { return minX; }
    public void setMinX(double minX) { this.minX = minX; }
    public double getMaxX() { return maxX; }
    public void setMaxX(double maxX) { this.maxX = maxX; }
    public double getMinY() { return minY; }
    public void setMinY(double minY) { this.minY = minY; }
    public double getMaxY() { return maxY; }
    public void setMaxY(double maxY) { this.maxY = maxY; }
    public boolean hasNearestTarget() { return hasNearestTarget; }
    public void setHasNearestTarget(boolean hasNearestTarget) { this.hasNearestTarget = hasNearestTarget; }
    public double getNearestVectorX() { return nearestVectorX; }
    public void setNearestVectorX(double nearestVectorX) { this.nearestVectorX = nearestVectorX; }
    public double getNearestVectorY() { return nearestVectorY; }
    public void setNearestVectorY(double nearestVectorY) { this.nearestVectorY = nearestVectorY; }
    public double getNearestDistanceMeters() { return nearestDistanceMeters; }
    public void setNearestDistanceMeters(double nearestDistanceMeters) { this.nearestDistanceMeters = nearestDistanceMeters; }
    public boolean hasOcclusion() { return hasOcclusion; }
    public void setHasOcclusion(boolean hasOcclusion) { this.hasOcclusion = hasOcclusion; }
    public double getOcclusionVectorX() { return occlusionVectorX; }
    public void setOcclusionVectorX(double occlusionVectorX) { this.occlusionVectorX = occlusionVectorX; }
    public double getOcclusionVectorY() { return occlusionVectorY; }
    public void setOcclusionVectorY(double occlusionVectorY) { this.occlusionVectorY = occlusionVectorY; }
    public Map<String, Double> getObstacleRiskByAction() { return obstacleRiskByAction; }
    public double obstacleRisk(String actionId) { return obstacleRiskByAction.getOrDefault(actionId, 0.0); }
    public Map<String, Double> getLidarRiskByAction() { return lidarRiskByAction; }
    public double lidarRisk(String actionId) {
        return lidarRiskByAction.getOrDefault(actionId, obstacleRisk(actionId));
    }
    public boolean isLidarAvailable() { return lidarAvailable; }
    public void setLidarAvailable(boolean lidarAvailable) { this.lidarAvailable = lidarAvailable; }
    public double getLidarMinDistanceMeters() { return lidarMinDistanceMeters; }
    public void setLidarMinDistanceMeters(double lidarMinDistanceMeters) { this.lidarMinDistanceMeters = lidarMinDistanceMeters; }
    public double getLidarFrontMinMeters() { return lidarFrontMinMeters; }
    public void setLidarFrontMinMeters(double lidarFrontMinMeters) { this.lidarFrontMinMeters = lidarFrontMinMeters; }
    public double getLidarLeftMinMeters() { return lidarLeftMinMeters; }
    public void setLidarLeftMinMeters(double lidarLeftMinMeters) { this.lidarLeftMinMeters = lidarLeftMinMeters; }
    public double getLidarRightMinMeters() { return lidarRightMinMeters; }
    public void setLidarRightMinMeters(double lidarRightMinMeters) { this.lidarRightMinMeters = lidarRightMinMeters; }
    public double getLidarRearMinMeters() { return lidarRearMinMeters; }
    public void setLidarRearMinMeters(double lidarRearMinMeters) { this.lidarRearMinMeters = lidarRearMinMeters; }
    public int getLidarPoints() { return lidarPoints; }
    public void setLidarPoints(int lidarPoints) { this.lidarPoints = lidarPoints; }
    public int getPhotographedTargets() { return photographedTargets; }
    public void setPhotographedTargets(int photographedTargets) { this.photographedTargets = photographedTargets; }
    public int getRemainingTargets() { return remainingTargets; }
    public void setRemainingTargets(int remainingTargets) { this.remainingTargets = remainingTargets; }
    public int getTotalTargets() { return totalTargets; }
    public void setTotalTargets(int totalTargets) { this.totalTargets = Math.max(1, totalTargets); }
    public double getBaseSpeedMetersPerSecond() { return baseSpeedMetersPerSecond; }
    public void setBaseSpeedMetersPerSecond(double baseSpeedMetersPerSecond) { this.baseSpeedMetersPerSecond = baseSpeedMetersPerSecond; }
    public double getPhotoRadiusMeters() { return photoRadiusMeters; }
    public void setPhotoRadiusMeters(double photoRadiusMeters) { this.photoRadiusMeters = Math.max(1.0, photoRadiusMeters); }
    public double getCameraFootprintMeters() { return cameraFootprintMeters; }
    public void setCameraFootprintMeters(double cameraFootprintMeters) {
        this.cameraFootprintMeters = Math.max(1.0, cameraFootprintMeters);
    }
    public double getHeadingYawDegrees() { return headingYawDegrees; }
    public void setHeadingYawDegrees(double headingYawDegrees) { this.headingYawDegrees = headingYawDegrees; }

    /** Builds an observation from a decoded JSON map (used by the CARLA-Air stdio bridge). */
    @SuppressWarnings("unchecked")
    public static MovementObservationSignal fromMap(Map<String, ?> map) {
        MovementObservationSignal obs = new MovementObservationSignal();
        obs.frame = (long) num(map, "frame", 0);
        obs.elapsedSeconds = num(map, "elapsedSeconds", 0);
        Object position = map.get("positionOffset");
        if (position instanceof Map<?, ?> p) {
            obs.positionX = num((Map<String, ?>) p, "x", 0);
            obs.positionY = num((Map<String, ?>) p, "y", 0);
        }
        Object bounds = map.get("searchBounds");
        if (bounds instanceof Map<?, ?> b) {
            obs.minX = num((Map<String, ?>) b, "minX", obs.minX);
            obs.maxX = num((Map<String, ?>) b, "maxX", obs.maxX);
            obs.minY = num((Map<String, ?>) b, "minY", obs.minY);
            obs.maxY = num((Map<String, ?>) b, "maxY", obs.maxY);
        }
        Object nearest = map.get("nearestTarget");
        if (nearest instanceof Map<?, ?> n) {
            obs.hasNearestTarget = true;
            obs.nearestDistanceMeters = num((Map<String, ?>) n, "distanceMeters", 0);
            Object vector = ((Map<String, ?>) n).get("vector");
            if (vector instanceof Map<?, ?> v) {
                obs.nearestVectorX = num((Map<String, ?>) v, "x", 0);
                obs.nearestVectorY = num((Map<String, ?>) v, "y", 0);
            }
        }
        Object occlusion = map.get("occludedTarget");
        if (occlusion instanceof Map<?, ?> o) {
            obs.hasOcclusion = true;
            Object vector = ((Map<String, ?>) o).get("vector");
            if (vector instanceof Map<?, ?> v) {
                obs.occlusionVectorX = num((Map<String, ?>) v, "x", 0);
                obs.occlusionVectorY = num((Map<String, ?>) v, "y", 0);
            }
        }
        Object risk = map.get("obstacleRiskByAction");
        if (risk instanceof Map<?, ?> r) {
            for (Map.Entry<?, ?> entry : r.entrySet()) {
                if (entry.getValue() instanceof Number number) {
                    obs.obstacleRiskByAction.put(String.valueOf(entry.getKey()), number.doubleValue());
                }
            }
        }
        Object lidarRisk = map.get("lidarRiskByAction");
        if (lidarRisk instanceof Map<?, ?> r) {
            for (Map.Entry<?, ?> entry : r.entrySet()) {
                if (entry.getValue() instanceof Number number) {
                    obs.lidarRiskByAction.put(String.valueOf(entry.getKey()), number.doubleValue());
                }
            }
        }
        Object lidar = map.get("lidar");
        if (lidar instanceof Map<?, ?> l) {
            obs.lidarAvailable = bool((Map<String, ?>) l, "available", false);
            obs.lidarPoints = (int) num((Map<String, ?>) l, "points", 0);
            obs.lidarMinDistanceMeters = num((Map<String, ?>) l, "minDistanceMeters", Double.POSITIVE_INFINITY);
            obs.lidarFrontMinMeters = num((Map<String, ?>) l, "frontMinMeters", Double.POSITIVE_INFINITY);
            obs.lidarLeftMinMeters = num((Map<String, ?>) l, "leftMinMeters", Double.POSITIVE_INFINITY);
            obs.lidarRightMinMeters = num((Map<String, ?>) l, "rightMinMeters", Double.POSITIVE_INFINITY);
            obs.lidarRearMinMeters = num((Map<String, ?>) l, "rearMinMeters", Double.POSITIVE_INFINITY);
        }
        obs.photographedTargets = (int) num(map, "photographedTargets", 0);
        obs.remainingTargets = (int) num(map, "remainingTargets", 0);
        obs.setTotalTargets((int) num(map, "totalTargets", 1));
        obs.baseSpeedMetersPerSecond = num(map, "baseSpeedMetersPerSecond", obs.baseSpeedMetersPerSecond);
        obs.setPhotoRadiusMeters(num(map, "photoRadiusMeters", obs.photoRadiusMeters));
        obs.setCameraFootprintMeters(num(map, "cameraFootprintMeters", obs.cameraFootprintMeters));
        obs.headingYawDegrees = num(map, "headingYawDegrees", 0);
        obs.setMissionId(str(map, "missionId"));
        obs.setUavId(str(map, "uavId"));
        obs.setTick(obs.frame);
        return obs;
    }

    private static double num(Map<String, ?> map, String key, double fallback) {
        Object value = map.get(key);
        return value instanceof Number number ? number.doubleValue() : fallback;
    }

    private static String str(Map<String, ?> map, String key) {
        Object value = map.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private static boolean bool(Map<String, ?> map, String key, boolean fallback) {
        Object value = map.get(key);
        return value instanceof Boolean bool ? bool : fallback;
    }
}
