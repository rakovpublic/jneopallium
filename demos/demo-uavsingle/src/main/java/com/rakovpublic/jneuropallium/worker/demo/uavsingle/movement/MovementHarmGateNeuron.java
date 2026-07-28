package com.rakovpublic.jneuropallium.worker.demo.uavsingle.movement;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lower movement-layer neuron that turns fast obstacle/LiDAR/boundary features into harm vetoes.
 * Its output is consumed by action-selection neurons before reward or dopamine can influence them.
 */
public class MovementHarmGateNeuron extends ModulatableNeuron {
    private MovementHarmAssessment lastAssessment = MovementHarmAssessment.safe("none");
    private int vetoCount;
    private int assessmentCount;

    public MovementHarmGateNeuron() {
        super();
        this.currentNeuronClass = MovementHarmGateNeuron.class;
    }

    public MovementHarmAssessment assess(MovementObservationSignal obs, MovementAction action,
                                         Map<String, Double> features, MovementRuntimeConfig config) {
        assessmentCount++;
        String actionId = action.getActionId();
        List<String> conditions = new ArrayList<>();

        double obstacleClearance = features.getOrDefault("obstacle_clearance", 1.0);
        double lidarClearance = features.getOrDefault("lidar_corridor_clearance", 1.0);
        double lidarEscape = features.getOrDefault("lidar_escape_route", 0.0);
        double areaCompliance = features.getOrDefault("area_compliance", 0.0);
        double altitudeWindow = features.getOrDefault("altitude_window", 0.0);
        double directObstacleRisk = obs.obstacleRisk(actionId);
        double directLidarRisk = obs.lidarRisk(actionId);

        double obstacleRisk = Math.max(1.0 - obstacleClearance, directObstacleRisk);
        double lidarRisk = obs.isLidarAvailable()
                ? Math.max(1.0 - lidarClearance, directLidarRisk)
                : Math.max(0.0, directLidarRisk);
        double boundaryRisk = action.isReturnCenter() ? 0.0 : clamp01((-areaCompliance - 0.25) / 1.25);
        double altitudeRisk = action.getAltitudeDeltaMeters() < 0.0 ? clamp01((-altitudeWindow - 0.35) / 1.35) : 0.0;
        double stuckEscapeRelief = Math.max(0.0, lidarEscape) * 0.18;
        double risk = clamp01(Math.max(Math.max(obstacleRisk, lidarRisk),
                Math.max(boundaryRisk, altitudeRisk)) - stuckEscapeRelief);

        boolean emergencyClimb = action.getSpeedMultiplier() == 0.0
                && action.getAltitudeDeltaMeters() > 0.0
                && altitudeWindow > -0.25;
        if (lidarRisk >= 0.68 && !emergencyClimb) {
            conditions.add("LIDAR_FAST_LOOP_COLLISION_RISK");
        }
        if (obstacleRisk >= 0.74 && !emergencyClimb) {
            conditions.add("OBSTACLE_RAY_COLLISION_RISK");
        }
        if (boundaryRisk >= 0.68) {
            conditions.add("SEARCH_AREA_BOUNDARY_RISK");
        }
        if (altitudeRisk >= 0.64) {
            conditions.add("ALTITUDE_WINDOW_RISK");
        }
        boolean vetoed = !conditions.isEmpty();
        if (vetoed) {
            vetoCount++;
        }
        String reason = vetoed ? String.join("+", conditions) : "clear";
        double confidence = obs.isLidarAvailable() ? 0.94 : (obs.getObstacleRiskByAction().isEmpty() ? 0.66 : 0.82);
        lastAssessment = new MovementHarmAssessment(actionId, risk, vetoed, reason, confidence, conditions);
        setInhibitionLevel(vetoed ? Math.max(getInhibitionLevel(), risk) : Math.max(0.0, getInhibitionLevel() * 0.7));
        setChanged(vetoed);
        return lastAssessment;
    }

    public void resetEpisode() {
        lastAssessment = MovementHarmAssessment.safe("none");
        setInhibitionLevel(0.0);
        setChanged(false);
    }

    public Map<String, Object> snapshot() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("neuronId", neuronName());
        map.put("model", "fast-loop-lidar-obstacle-boundary-harm-gate");
        map.put("loop", 1);
        map.put("assessmentCount", assessmentCount);
        map.put("vetoCount", vetoCount);
        map.put("inhibitionLevel", round(getInhibitionLevel()));
        map.put("lastAssessment", lastAssessment.asMap());
        map.put("consumes", List.of(
                "ILidarObstacleSignal",
                "IObstacleAvoidanceSignal",
                "IAreaBoundarySignal",
                "IAltitudeControlSignal"));
        return map;
    }

    public String neuronName() {
        return "movement-harm-gate-lidar-fast-loop";
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static double round(double value) {
        return Math.round(value * 1_000_000.0) / 1_000_000.0;
    }
}
