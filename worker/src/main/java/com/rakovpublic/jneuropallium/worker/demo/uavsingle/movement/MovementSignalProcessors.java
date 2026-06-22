package com.rakovpublic.jneuropallium.worker.demo.uavsingle.movement;

import java.util.List;

/**
 * The heterogeneous movement signal processors. Each consumes one navigation interface and emits a
 * single bounded feature for a candidate action. Multiple processors can share an interface (the
 * area-boundary interface is consumed by both the compliance and the center-pull processors), which
 * is exactly why the action neurons depend on the processor/interface abstraction rather than on
 * concrete signal types.
 */
public final class MovementSignalProcessors {
    private MovementSignalProcessors() {
    }

    /** Canonical ordered processor set shared by every action neuron. */
    public static List<IMovementSignalProcessor> defaults() {
        return List.of(
                new Coverage(),
                new CoverageFrontier(),
                new SweepCoverage(),
                new TargetView(),
                new OcclusionEscape(),
                new ObstacleClearance(),
                new LidarCorridorClearance(),
                new LidarEscapeRoute(),
                new AreaCompliance(),
                new CenterPull(),
                new AltitudeWindow(),
                new SearchProgress(),
                new MovementEnergy(),
                new StuckEscape());
    }

    /** Pulls action selection toward the nearest cell not yet covered by the camera footprint. */
    static final class CoverageFrontier extends Base {
        CoverageFrontier() {
            super("movement-proc-coverage-frontier", "INavigationCoverageSignal", "coverage_frontier_alignment",
                    "aligns movement with the nearest still-uncovered camera-footprint cell");
        }

        @Override
        public double consume(MovementPolicyNetwork network, MovementObservationSignal obs, MovementAction action) {
            double[] frontier = network.nearestUncoveredCell(obs.getPositionX(), obs.getPositionY(), obs);
            if (frontier == null) {
                return action.getSpeedMultiplier() == 0.0 ? 0.2 : -0.1;
            }
            double distance = Math.max(1.0, frontier[2]);
            double[] vector = network.actionVector(obs, action);
            if (vector[0] == 0.0 && vector[1] == 0.0) {
                return -0.2;
            }
            return clamp((vector[0] * frontier[0] + vector[1] * frontier[1]) / distance, -1.0, 1.0);
        }
    }

    /** Lawn-mower sweep prior: run to one boundary, step one lane, then sweep back. */
    static final class SweepCoverage extends Base {
        SweepCoverage() {
            super("movement-proc-sweep-coverage", "INavigationCoverageSignal", "sweep_alignment",
                    "encourages boundary-to-boundary lawn-mower coverage of the specified area");
        }

        @Override
        public double consume(MovementPolicyNetwork network, MovementObservationSignal obs, MovementAction action) {
            double grid = Math.max(1.0, network.getConfig().getCoverageGridSizeMeters());
            double footprint = Math.max(grid, obs.getCameraFootprintMeters());
            double lane = Math.max(grid * 1.7, footprint * 1.72);
            int laneIndex = (int) Math.floor((obs.getPositionY() - obs.getMinY()) / lane);
            double margin = Math.max(grid, footprint * 0.28);
            boolean eastbound = laneIndex % 2 == 0;
            double desiredX = eastbound ? 1.0 : -1.0;
            double desiredY = 0.0;
            if ((eastbound && obs.getMaxX() - obs.getPositionX() < margin)
                    || (!eastbound && obs.getPositionX() - obs.getMinX() < margin)) {
                desiredX = 0.0;
                desiredY = obs.getPositionY() < obs.getMaxY() - margin ? 1.0 : -1.0;
            }
            double[] vector = network.actionVector(obs, action);
            if (vector[0] == 0.0 && vector[1] == 0.0) {
                return -0.2;
            }
            return clamp(vector[0] * desiredX + vector[1] * desiredY, -1.0, 1.0);
        }
    }

    private abstract static class Base implements IMovementSignalProcessor {
        private final String processorId;
        private final String interfaceName;
        private final String featureName;
        private final String description;

        Base(String processorId, String interfaceName, String featureName, String description) {
            this.processorId = processorId;
            this.interfaceName = interfaceName;
            this.featureName = featureName;
            this.description = description;
        }

        @Override public String processorId() { return processorId; }
        @Override public String interfaceName() { return interfaceName; }
        @Override public String featureName() { return featureName; }
        @Override public String description() { return description; }

        static double clamp(double value, double low, double high) {
            return Math.max(low, Math.min(high, value));
        }
    }

    /** Prefers candidate positions in grid cells the UAV has not visited yet. */
    static final class Coverage extends Base {
        Coverage() {
            super("movement-proc-coverage", "INavigationCoverageSignal", "coverage_unvisited",
                    "prefers unvisited coverage-grid cells inside the search area");
        }

        @Override
        public double consume(MovementPolicyNetwork network, MovementObservationSignal obs, MovementAction action) {
            // Sample coverage several grid cells ahead along the action heading so different
            // directions are distinguishable even though one control step moves less than one cell.
            double[] direction = network.actionVector(obs, action);
            if (direction[0] == 0.0 && direction[1] == 0.0) {
                return 1.0 / (1.0 + network.coverageVisits(obs.getPositionX(), obs.getPositionY(), obs));
            }
            double reach = Math.max(network.getConfig().getCoverageGridSizeMeters(),
                    network.speedFor(obs, action) * network.getConfig().getDecisionHorizonSeconds());
            double total = 0.0;
            for (int k = 1; k <= 3; k++) {
                double px = obs.getPositionX() + direction[0] * reach * k;
                double py = obs.getPositionY() + direction[1] * reach * k;
                total += 1.0 / (1.0 + network.coverageVisits(px, py, obs));
            }
            return clamp(total / 3.0, 0.0, 1.0);
        }
    }

    /** Turns motion toward the closest unphotographed recognized target for a better view. */
    static final class TargetView extends Base {
        TargetView() {
            super("movement-proc-target-view", "ITargetViewSignal", "target_view_alignment",
                    "aligns heading with the nearest unphotographed target and rewards close inspection");
        }

        @Override
        public double consume(MovementPolicyNetwork network, MovementObservationSignal obs, MovementAction action) {
            if (!obs.hasNearestTarget()) {
                return 0.0;
            }
            double dx = obs.getNearestVectorX();
            double dy = obs.getNearestVectorY();
            double distance = Math.max(1.0, Math.hypot(dx, dy));
            double[] vector = network.actionVector(obs, action);
            double photoRadius = Math.max(1.0, obs.getPhotoRadiusMeters());
            double heading;
            if (vector[0] == 0.0 && vector[1] == 0.0) {
                heading = action.getAltitudeDeltaMeters() < 0.0 && distance < photoRadius ? 0.2 : 0.0;
            } else {
                heading = (vector[0] * dx + vector[1] * dy) / distance;
            }
            // Keep a persistent directional pull toward the target at any range (the heading cosine
            // stays at >=0.5 strength), and add extra weight plus an inspect bonus as it closes in.
            double rangeWeight = 0.5 + 0.5 / (1.0 + distance / photoRadius);
            double inspectBonus = action.getAltitudeDeltaMeters() < 0.0 && distance < photoRadius ? 0.28 : 0.0;
            return clamp(heading * rangeWeight + inspectBonus, -1.0, 1.0);
        }
    }

    /** Side-steps or climbs after the simulator rejects a photo for line-of-sight occlusion. */
    static final class OcclusionEscape extends Base {
        OcclusionEscape() {
            super("movement-proc-occlusion", "IOcclusionAvoidanceSignal", "occlusion_escape",
                    "side-steps or climbs to clear an occluded target after a line-of-sight rejection");
        }

        @Override
        public double consume(MovementPolicyNetwork network, MovementObservationSignal obs, MovementAction action) {
            if (!obs.hasOcclusion()) {
                return 0.0;
            }
            double dx = obs.getOcclusionVectorX();
            double dy = obs.getOcclusionVectorY();
            double distance = Math.max(1.0, Math.hypot(dx, dy));
            double ux = dx / distance;
            double uy = dy / distance;
            double[] vector = network.actionVector(obs, action);
            double lateral = Math.abs(vector[0] * uy - vector[1] * ux);
            double climb = action.getAltitudeDeltaMeters() > 0.0 ? 0.6 : 0.0;
            return Math.min(1.0, lateral * 0.72 + climb);
        }
    }

    /** Uses the per-action obstacle ray-cast risk to keep clear of geometry. */
    static final class ObstacleClearance extends Base {
        ObstacleClearance() {
            super("movement-proc-obstacle", "IObstacleAvoidanceSignal", "obstacle_clearance",
                    "avoids moving into geometry using per-action obstacle ray-cast risk");
        }

        @Override
        public double consume(MovementPolicyNetwork network, MovementObservationSignal obs, MovementAction action) {
            return clamp(1.0 - obs.obstacleRisk(action.getActionId()), 0.0, 1.0);
        }
    }

    /** Consumes LiDAR corridor risk separately from the generic obstacle estimate. */
    static final class LidarCorridorClearance extends Base {
        LidarCorridorClearance() {
            super("movement-proc-lidar-corridor", "ILidarObstacleSignal", "lidar_corridor_clearance",
                    "keeps the candidate movement corridor clear according to LiDAR point/raycast risk");
        }

        @Override
        public double consume(MovementPolicyNetwork network, MovementObservationSignal obs, MovementAction action) {
            double clear = 1.0 - obs.lidarRisk(action.getActionId());
            double minDistance = obs.getLidarMinDistanceMeters();
            boolean hasDirectionalRisk = obs.getLidarRiskByAction().containsKey(action.getActionId())
                    || obs.getObstacleRiskByAction().containsKey(action.getActionId());
            if (!hasDirectionalRisk && Double.isFinite(minDistance)) {
                clear = Math.min(clear, minDistance / Math.max(1.0, network.getConfig().getObstacleRayMeters()));
            }
            return clamp(clear, 0.0, 1.0);
        }
    }

    /** Rewards the safest lateral/free corridor when at least one LiDAR action ray is blocked. */
    static final class LidarEscapeRoute extends Base {
        LidarEscapeRoute() {
            super("movement-proc-lidar-escape-route", "ILidarObstacleSignal", "lidar_escape_route",
                    "selects the lowest-risk corridor when obstacle rays report blocked movement");
        }

        @Override
        public double consume(MovementPolicyNetwork network, MovementObservationSignal obs, MovementAction action) {
            if (obs.getLidarRiskByAction().isEmpty() && obs.getObstacleRiskByAction().isEmpty()) {
                return 0.0;
            }
            double risk = obs.lidarRisk(action.getActionId());
            double maxRisk = 0.0;
            double totalRisk = 0.0;
            int count = 0;
            for (double value : obs.getLidarRiskByAction().values()) {
                maxRisk = Math.max(maxRisk, value);
                totalRisk += value;
                count++;
            }
            if (count == 0) {
                for (double value : obs.getObstacleRiskByAction().values()) {
                    maxRisk = Math.max(maxRisk, value);
                    totalRisk += value;
                    count++;
                }
            }
            if (count == 0 || maxRisk < 0.25) {
                return 0.0;
            }
            double averageRisk = totalRisk / count;
            double clear = 1.0 - risk;
            double relative = averageRisk - risk;
            double climb = action.getAltitudeDeltaMeters() > 0.0 && action.getSpeedMultiplier() == 0.0 ? 0.35 : 0.0;
            return clamp(relative + clear * 0.45 + climb, -1.0, 1.0);
        }
    }

    /** Keeps the drone inside the configured search polygon. */
    static final class AreaCompliance extends Base {
        AreaCompliance() {
            super("movement-proc-boundary", "IAreaBoundarySignal", "area_compliance",
                    "keeps the predicted position inside the configured search area");
        }

        @Override
        public double consume(MovementPolicyNetwork network, MovementObservationSignal obs, MovementAction action) {
            double[] predicted = network.predictedOffset(obs, action);
            if (!network.insideBounds(predicted[0], predicted[1], obs)) {
                return -1.0;
            }
            // Neutral (0) when safely inside so it does not bias action selection; only ramps
            // negative as the predicted position approaches the boundary.
            double edge = network.distanceToBoundsEdge(predicted[0], predicted[1], obs);
            double margin = Math.max(1.0, network.getConfig().getBoundaryMarginMeters());
            if (edge >= margin) {
                return 0.0;
            }
            return clamp(edge / margin - 1.0, -1.0, 0.0);
        }
    }

    /** Returns toward the area center when near or outside a boundary. */
    static final class CenterPull extends Base {
        CenterPull() {
            super("movement-proc-center-pull", "IAreaBoundarySignal", "center_pull",
                    "pulls back toward the area center near or outside a boundary");
        }

        @Override
        public double consume(MovementPolicyNetwork network, MovementObservationSignal obs, MovementAction action) {
            double margin = network.distanceToBoundsEdge(obs.getPositionX(), obs.getPositionY(), obs);
            boolean inside = network.insideBounds(obs.getPositionX(), obs.getPositionY(), obs);
            if (inside && margin > network.getConfig().getBoundaryMarginMeters() * 2.0) {
                return 0.0;
            }
            double cx = (obs.getMinX() + obs.getMaxX()) / 2.0 - obs.getPositionX();
            double cy = (obs.getMinY() + obs.getMaxY()) / 2.0 - obs.getPositionY();
            double distance = Math.max(1.0, Math.hypot(cx, cy));
            double[] vector = network.actionVector(obs, action);
            if (vector[0] == 0.0 && vector[1] == 0.0) {
                return 0.0;
            }
            return clamp((vector[0] * cx + vector[1] * cy) / distance, -1.0, 1.0);
        }
    }

    /** Chooses altitude changes for inspection and occlusion recovery inside the altitude band. */
    static final class AltitudeWindow extends Base {
        AltitudeWindow() {
            super("movement-proc-altitude", "IAltitudeControlSignal", "altitude_window",
                    "keeps altitude inside the band and biases climbs for occlusion, descents for inspection");
        }

        @Override
        public double consume(MovementPolicyNetwork network, MovementObservationSignal obs, MovementAction action) {
            double next = network.clampAltitude(network.getCurrentAltitude() + action.getAltitudeDeltaMeters());
            MovementRuntimeConfig config = network.getConfig();
            double base = next >= config.getMinAltitudeMeters() && next <= config.getMaxAltitudeMeters() ? 0.75 : -1.0;
            if (network.hasPendingOcclusion() && action.getAltitudeDeltaMeters() > 0.0) {
                base += 0.2;
            }
            if (action.getAltitudeDeltaMeters() < 0.0) {
                base += 0.08;
            }
            return clamp(base, -1.0, 1.0);
        }
    }

    /** Balances fast area coverage with slow close inspection of a nearby target. */
    static final class SearchProgress extends Base {
        SearchProgress() {
            super("movement-proc-progress", "ISearchProgressSignal", "search_progress",
                    "balances fast survey speed with slow close inspection near a target");
        }

        @Override
        public double consume(MovementPolicyNetwork network, MovementObservationSignal obs, MovementAction action) {
            double remainingRatio = obs.getRemainingTargets() / (double) Math.max(1, obs.getTotalTargets());
            if (obs.hasNearestTarget() && obs.getNearestDistanceMeters() < obs.getPhotoRadiusMeters()
                    && action.getSpeedMultiplier() <= 1.0) {
                return 0.9;
            }
            return clamp(action.getSpeedMultiplier() * remainingRatio / 1.7, 0.0, 1.0);
        }
    }

    /** Rewards using the available speed envelope so the UAV actually covers ground. */
    static final class MovementEnergy extends Base {
        MovementEnergy() {
            super("movement-proc-energy", "IMovementEnergySignal", "movement_energy",
                    "rewards using the available speed envelope to cover ground");
        }

        @Override
        public double consume(MovementPolicyNetwork network, MovementObservationSignal obs, MovementAction action) {
            double speed = network.speedFor(obs, action);
            return clamp(speed / Math.max(1.0, network.getConfig().getMaxSpeedMetersPerSecond()), 0.0, 1.0);
        }
    }

    /** Breaks out of a stall when recent displacement has been below the active threshold. */
    static final class StuckEscape extends Base {
        StuckEscape() {
            super("movement-proc-stuck-escape", "IMovementEnergySignal", "stuck_escape",
                    "favours faster actions when recent displacement stalls");
        }

        @Override
        public double consume(MovementPolicyNetwork network, MovementObservationSignal obs, MovementAction action) {
            if (network.recentDisplacementSamples() < 2) {
                return 0.0;
            }
            double mean = network.meanRecentDisplacement();
            if (mean >= network.getConfig().getMinimumActiveDisplacementMeters()) {
                return 0.0;
            }
            return clamp(action.getSpeedMultiplier() / 1.8, 0.0, 1.0);
        }
    }
}
