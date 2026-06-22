package com.rakovpublic.jneuropallium.worker.demo.uavsingle.movement;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeSet;

/**
 * Movement neuron layer facade: routes simulator observations through heterogeneous processors,
 * lower fast-loop harm neurons and temporal action-selection neurons.
 *
 * <p>The policy state lives in neurons. This class keeps the existing runner/bridge API stable,
 * tracks episode context for processor features, and exports the movement layer model.
 */
public class MovementPolicyNetwork {
    public static final String POLICY_VERSION = "movement-temporal-action-neurons-v2";
    public static final String PROCESSOR_CONTRACT =
            "heterogeneous processors consume typed navigation interfaces; lower harm neurons convert "
                    + "lidar/obstacle/boundary signals into fast veto inputs; temporal action neurons "
                    + "own action value, eligibility, ACh/DA gates and movement selection";

    private final MovementRuntimeConfig config;
    private final List<IMovementSignalProcessor> processors;
    private final List<MovementActionNeuron> neurons;
    private final MovementHarmGateNeuron harmGateNeuron;
    private final Map<Long, Integer> coverageVisits = new HashMap<>();
    private final Deque<Double> recentDisplacements = new ArrayDeque<>();
    private final List<Long> pendingOcclusions = new ArrayList<>();
    private final Random rng;

    private double currentAltitude;
    private LastDecision lastDecision;
    private double[] lastPosition;
    private int lastPhotographedCount;
    private Double lastNearestDistance;
    private int decisionsMade;
    private int reinforcementUpdates;
    private boolean autoReinforce = true;

    public MovementPolicyNetwork() {
        this(MovementRuntimeConfig.defaults());
    }

    public MovementPolicyNetwork(MovementRuntimeConfig config) {
        this.config = config == null ? MovementRuntimeConfig.defaults() : config;
        this.processors = MovementSignalProcessors.defaults();
        this.rng = new Random(this.config.getDeterministicSeed());
        this.currentAltitude = clampAltitude(this.config.getInitialAltitudeMeters());
        this.harmGateNeuron = new MovementHarmGateNeuron();
        this.neurons = buildNeurons();
    }

    public MovementRuntimeConfig getConfig() { return config; }
    public List<IMovementSignalProcessor> getProcessors() { return processors; }
    public List<MovementActionNeuron> getNeurons() { return neurons; }
    public MovementHarmGateNeuron getHarmGateNeuron() { return harmGateNeuron; }
    public double getCurrentAltitude() { return currentAltitude; }
    public int getDecisionsMade() { return decisionsMade; }
    public int getReinforcementUpdates() { return reinforcementUpdates; }
    public int getCoverageCellsVisited() { return coverageVisits.size(); }
    public boolean hasPendingOcclusion() { return !pendingOcclusions.isEmpty(); }
    public int recentDisplacementSamples() { return recentDisplacements.size(); }

    public double meanRecentDisplacement() {
        if (recentDisplacements.isEmpty()) {
            return 0.0;
        }
        double sum = 0.0;
        for (double value : recentDisplacements) {
            sum += value;
        }
        return sum / recentDisplacements.size();
    }

    // ------------------------------------------------------------------ decisions

    /** Selects the next action for the given observation and reinforces the previous decision. */
    public DecisionOutcome decide(MovementObservationSignal obs) {
        pruneOcclusions(obs.getFrame());
        markCameraFootprint(obs.getPositionX(), obs.getPositionY(), obs.getCameraFootprintMeters(), obs);
        MovementLearningResultSignal autoReinforcement = autoReinforce ? reinforceFromObservation(obs) : null;

        List<Candidate> candidates = new ArrayList<>();
        for (MovementActionNeuron neuron : neurons) {
            Map<String, Double> features = computeFeatures(obs, neuron.getAction());
            MovementHarmAssessment harm = harmGateNeuron.assess(obs, neuron.getAction(), features, config);
            double score = neuron.activate(features, harm, config, obs.getFrame());
            if (!harm.isVetoed() && config.getExplorationSigma() > 0.0) {
                score += rng.nextGaussian() * config.getExplorationSigma();
            }
            candidates.add(new Candidate(neuron, features, score, harm));
        }

        Candidate chosen = candidates.get(0);
        for (Candidate candidate : candidates) {
            if (candidate.score > chosen.score) {
                chosen = candidate;
            }
        }
        chosen.neuron.markSelected(obs.getFrame());

        MotorCommand command = buildCommand(obs, chosen.neuron.getAction());
        decisionsMade++;
        lastNearestDistance = obs.hasNearestTarget() ? obs.getNearestDistanceMeters() : null;

        MovementDecisionSignal decision = new MovementDecisionSignal();
        decision.setMissionId(obs.getMissionId());
        decision.setUavId(obs.getUavId());
        decision.setTick(obs.getFrame());
        decision.setFrame(obs.getFrame());
        decision.setElapsedSeconds(obs.getElapsedSeconds());
        decision.setNeuronId(chosen.neuron.neuronName());
        decision.setActionId(chosen.neuron.getAction().getActionId());
        decision.setActionLabel(chosen.neuron.getAction().getLabel());
        decision.setScore(chosen.score);
        decision.getFeatures().putAll(chosen.features);
        decision.getHarmAssessment().putAll(chosen.harm.asMap());
        decision.setCommand(command);
        decision.getTopNeuronScores().addAll(topNeuronScores(candidates));

        lastDecision = new LastDecision(chosen.neuron, chosen.features, obs.getFrame(), chosen.harm);
        return new DecisionOutcome(decision, autoReinforcement);
    }

    private Map<String, Double> computeFeatures(MovementObservationSignal obs, MovementAction action) {
        Map<String, Double> features = new LinkedHashMap<>();
        for (IMovementSignalProcessor processor : processors) {
            features.put(processor.featureName(), processor.consume(this, obs, action));
        }
        return features;
    }

    private List<Map<String, Object>> topNeuronScores(List<Candidate> candidates) {
        List<Candidate> sorted = new ArrayList<>(candidates);
        sorted.sort(Comparator.comparingDouble((Candidate candidate) -> candidate.score).reversed());
        List<Map<String, Object>> top = new ArrayList<>();
        for (int i = 0; i < Math.min(5, sorted.size()); i++) {
            Candidate candidate = sorted.get(i);
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("neuronId", candidate.neuron.neuronName());
            entry.put("actionId", candidate.neuron.getAction().getActionId());
            entry.put("score", round(candidate.score));
            entry.put("harmRisk", round(candidate.harm.getRisk()));
            entry.put("vetoed", candidate.harm.isVetoed());
            entry.put("vetoReason", candidate.harm.getReason());
            entry.put("fastEvidence", round(candidate.neuron.getFastEvidence()));
            entry.put("eligibilityTrace", round(candidate.neuron.getEligibilityTrace()));
            top.add(entry);
        }
        return top;
    }

    // ------------------------------------------------------------------ reinforcement

    private MovementLearningResultSignal reinforceFromObservation(MovementObservationSignal obs) {
        double[] position = {obs.getPositionX(), obs.getPositionY()};
        if (lastDecision == null) {
            lastPhotographedCount = obs.getPhotographedTargets();
            lastPosition = position;
            return null;
        }

        double displacement = 0.0;
        if (lastPosition != null) {
            displacement = Math.hypot(position[0] - lastPosition[0], position[1] - lastPosition[1]);
            recentDisplacements.addLast(displacement);
            while (recentDisplacements.size() > 8) {
                recentDisplacements.removeFirst();
            }
        }
        lastPosition = position;

        int photographed = obs.getPhotographedTargets();
        int newPhotos = Math.max(0, photographed - lastPhotographedCount);
        lastPhotographedCount = photographed;

        long cell = gridCell(position[0], position[1], obs);
        int newCoverage = coverageVisits.getOrDefault(cell, 0) <= 1 ? 1 : 0;
        boolean inside = insideBounds(position[0], position[1], obs);
        double insidePenalty = inside ? 0.0 : -0.55;
        Double currentNearest = obs.hasNearestTarget() ? obs.getNearestDistanceMeters() : null;
        double distanceReward = 0.0;
        if (lastNearestDistance != null && currentNearest != null) {
            distanceReward = clamp((lastNearestDistance - currentNearest) / 70.0, -0.3, 0.45);
        }
        double inactivityPenalty = displacement < config.getMinimumActiveDisplacementMeters() ? -0.22 : 0.08;
        int recentOcclusions = 0;
        for (long frame : pendingOcclusions) {
            if (frame >= lastDecision.frame) {
                recentOcclusions++;
            }
        }
        double reward = 1.7 * newPhotos
                + 0.16 * newCoverage
                + distanceReward
                + insidePenalty
                + inactivityPenalty
                - 0.15 * recentOcclusions;

        Map<String, Object> extras = new LinkedHashMap<>();
        extras.put("newPhotos", newPhotos);
        extras.put("newCoverageCells", newCoverage);
        extras.put("recentOcclusionSignals", recentOcclusions);
        extras.put("insideAreaPenalty", round(insidePenalty));
        extras.put("distanceReward", round(distanceReward));
        extras.put("activeDisplacementMeters", round(displacement));
        extras.put("inactivityPenalty", round(inactivityPenalty));
        return applyReward(reward, obs.getFrame(), "SIMULATOR_REWARD_TO_JNEOPALLIUM_MOVEMENT_LAYER", extras);
    }

    /** Reinforces the last decision with an externally supplied reward (occlusion, missed target). */
    public MovementLearningResultSignal reinforceEvent(double reward, long frame, String reason,
                                                       Map<String, Object> extras) {
        return applyReward(reward, frame, reason, extras == null ? new LinkedHashMap<>() : extras);
    }

    private MovementLearningResultSignal applyReward(double reward, long frame, String reason,
                                                     Map<String, Object> extras) {
        MovementLearningResultSignal result = new MovementLearningResultSignal();
        result.setFrame(frame);
        result.setReward(reward);
        result.setReason(reason);
        if (lastDecision == null) {
            result.setApplied(false);
            result.setReason("NO_PRIOR_DECISION");
            return result;
        }
        Map<String, Object> learningExtras = new LinkedHashMap<>(extras);
        learningExtras.put("lastHarmAssessment", lastDecision.harm.asMap());
        Map<String, Double> deltas = lastDecision.neuron.reinforce(lastDecision.features, reward,
                config.getLearningRate(), frame, reason, learningExtras);
        reinforcementUpdates++;
        result.setApplied(true);
        result.setReinforcedNeuronId(lastDecision.neuron.neuronName());
        result.setActionId(lastDecision.neuron.getAction().getActionId());
        result.setUpdatedBias(round(lastDecision.neuron.getBias()));
        result.getUpdatedDendrites().putAll(deltas);
        result.getExtras().putAll(learningExtras);
        result.getExtras().put("learnedActionValue", round(lastDecision.neuron.getLearnedActionValue()));
        result.getExtras().put("eligibilityTrace", round(lastDecision.neuron.getEligibilityTrace()));
        return result;
    }

    public void recordOcclusion(long frame) {
        pendingOcclusions.add(frame);
    }

    private void pruneOcclusions(long frame) {
        pendingOcclusions.removeIf(item -> frame - item > 96);
    }

    /** When false, {@link #decide} does not self-reinforce — the coverage trainer owns the reward. */
    public void setAutoReinforce(boolean autoReinforce) {
        this.autoReinforce = autoReinforce;
    }

    /** Reset the per-episode trajectory state (coverage, trail, altitude). Weights are kept. */
    public void resetEpisode() {
        coverageVisits.clear();
        recentDisplacements.clear();
        pendingOcclusions.clear();
        lastDecision = null;
        lastPosition = null;
        lastPhotographedCount = 0;
        lastNearestDistance = null;
        currentAltitude = clampAltitude(config.getInitialAltitudeMeters());
        harmGateNeuron.resetEpisode();
        for (MovementActionNeuron neuron : neurons) {
            neuron.resetEpisodeState();
        }
    }

    /**
     * Mark every coverage-grid cell whose centre is within the camera footprint radius of
     * {@code (centerX, centerY)} as covered. Returns the running count of distinct covered cells —
     * the "camera saw all of the specified area" signal used by the coverage trainer.
     */
    public int markCameraFootprint(double centerX, double centerY, double radius, MovementObservationSignal obs) {
        double grid = Math.max(1.0, config.getCoverageGridSizeMeters());
        int span = (int) Math.ceil(radius / grid);
        long baseGx = (long) Math.floor((centerX - obs.getMinX()) / grid);
        long baseGy = (long) Math.floor((centerY - obs.getMinY()) / grid);
        for (long dgx = -span; dgx <= span; dgx++) {
            for (long dgy = -span; dgy <= span; dgy++) {
                double cx = obs.getMinX() + (baseGx + dgx + 0.5) * grid;
                double cy = obs.getMinY() + (baseGy + dgy + 0.5) * grid;
                if (cx < obs.getMinX() || cx > obs.getMaxX() || cy < obs.getMinY() || cy > obs.getMaxY()) {
                    continue;
                }
                if (Math.hypot(cx - centerX, cy - centerY) <= radius) {
                    coverageVisits.merge(gridCell(cx, cy, obs), 1, Integer::sum);
                }
            }
        }
        return coverageVisits.size();
    }

    /**
     * Vector {@code {dx, dy, distance}} from {@code (x, y)} to the centre of the nearest coverage
     * cell that has not been covered yet, or {@code null} when every cell is covered. The coverage
     * trainer feeds this as the pseudo-target so the policy heads to the nearest uncovered frontier
     * instead of getting stuck once its local neighbourhood is covered.
     */
    public double[] nearestUncoveredCell(double x, double y, MovementObservationSignal obs) {
        double grid = Math.max(1.0, config.getCoverageGridSizeMeters());
        int cols = (int) Math.ceil((obs.getMaxX() - obs.getMinX()) / grid);
        int rows = (int) Math.ceil((obs.getMaxY() - obs.getMinY()) / grid);
        double bestDistance = Double.MAX_VALUE;
        double[] best = null;
        for (int gi = 0; gi < cols; gi++) {
            for (int gj = 0; gj < rows; gj++) {
                double cx = obs.getMinX() + (gi + 0.5) * grid;
                double cy = obs.getMinY() + (gj + 0.5) * grid;
                if (cx > obs.getMaxX() || cy > obs.getMaxY()) {
                    continue;
                }
                if (coverageVisits.getOrDefault(gridCell(cx, cy, obs), 0) == 0) {
                    double distance = Math.hypot(cx - x, cy - y);
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        best = new double[] {cx - x, cy - y, distance};
                    }
                }
            }
        }
        return best;
    }

    // ------------------------------------------------------------------ geometry helpers

    public double[] actionVector(MovementObservationSignal obs, MovementAction action) {
        double dx;
        double dy;
        if (action.isReturnCenter()) {
            dx = (obs.getMinX() + obs.getMaxX()) / 2.0 - obs.getPositionX();
            dy = (obs.getMinY() + obs.getMaxY()) / 2.0 - obs.getPositionY();
        } else {
            dx = action.getDirectionX();
            dy = action.getDirectionY();
        }
        double length = Math.hypot(dx, dy);
        if (length <= 0.001) {
            return new double[] {0.0, 0.0};
        }
        return new double[] {dx / length, dy / length};
    }

    public double speedFor(MovementObservationSignal obs, MovementAction action) {
        double base = obs.getBaseSpeedMetersPerSecond() > 0.0
                ? obs.getBaseSpeedMetersPerSecond() : config.getMinSpeedMetersPerSecond();
        return Math.max(config.getMinSpeedMetersPerSecond(),
                Math.min(config.getMaxSpeedMetersPerSecond(), base * action.getSpeedMultiplier()));
    }

    public double[] predictedOffset(MovementObservationSignal obs, MovementAction action) {
        double[] vector = actionVector(obs, action);
        double distance = speedFor(obs, action) * config.getDecisionHorizonSeconds();
        return new double[] {obs.getPositionX() + vector[0] * distance, obs.getPositionY() + vector[1] * distance};
    }

    private MotorCommand buildCommand(MovementObservationSignal obs, MovementAction action) {
        double[] vector = actionVector(obs, action);
        double speed = speedFor(obs, action);
        double targetAltitude = clampAltitude(currentAltitude + action.getAltitudeDeltaMeters());
        currentAltitude = targetAltitude;
        double yaw = (vector[0] == 0.0 && vector[1] == 0.0)
                ? obs.getHeadingYawDegrees()
                : Math.toDegrees(Math.atan2(vector[1], vector[0]));
        return new MotorCommand(action.getActionId(), vector[0] * speed, vector[1] * speed,
                targetAltitude, config.getCommandHoldSeconds(), yaw);
    }

    private void markCoverage(double x, double y, MovementObservationSignal obs) {
        long cell = gridCell(x, y, obs);
        coverageVisits.merge(cell, 1, Integer::sum);
    }

    public int coverageVisits(double x, double y, MovementObservationSignal obs) {
        return coverageVisits.getOrDefault(gridCell(x, y, obs), 0);
    }

    private long gridCell(double x, double y, MovementObservationSignal obs) {
        long gx = (long) Math.floor((x - obs.getMinX()) / Math.max(1.0, config.getCoverageGridSizeMeters()));
        long gy = (long) Math.floor((y - obs.getMinY()) / Math.max(1.0, config.getCoverageGridSizeMeters()));
        return (gx << 32) ^ (gy & 0xffffffffL);
    }

    public boolean insideBounds(double x, double y, MovementObservationSignal obs) {
        return x >= obs.getMinX() && x <= obs.getMaxX() && y >= obs.getMinY() && y <= obs.getMaxY();
    }

    public double distanceToBoundsEdge(double x, double y, MovementObservationSignal obs) {
        if (!insideBounds(x, y, obs)) {
            double dx = Math.max(Math.max(obs.getMinX() - x, 0.0), x - obs.getMaxX());
            double dy = Math.max(Math.max(obs.getMinY() - y, 0.0), y - obs.getMaxY());
            return -Math.hypot(dx, dy);
        }
        return Math.min(Math.min(x - obs.getMinX(), obs.getMaxX() - x),
                Math.min(y - obs.getMinY(), obs.getMaxY() - y));
    }

    public double clampAltitude(double altitude) {
        return Math.max(config.getMinAltitudeMeters(), Math.min(config.getMaxAltitudeMeters(), Math.abs(altitude)));
    }

    // ------------------------------------------------------------------ model build / IO

    private List<MovementActionNeuron> buildNeurons() {
        List<MovementActionNeuron> built = new ArrayList<>();
        for (MovementAction action : defaultActions()) {
            double bias = action.getSpeedMultiplier() == 0.0 && action.getAltitudeDeltaMeters() == 0.0 ? -0.12 : 0.0;
            built.add(new MovementActionNeuron(action, initialWeights(action), bias));
        }
        return built;
    }

    private static List<MovementAction> defaultActions() {
        List<MovementAction> actions = new ArrayList<>();
        actions.add(new MovementAction("survey-east", "survey east", 1.0, 0.0, 0.0, 1.0));
        actions.add(new MovementAction("survey-west", "survey west", -1.0, 0.0, 0.0, 1.0));
        actions.add(new MovementAction("survey-north", "survey north", 0.0, 1.0, 0.0, 1.0));
        actions.add(new MovementAction("survey-south", "survey south", 0.0, -1.0, 0.0, 1.0));
        actions.add(new MovementAction("survey-north-east", "survey north east", 0.707, 0.707, 0.0, 1.0));
        actions.add(new MovementAction("survey-north-west", "survey north west", -0.707, 0.707, 0.0, 1.0));
        actions.add(new MovementAction("survey-south-east", "survey south east", 0.707, -0.707, 0.0, 1.0));
        actions.add(new MovementAction("survey-south-west", "survey south west", -0.707, -0.707, 0.0, 1.0));
        actions.add(new MovementAction("inspect-east", "slow inspect east", 1.0, 0.0, -1.6, 0.55));
        actions.add(new MovementAction("inspect-west", "slow inspect west", -1.0, 0.0, -1.6, 0.55));
        actions.add(new MovementAction("inspect-north", "slow inspect north", 0.0, 1.0, -1.6, 0.55));
        actions.add(new MovementAction("inspect-south", "slow inspect south", 0.0, -1.0, -1.6, 0.55));
        actions.add(new MovementAction("climb-east", "climb east", 1.0, 0.0, 2.4, 0.7));
        actions.add(new MovementAction("climb-west", "climb west", -1.0, 0.0, 2.4, 0.7));
        actions.add(new MovementAction("climb-north", "climb north", 0.0, 1.0, 2.4, 0.7));
        actions.add(new MovementAction("climb-south", "climb south", 0.0, -1.0, 2.4, 0.7));
        actions.add(new MovementAction("return-to-area", "return to search area", 0.0, 0.0, 0.0, 0.85,
                MovementAction.BEHAVIOR_RETURN_CENTER));
        actions.add(new MovementAction("hold-climb", "hold and climb", 0.0, 0.0, 3.0, 0.0));
        actions.add(new MovementAction("hold-descend", "hold and descend", 0.0, 0.0, -2.5, 0.0));
        actions.add(new MovementAction("hover-scan", "hover scan", 0.0, 0.0, 0.0, 0.0));
        return actions;
    }

    private static Map<String, Double> initialWeights(MovementAction action) {
        Map<String, Double> weights = new LinkedHashMap<>();
        weights.put("coverage_unvisited", 0.9);
        weights.put("coverage_frontier_alignment", 2.4);
        weights.put("sweep_alignment", 1.05);
        weights.put("target_view_alignment", 0.75);
        weights.put("occlusion_escape", 0.5);
        weights.put("obstacle_clearance", 1.15);
        weights.put("lidar_corridor_clearance", 1.4);
        weights.put("lidar_escape_route", 1.2);
        weights.put("area_compliance", 1.35);
        weights.put("center_pull", 0.45);
        weights.put("altitude_window", 0.5);
        weights.put("search_progress", 0.45);
        weights.put("movement_energy", 0.3);
        weights.put("stuck_escape", 0.3);
        if (action.isReturnCenter()) {
            weights.put("area_compliance", 2.1);
            weights.put("center_pull", 1.6);
            weights.put("coverage_frontier_alignment", 0.2);
            weights.put("target_view_alignment", 0.2);
            weights.put("sweep_alignment", 0.1);
            weights.put("lidar_escape_route", 1.6);
        }
        if (action.getAltitudeDeltaMeters() > 0.0) {
            weights.put("occlusion_escape", 1.15);
            weights.put("altitude_window", 0.85);
            weights.put("lidar_escape_route", 1.5);
        }
        if (action.getAltitudeDeltaMeters() < 0.0) {
            weights.put("target_view_alignment", 0.95);
            weights.put("search_progress", 0.7);
        }
        if (action.getSpeedMultiplier() == 0.0) {
            weights.put("coverage_unvisited", 0.1);
            weights.put("coverage_frontier_alignment", -0.1);
            weights.put("sweep_alignment", 0.0);
            weights.put("search_progress", 0.1);
            weights.put("movement_energy", 0.05);
        }
        return weights;
    }

    /** Warm-starts trained weights from a previously exported {@code layers.movementPolicy} block. */
    @SuppressWarnings("unchecked")
    public int loadWeights(Map<String, ?> modelMap) {
        Object runtime = modelMap.get("runtime");
        if (runtime instanceof Map<?, ?> runtimeMap) {
            config.applyOverrides((Map<String, ?>) runtimeMap);
            currentAltitude = clampAltitude(config.getInitialAltitudeMeters());
        }
        Object layers = modelMap.get("layers");
        if (!(layers instanceof Map<?, ?> layerMap)) {
            return 0;
        }
        Object movementPolicy = ((Map<String, ?>) layerMap).get("movementPolicy");
        if (!(movementPolicy instanceof Map<?, ?> policy)) {
            return 0;
        }
        Object neuronList = ((Map<String, ?>) policy).get("neurons");
        if (!(neuronList instanceof List<?> list)) {
            return 0;
        }
        Map<String, MovementActionNeuron> byAction = new HashMap<>();
        for (MovementActionNeuron neuron : neurons) {
            byAction.put(neuron.getAction().getActionId(), neuron);
        }
        int restored = 0;
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> neuronMap)) {
                continue;
            }
            Object actionId = ((Map<String, ?>) neuronMap).get("actionId");
            MovementActionNeuron neuron = actionId == null ? null : byAction.get(String.valueOf(actionId));
            if (neuron == null) {
                continue;
            }
            Object dendrites = ((Map<String, ?>) neuronMap).get("dendrites");
            if (dendrites instanceof Map<?, ?> dendriteMap) {
                Object savedWeights = ((Map<String, ?>) dendriteMap).get("weights");
                if (savedWeights instanceof Map<?, ?> weightMap) {
                    for (Map.Entry<?, ?> entry : weightMap.entrySet()) {
                        if (entry.getValue() instanceof Number number) {
                            neuron.getWeights().put(String.valueOf(entry.getKey()), number.doubleValue());
                        }
                    }
                }
            }
            Object savedBias = ((Map<String, ?>) neuronMap).get("bias");
            if (savedBias instanceof Number number) {
                neuron.setBias(number.doubleValue());
            }
            neuron.restoreLearnedState((Map<String, ?>) neuronMap);
            restored++;
        }
        return restored;
    }

    /** The {@code movementPolicy} model block consumed by the CARLA-Air bridge and exporters. */
    public Map<String, Object> toModelMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("policyVersion", POLICY_VERSION);
        map.put("processorContract", PROCESSOR_CONTRACT);
        map.put("runtime", config.asRuntimeMap());
        Map<String, Object> movementPolicy = new LinkedHashMap<>();
        List<Map<String, Object>> neuronMaps = new ArrayList<>();
        for (MovementActionNeuron neuron : neurons) {
            neuronMaps.add(neuron.snapshot(processors));
        }
        movementPolicy.put("harmGateNeuron", harmGateNeuron.snapshot());
        movementPolicy.put("neurons", neuronMaps);
        Map<String, Object> layers = new LinkedHashMap<>();
        layers.put("movementPolicy", movementPolicy);
        map.put("layers", layers);
        return map;
    }

    public Map<String, Object> snapshot() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("mode", "jneopallium-movement-policy-neurons");
        map.put("policyVersion", POLICY_VERSION);
        map.put("processorContract", PROCESSOR_CONTRACT);
        map.put("neuronCount", neurons.size());
        map.put("processorCount", processors.size());
        map.put("interfaces", interfaceNames());
        map.put("runtime", config.asRuntimeMap());
        map.put("coverageCellsVisited", coverageVisits.size());
        map.put("decisionsMade", decisionsMade);
        map.put("reinforcementUpdates", reinforcementUpdates);
        map.put("currentCommandedAltitudeMeters", round(currentAltitude));
        map.put("harmGateNeuron", harmGateNeuron.snapshot());
        List<Map<String, Object>> neuronMaps = new ArrayList<>();
        for (MovementActionNeuron neuron : neurons) {
            neuronMaps.add(neuron.snapshot(processors));
        }
        map.put("neurons", neuronMaps);
        return map;
    }

    public Map<String, Object> summary() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("mode", "jneopallium-movement-policy-neurons");
        map.put("policyVersion", POLICY_VERSION);
        map.put("neuronCount", neurons.size());
        map.put("processorCount", processors.size());
        map.put("interfaces", interfaceNames());
        map.put("harmGateNeuron", harmGateNeuron.snapshot());
        map.put("decisionsMade", decisionsMade);
        map.put("reinforcementUpdates", reinforcementUpdates);
        map.put("coverageCellsVisited", coverageVisits.size());
        map.put("commandHoldSeconds", config.getCommandHoldSeconds());
        Map<String, Object> speedWindow = new LinkedHashMap<>();
        speedWindow.put("min", config.getMinSpeedMetersPerSecond());
        speedWindow.put("max", config.getMaxSpeedMetersPerSecond());
        map.put("speedWindowMetersPerSecond", speedWindow);
        return map;
    }

    private List<String> interfaceNames() {
        TreeSet<String> names = new TreeSet<>();
        for (IMovementSignalProcessor processor : processors) {
            names.add(processor.interfaceName());
        }
        names.add("FastMovementHarmVeto");
        names.add("PhasicDopamineSignal");
        names.add("AcetylcholinePhaseSignal");
        names.add("HomeostasisSignal");
        return new ArrayList<>(names);
    }

    private static double clamp(double value, double low, double high) {
        return Math.max(low, Math.min(high, value));
    }

    private static double round(double value) {
        return Math.round(value * 1_000_000.0) / 1_000_000.0;
    }

    /** Decision plus the (optional) reinforcement applied to the previous decision. */
    public static final class DecisionOutcome {
        private final MovementDecisionSignal decision;
        private final MovementLearningResultSignal autoReinforcement;

        DecisionOutcome(MovementDecisionSignal decision, MovementLearningResultSignal autoReinforcement) {
            this.decision = decision;
            this.autoReinforcement = autoReinforcement;
        }

        public MovementDecisionSignal getDecision() { return decision; }
        public MovementLearningResultSignal getAutoReinforcement() { return autoReinforcement; }
    }

    private static final class LastDecision {
        private final MovementActionNeuron neuron;
        private final Map<String, Double> features;
        private final long frame;
        private final MovementHarmAssessment harm;

        LastDecision(MovementActionNeuron neuron, Map<String, Double> features, long frame,
                     MovementHarmAssessment harm) {
            this.neuron = neuron;
            this.features = features;
            this.frame = frame;
            this.harm = harm;
        }
    }

    private static final class Candidate {
        private final MovementActionNeuron neuron;
        private final Map<String, Double> features;
        private final double score;
        private final MovementHarmAssessment harm;

        Candidate(MovementActionNeuron neuron, Map<String, Double> features, double score,
                  MovementHarmAssessment harm) {
            this.neuron = neuron;
            this.features = features;
            this.score = score;
            this.harm = harm;
        }
    }
}
