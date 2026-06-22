package com.rakovpublic.jneuropallium.worker.demo.uavsingle.movement;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tunable runtime parameters for the movement policy. Field names mirror the {@code runtime}
 * block of the exported model JSON so the CARLA-Air bridge can override them per run (control
 * period, speed/altitude window, coverage grid, exploration, learning rate, ...).
 */
public final class MovementRuntimeConfig {
    private double learningRate = 0.07;
    private double explorationSigma = 0.03;
    private double commandHoldSeconds = 3.0;
    private double minSpeedMetersPerSecond = 8.0;
    private double maxSpeedMetersPerSecond = 24.0;
    private double minAltitudeMeters = 16.0;
    private double maxAltitudeMeters = 48.0;
    private double coverageGridSizeMeters = 30.0;
    private double boundaryMarginMeters = 10.0;
    private double decisionHorizonSeconds = 3.0;
    private double initialAltitudeMeters = 24.0;
    private double minimumActiveDisplacementMeters = 1.0;
    private double obstacleRayMeters = 35.0;
    private long deterministicSeed = 17L;

    public static MovementRuntimeConfig defaults() {
        return new MovementRuntimeConfig();
    }

    /** Tuning that matches the live CARLA-Air / AirSim multirotor control loop. */
    public static MovementRuntimeConfig carlaAirLive() {
        MovementRuntimeConfig config = new MovementRuntimeConfig();
        config.commandHoldSeconds = 0.6;
        config.decisionHorizonSeconds = 0.6;
        config.minSpeedMetersPerSecond = 6.0;
        config.maxSpeedMetersPerSecond = 18.0;
        config.minAltitudeMeters = 28.0;
        config.maxAltitudeMeters = 90.0;
        config.coverageGridSizeMeters = 35.0;
        config.boundaryMarginMeters = 8.0;
        config.initialAltitudeMeters = 70.0;
        config.learningRate = 0.025;
        config.explorationSigma = 0.05;
        config.obstacleRayMeters = 70.0;
        config.minimumActiveDisplacementMeters = 0.25;
        return config;
    }

    public MovementRuntimeConfig applyOverrides(Map<String, ?> overrides) {
        if (overrides == null) {
            return this;
        }
        learningRate = doubleOrDefault(overrides, "learningRate", learningRate);
        explorationSigma = doubleOrDefault(overrides, "explorationSigma", explorationSigma);
        commandHoldSeconds = doubleOrDefault(overrides, "commandHoldSeconds", commandHoldSeconds);
        minSpeedMetersPerSecond = doubleOrDefault(overrides, "minSpeedMetersPerSecond", minSpeedMetersPerSecond);
        maxSpeedMetersPerSecond = doubleOrDefault(overrides, "maxSpeedMetersPerSecond", maxSpeedMetersPerSecond);
        minAltitudeMeters = doubleOrDefault(overrides, "minAltitudeMeters", minAltitudeMeters);
        maxAltitudeMeters = doubleOrDefault(overrides, "maxAltitudeMeters", maxAltitudeMeters);
        coverageGridSizeMeters = doubleOrDefault(overrides, "coverageGridSizeMeters", coverageGridSizeMeters);
        boundaryMarginMeters = doubleOrDefault(overrides, "boundaryMarginMeters", boundaryMarginMeters);
        decisionHorizonSeconds = doubleOrDefault(overrides, "decisionHorizonSeconds", decisionHorizonSeconds);
        initialAltitudeMeters = doubleOrDefault(overrides, "initialAltitudeMeters", initialAltitudeMeters);
        minimumActiveDisplacementMeters =
                doubleOrDefault(overrides, "minimumActiveDisplacementMeters", minimumActiveDisplacementMeters);
        obstacleRayMeters = doubleOrDefault(overrides, "obstacleRayMeters", obstacleRayMeters);
        deterministicSeed = (long) doubleOrDefault(overrides, "deterministicSeed", deterministicSeed);
        return this;
    }

    private static double doubleOrDefault(Map<String, ?> map, String key, double fallback) {
        Object value = map.get(key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Double.parseDouble(text.trim());
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    public Map<String, Object> asRuntimeMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("learningRate", learningRate);
        map.put("explorationSigma", explorationSigma);
        map.put("commandHoldSeconds", commandHoldSeconds);
        map.put("minSpeedMetersPerSecond", minSpeedMetersPerSecond);
        map.put("maxSpeedMetersPerSecond", maxSpeedMetersPerSecond);
        map.put("minAltitudeMeters", minAltitudeMeters);
        map.put("maxAltitudeMeters", maxAltitudeMeters);
        map.put("coverageGridSizeMeters", coverageGridSizeMeters);
        map.put("boundaryMarginMeters", boundaryMarginMeters);
        map.put("decisionHorizonSeconds", decisionHorizonSeconds);
        map.put("initialAltitudeMeters", initialAltitudeMeters);
        map.put("minimumActiveDisplacementMeters", minimumActiveDisplacementMeters);
        map.put("obstacleRayMeters", obstacleRayMeters);
        map.put("deterministicSeed", deterministicSeed);
        return map;
    }

    public double getLearningRate() { return learningRate; }
    public void setExplorationSigma(double explorationSigma) { this.explorationSigma = explorationSigma; }
    public double getExplorationSigma() { return explorationSigma; }
    public double getCommandHoldSeconds() { return commandHoldSeconds; }
    public double getMinSpeedMetersPerSecond() { return minSpeedMetersPerSecond; }
    public double getMaxSpeedMetersPerSecond() { return maxSpeedMetersPerSecond; }
    public double getMinAltitudeMeters() { return minAltitudeMeters; }
    public double getMaxAltitudeMeters() { return maxAltitudeMeters; }
    public double getCoverageGridSizeMeters() { return coverageGridSizeMeters; }
    public double getBoundaryMarginMeters() { return boundaryMarginMeters; }
    public double getDecisionHorizonSeconds() { return decisionHorizonSeconds; }
    public double getInitialAltitudeMeters() { return initialAltitudeMeters; }
    public double getMinimumActiveDisplacementMeters() { return minimumActiveDisplacementMeters; }
    public double getObstacleRayMeters() { return obstacleRayMeters; }
    public long getDeterministicSeed() { return deterministicSeed; }
}
