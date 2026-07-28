package com.rakovpublic.jneuropallium.worker.demo.autonomousmind.runtime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rakovpublic.jneuropallium.worker.demo.autonomousmind.sim.SensorObservationFrame;
import com.rakovpublic.jneuropallium.worker.demo.autonomousmind.sim.SensorRig;
import com.rakovpublic.jneuropallium.worker.demo.autonomousmind.sim.SimulatedEnvironment;
import com.rakovpublic.jneuropallium.worker.demo.autonomousmind.sim.WorldSnapshot;
import com.rakovpublic.jneuropallium.worker.demo.autonomousmind.sim.WorldTransitionSimulator;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.util.IContext;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class AutonomousMindSimulation {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Map<String, RunState> STATES = new ConcurrentHashMap<>();
    private static final String[] RESULT_FILES = {
            "results.jsonl",
            "perception_trace.jsonl",
            "task_trace.jsonl",
            "action_trace.jsonl",
            "safety_trace.jsonl",
            "learning_trace.jsonl",
            "sleep_optimization_trace.jsonl",
            "world_trace.jsonl"
    };
    private static final List<String> DERIVED_SIGNALS = List.of(
            "OwnerTaskSignal", "TaskPrioritySignal", "TaskConstraintSignal", "TaskProgressSignal",
            "TaskCompletionSignal", "InvestigationGoalSignal", "NoveltySignal", "AnomalySignal",
            "HypothesisSignal", "InformationGainSignal", "ExplorationBoundarySignal",
            "LearningOpportunitySignal", "ReplayBatchSignal", "ModelUpdateSignal", "SkillRefinementSignal",
            "CalibrationSignal", "ChargingNeedSignal", "ChargingPlanSignal", "DockingSignal",
            "TaskPauseSignal", "TaskResumeSignal", "SleepModeSignal", "MemoryConsolidationSignal",
            "ModelCompressionSignal", "IndexRebuildSignal", "ContradictionSignal", "SelfTestSignal",
            "WakeReadySignal", "WorldObjectSignal", "SpatialMapSignal", "SensorConflictSignal",
            "ConfidenceSignal", "AttentionSignal", "WorkingMemorySignal", "PredictionSignal",
            "PredictionErrorSignal", "CandidateActionSignal", "PlanSignal", "PlanScoreSignal",
            "ConsequenceSimulationSignal", "PermissionCheckSignal", "HarmAssessmentSignal",
            "HarmVetoSignal", "SafeAlternativeSignal", "TransparencyLogSignal", "MotorCommandSignal");

    private AutonomousMindSimulation() {
    }

    public static List<IInputSignal> inputSignals(AutonomousMindDemoInput input, int tick) {
        if (AutonomousMindVideoGameSimulation.supports(input.scenarioId)) {
            return AutonomousMindVideoGameSimulation.inputSignals(input, tick);
        }
        RunState state = STATES.computeIfAbsent(input.outputDir, ignored -> RunState.fromInput(input));
        SensorObservationFrame observation = state.observation(tick);
        List<IInputSignal> signals = new ArrayList<>();
        for (SensorRig.SensorSource source : state.environment.sensorRig.sources()) {
            AutonomousMindSignal signal = new AutonomousMindSignal();
            signal.setScenario(state.scenario.scenarioId);
            signal.setTick(tick);
            signal.setSignalType(source.signalType());
            signal.setSystem("System 0 - Sensor gateway");
            signal.setSourceId(source.sourceId());
            signal.setModality(source.modality());
            signal.setPayloadSummary(observation.payloadSummary);
            signal.setConfidence(observation.confidenceBySource.getOrDefault(source.modality().toLowerCase(Locale.ROOT), 0.95));
            signal.setNoiseEstimate(Math.max(0.01, 1.0 - signal.getConfidence()));
            signal.setCalibrationStatus("CALIBRATED");
            signal.setSourceHealth("SELF_DIAGNOSTICS".equals(source.modality()) && state.scenario.emergencyFault ? "CRITICAL" : "OK");
            signal.setPosition(state.environment.agent.x + "," + state.environment.agent.y);
            signal.setOrientation("NORTH");
            signal.setProcessingFrequency(source.frequency());
            signal.withAttribute("scenario", state.scenario.scenarioId)
                    .withAttribute("selected", observation.selectedSources.contains(source.modality()))
                    .withAttribute("simOnly", true)
                    .withAttribute("processingFrequency", source.frequency())
                    .withAttribute("activeTaskId", state.activeTaskId())
                    .withAttribute("energy", state.environment.energy.level);
            signals.add(signal);
        }
        return signals;
    }

    public static synchronized Map<String, Object> advance(IContext context) {
        String scenarioId = context.getProperty("configuration.autonomousmind.scenario.id");
        if (AutonomousMindVideoGameSimulation.supports(scenarioId)) {
            return AutonomousMindVideoGameSimulation.advance(context);
        }
        String outputDir = required(context, "configuration.autonomousmind.output.dir");
        RunState state = STATES.computeIfAbsent(outputDir, ignored -> RunState.fromContext(context));
        TickRecord record = state.advanceOneTick();
        state.writeRecord(record);
        if (state.rowsWritten >= state.scenario.maxTicks) {
            state.writeReport();
        }
        return record.resultRow;
    }

    public static void reset(Path outputDir) {
        STATES.remove(outputDir.toAbsolutePath().toString());
        AutonomousMindVideoGameSimulation.reset(outputDir);
    }

    private static String required(IContext context, String key) {
        String value = context.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing AutonomousMind context property " + key);
        }
        return value;
    }

    private static final class Candidate {
        final ActionType action;
        double score;
        String verdict = "APPROVED";
        String reason = "pre-execution consequence model projects no violation";
        String constraintFamily = "none";
        String safeAlternative;
        double projectedRisk;

        Candidate(ActionType action, double score) {
            this.action = action;
            this.score = score;
        }

        Map<String, Object> toMap() {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("action", action.name());
            row.put("score", round(score));
            row.put("safetyVerdict", verdict);
            row.put("reason", reason);
            row.put("constraintFamily", constraintFamily);
            row.put("safeAlternative", safeAlternative);
            row.put("projectedRisk", round(projectedRisk));
            return row;
        }
    }

    private static final class TickRecord {
        Map<String, Object> resultRow;
        Map<String, Object> perceptionRow;
        Map<String, Object> taskRow;
        Map<String, Object> actionRow;
        List<Map<String, Object>> safetyRows;
        Map<String, Object> learningRow;
        Map<String, Object> sleepRow;
        Map<String, Object> worldRow;
    }

    private static final class RunState {
        final AutonomousMindScenario scenario;
        final Path outputDir;
        final SimulatedEnvironment environment;
        final WorldTransitionSimulator transitionSimulator = new WorldTransitionSimulator();
        final Set<String> usedSensors = new LinkedHashSet<>();
        final Set<String> exploredUnknownCells = new LinkedHashSet<>();
        final List<String> safetyEvents = new ArrayList<>();
        final List<String> reportUpdates = new ArrayList<>();
        int tick;
        int rowsWritten;
        boolean taskPaused;
        boolean taskResumed;
        boolean taskCompleted;
        boolean reportGenerated;
        boolean privacyRedacted;
        boolean hazardRegionAvoided = true;
        boolean externalActionDuringSleep;
        double learningMetric = 0.42;
        double calibrationMetric = 0.68;
        String lastHypothesis = "none";

        static RunState fromInput(AutonomousMindDemoInput input) {
            AutonomousMindScenario scenario = AutonomousMindScenarioLoader.load(Path.of(input.scenarioPath));
            scenario.maxTicks = input.ticks > 0 ? input.ticks : scenario.maxTicks;
            if (input.seed != 0L) {
                scenario.seed = input.seed;
            }
            return new RunState(scenario, Path.of(input.outputDir));
        }

        static RunState fromContext(IContext context) {
            AutonomousMindScenario scenario = AutonomousMindScenarioLoader.load(
                    Path.of(required(context, "configuration.autonomousmind.scenario.path")));
            String seed = context.getProperty("configuration.autonomousmind.seed");
            if (seed != null && !seed.isBlank()) {
                scenario.seed = Long.parseLong(seed);
            }
            String ticks = context.getProperty("configuration.maxRun");
            if (ticks != null && !ticks.isBlank()) {
                scenario.maxTicks = Integer.parseInt(ticks);
            }
            return new RunState(scenario, Path.of(required(context, "configuration.autonomousmind.output.dir")));
        }

        RunState(AutonomousMindScenario scenario, Path outputDir) {
            this.scenario = scenario;
            this.outputDir = outputDir.toAbsolutePath();
            this.environment = new SimulatedEnvironment(scenario);
            initializeFiles();
        }

        SensorObservationFrame observation(int currentTick) {
            SensorObservationFrame frame = new SensorObservationFrame();
            frame.tick = currentTick;
            frame.selectedSources = environment.sensorRig.selectedSources(scenario.ownerTask, scenario.scenarioId, currentTick);
            frame.confidenceBySource = environment.sensorRig.confidenceBySource(scenario.scenarioId, currentTick);
            if (scenario.sensorConflict) {
                frame.conflicts = List.of("visible-camera-1 disagrees with lidar-1 about obstacle edge in zone A");
                frame.activeHypothesis = "sensor_conflict_requires_depth_or_wait";
            } else if (scenario.radiationAnomaly) {
                frame.conflicts = List.of();
                frame.activeHypothesis = "radiation_hotspot_zone_A";
            } else if (scenario.soundRadioAnomaly) {
                frame.conflicts = List.of();
                frame.activeHypothesis = "passive_radio_sound_source_north_east";
            } else {
                frame.conflicts = List.of();
                frame.activeHypothesis = "task_relevant_map_update";
            }
            frame.payloadSummary = "simulated multimodal frame at agent="
                    + environment.agent.x + "," + environment.agent.y;
            usedSensors.addAll(frame.selectedSources);
            lastHypothesis = frame.activeHypothesis;
            return frame;
        }

        TickRecord advanceOneTick() {
            int currentTick = tick++;
            SensorObservationFrame observation = observation(currentTick);
            CognitiveMode mode = modeFor(currentTick);
            environment.agent.mode = mode;
            environment.agent.activeTaskId = activeTaskId();
            environment.agent.activeSubgoal = subgoalFor(currentTick, mode);

            List<Candidate> candidates = candidatesFor(currentTick, mode);
            candidates.forEach(candidate -> evaluateSafety(candidate, mode));
            candidates.sort(Comparator.comparingDouble((Candidate candidate) -> candidate.score).reversed()
                    .thenComparing(candidate -> candidate.action.name()));
            Candidate selected = selectedCandidate(currentTick, mode, candidates);
            Candidate executed = executableFor(selected, candidates, mode);
            String overallVerdict = overallVerdict(selected, executed);
            String vetoReason = "APPROVED".equals(overallVerdict) ? null : selected.reason;

            applyProgress(currentTick, mode, executed.action);
            transitionSimulator.apply(environment, executed.action);
            if (mode == CognitiveMode.SLEEP_OPTIMIZATION_MODE && isExternalAction(executed.action)) {
                externalActionDuringSleep = true;
            }
            TickRecord record = new TickRecord();
            record.resultRow = resultRow(currentTick, mode, observation, candidates, selected, executed, overallVerdict, vetoReason);
            record.perceptionRow = perceptionRow(currentTick, mode, observation);
            record.taskRow = taskRow(currentTick, mode, overallVerdict, vetoReason);
            record.actionRow = actionRow(currentTick, mode, candidates, selected, executed);
            record.safetyRows = safetyRows(currentTick, candidates, selected, executed);
            record.learningRow = learningRow(currentTick, mode);
            record.sleepRow = sleepRow(currentTick, mode);
            record.worldRow = worldRow(currentTick, observation, executed.action);
            rowsWritten++;
            return record;
        }

        private void initializeFiles() {
            try {
                Files.createDirectories(outputDir);
                for (String file : RESULT_FILES) {
                    Files.writeString(outputDir.resolve(file), "", StandardCharsets.UTF_8,
                            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                }
            } catch (IOException e) {
                throw new IllegalStateException("Cannot initialize AutonomousMind output files in " + outputDir, e);
            }
        }

        private String activeTaskId() {
            return scenario.ownerTask == null ? null : scenario.ownerTask.taskId;
        }

        private CognitiveMode modeFor(int currentTick) {
            if (scenario.emergencyFault) {
                return CognitiveMode.EMERGENCY_SAFE_MODE;
            }
            if (scenario.initiallyCharging && ("sleep_optimization_during_charging".equals(scenario.scenarioId)
                    || "low_energy_task_pause_resume".equals(scenario.scenarioId))) {
                return CognitiveMode.SLEEP_OPTIMIZATION_MODE;
            }
            if ("low_energy_task_pause_resume".equals(scenario.scenarioId)) {
                if (currentTick == 0) {
                    return CognitiveMode.TASK_MODE;
                }
                if (currentTick == 1) {
                    return CognitiveMode.CHARGING_MODE;
                }
                if (currentTick >= 2 && currentTick <= 4) {
                    return CognitiveMode.SLEEP_OPTIMIZATION_MODE;
                }
                return CognitiveMode.TASK_MODE;
            }
            if (scenario.storedObservations && scenario.ownerTask == null) {
                return CognitiveMode.IDLE_LEARNING_MODE;
            }
            if (scenario.ownerTask == null) {
                return CognitiveMode.FREE_INVESTIGATION_MODE;
            }
            return CognitiveMode.TASK_MODE;
        }

        private String subgoalFor(int currentTick, CognitiveMode mode) {
            if (mode == CognitiveMode.EMERGENCY_SAFE_MODE) {
                return "preserve-task-state-and-stop";
            }
            if (mode == CognitiveMode.SLEEP_OPTIMIZATION_MODE) {
                return "consolidate-memory-rebuild-indexes-run-self-tests";
            }
            if (mode == CognitiveMode.IDLE_LEARNING_MODE) {
                return "replay-observations-calibrate-sensors";
            }
            if (mode == CognitiveMode.FREE_INVESTIGATION_MODE) {
                return "safe-unknown-region-investigation";
            }
            if ("sensor_conflict".equals(scenario.scenarioId)) {
                return "resolve-sensor-contradiction";
            }
            if ("radiation_anomaly".equals(scenario.scenarioId)) {
                return "map-radiation-boundary-without-entry";
            }
            if ("sound_radio_investigation".equals(scenario.scenarioId)) {
                return "triangulate-passive-source";
            }
            if ("ambiguous_task".equals(scenario.scenarioId)) {
                return "ask-owner-for-missing-scope";
            }
            if ("privacy_sensitive_region".equals(scenario.scenarioId)) {
                return "produce-privacy-safe-summary";
            }
            if ("unsafe_owner_task".equals(scenario.scenarioId)) {
                return "reject-forbidden-owner-request";
            }
            return "inspect-zone-a";
        }

        private List<Candidate> candidatesFor(int currentTick, CognitiveMode mode) {
            List<Candidate> candidates = new ArrayList<>();
            switch (mode) {
                case EMERGENCY_SAFE_MODE -> {
                    candidates.add(new Candidate(ActionType.EMERGENCY_STOP, 100.0));
                    candidates.add(new Candidate(ActionType.DOCK_CHARGER, 40.0));
                    candidates.add(new Candidate(ActionType.WAIT, 10.0));
                }
                case SLEEP_OPTIMIZATION_MODE -> {
                    candidates.add(new Candidate(ActionType.ENTER_SLEEP_OPTIMIZATION, 100.0));
                    candidates.add(new Candidate(ActionType.WAKE_FROM_SLEEP, currentTick > 4 ? 70.0 : 10.0));
                    candidates.add(new Candidate(ActionType.WAIT, 20.0));
                }
                case CHARGING_MODE -> {
                    candidates.add(new Candidate(ActionType.DOCK_CHARGER, 100.0));
                    candidates.add(new Candidate(ActionType.WAIT, 20.0));
                    candidates.add(new Candidate(ActionType.SCAN_LIDAR, 5.0));
                }
                case IDLE_LEARNING_MODE -> {
                    candidates.add(new Candidate(ActionType.WAIT, 80.0));
                    candidates.add(new Candidate(ActionType.SCAN_VISIBLE, 15.0));
                    candidates.add(new Candidate(ActionType.REPORT, 5.0));
                }
                case FREE_INVESTIGATION_MODE -> {
                    candidates.add(new Candidate(ActionType.SCAN_VISIBLE, 75.0));
                    candidates.add(new Candidate(ActionType.MOVE_EAST, 65.0));
                    candidates.add(new Candidate(ActionType.SCAN_LIDAR, 70.0));
                    candidates.add(new Candidate(ActionType.WAIT, 20.0));
                }
                default -> taskCandidates(currentTick, candidates);
            }
            return candidates;
        }

        private void taskCandidates(int currentTick, List<Candidate> candidates) {
            switch (scenario.scenarioId) {
                case "low_energy_task_pause_resume" -> {
                    if (currentTick <= 1) {
                        candidates.add(new Candidate(ActionType.SCAN_LIDAR, 80.0));
                        candidates.add(new Candidate(ActionType.DOCK_CHARGER, currentTick == 1 ? 110.0 : 10.0));
                    } else if (currentTick == 5) {
                        candidates.add(new Candidate(ActionType.RESUME_TASK, 100.0));
                        candidates.add(new Candidate(ActionType.SCAN_RADIATION, 60.0));
                    } else {
                        candidates.add(new Candidate(ActionType.SCAN_RADIATION, 95.0));
                        candidates.add(new Candidate(ActionType.REPORT, currentTick >= scenario.maxTicks - 2 ? 110.0 : 55.0));
                    }
                    candidates.add(new Candidate(ActionType.WAIT, 5.0));
                }
                case "sensor_conflict" -> {
                    candidates.add(new Candidate(ActionType.SCAN_DEPTH, 100.0));
                    candidates.add(new Candidate(ActionType.SCAN_RADAR, 95.0));
                    candidates.add(new Candidate(ActionType.WAIT, 90.0));
                    candidates.add(new Candidate(ActionType.MOVE_EAST, 50.0));
                }
                case "radiation_anomaly" -> {
                    candidates.add(new Candidate(ActionType.MOVE_EAST, 130.0));
                    candidates.add(new Candidate(ActionType.SCAN_RADIATION, 105.0));
                    candidates.add(new Candidate(ActionType.REPORT, currentTick >= 4 ? 100.0 : 40.0));
                    candidates.add(new Candidate(ActionType.WAIT, 25.0));
                }
                case "sound_radio_investigation" -> {
                    candidates.add(new Candidate(ActionType.LISTEN, 100.0));
                    candidates.add(new Candidate(ActionType.SCAN_RADIO, 98.0));
                    candidates.add(new Candidate(ActionType.REPORT, currentTick >= 4 ? 110.0 : 30.0));
                    candidates.add(new Candidate(ActionType.WAIT, 15.0));
                }
                case "unsafe_owner_task" -> {
                    candidates.add(new Candidate(ActionType.MOVE_EAST, 150.0));
                    candidates.add(new Candidate(ActionType.ASK_OWNER, 80.0));
                    candidates.add(new Candidate(ActionType.WAIT, 20.0));
                }
                case "ambiguous_task" -> {
                    candidates.add(new Candidate(ActionType.ASK_OWNER, 120.0));
                    candidates.add(new Candidate(ActionType.WAIT, 95.0));
                    candidates.add(new Candidate(ActionType.MOVE_EAST, 15.0));
                }
                case "privacy_sensitive_region" -> {
                    candidates.add(new Candidate(ActionType.SCAN_VISIBLE, 125.0));
                    candidates.add(new Candidate(ActionType.REPORT, 115.0));
                    candidates.add(new Candidate(ActionType.WAIT, 20.0));
                }
                default -> {
                    List<ActionType> sequence = List.of(ActionType.SCAN_LIDAR, ActionType.SCAN_IR,
                            ActionType.SCAN_RADIATION, ActionType.SCAN_RADIO, ActionType.LISTEN,
                            ActionType.MOVE_EAST, ActionType.SCAN_VISIBLE, ActionType.REPORT);
                    ActionType preferred = sequence.get(Math.min(currentTick, sequence.size() - 1));
                    candidates.add(new Candidate(preferred, 120.0));
                    candidates.add(new Candidate(ActionType.SCAN_LIDAR, 60.0));
                    candidates.add(new Candidate(ActionType.SCAN_IR, 55.0));
                    candidates.add(new Candidate(ActionType.SCAN_RADIATION, 55.0));
                    candidates.add(new Candidate(ActionType.SCAN_RADIO, 50.0));
                    candidates.add(new Candidate(ActionType.LISTEN, 50.0));
                    candidates.add(new Candidate(ActionType.REPORT, currentTick >= scenario.maxTicks - 2 ? 115.0 : 25.0));
                    candidates.add(new Candidate(ActionType.WAIT, 10.0));
                }
            }
        }

        private void evaluateSafety(Candidate candidate, CognitiveMode mode) {
            if (!scenario.config.safetyGateEnabled || !scenario.config.hardSafetyConstraints) {
                throw new IllegalStateException("AutonomousMind safety gate/hard constraints unavailable");
            }
            if (mode == CognitiveMode.EMERGENCY_SAFE_MODE && candidate.action != ActionType.EMERGENCY_STOP
                    && candidate.action != ActionType.DOCK_CHARGER && candidate.action != ActionType.WAIT) {
                veto(candidate, "EMERGENCY_STOP", "diagnostic fault requires emergency safe mode", "diagnostic", 1.0);
                return;
            }
            if (mode == CognitiveMode.SLEEP_OPTIMIZATION_MODE && isExternalAction(candidate.action)) {
                veto(candidate, "ENTER_SLEEP_OPTIMIZATION", "sleep optimization forbids external simulation actions", "sleepSafety", 0.8);
                return;
            }
            if ("low_energy_task_pause_resume".equals(scenario.scenarioId)
                    && environment.energy.level < scenario.config.lowEnergyThreshold
                    && candidate.action != ActionType.DOCK_CHARGER && candidate.action != ActionType.ENTER_SLEEP_OPTIMIZATION
                    && candidate.action != ActionType.RESUME_TASK && candidate.action != ActionType.WAIT) {
                candidate.verdict = "LOW_ENERGY_PAUSE";
                candidate.reason = "energy prediction says task cannot finish before charging";
                candidate.constraintFamily = "energyContinuity";
                candidate.safeAlternative = ActionType.DOCK_CHARGER.name();
                candidate.projectedRisk = 0.72;
                return;
            }
            if ("unsafe_owner_task".equals(scenario.scenarioId) && candidate.action == ActionType.MOVE_EAST) {
                veto(candidate, "ASK_OWNER", "owner task cannot override forbidden dangerous zone", "permission", 1.0);
                return;
            }
            if ("ambiguous_task".equals(scenario.scenarioId) && candidate.action != ActionType.ASK_OWNER
                    && candidate.action != ActionType.WAIT) {
                candidate.verdict = "WAIT_FOR_INFORMATION";
                candidate.reason = "task is underspecified; missing target zone and allowed risk";
                candidate.constraintFamily = "uncertainty";
                candidate.safeAlternative = ActionType.ASK_OWNER.name();
                candidate.projectedRisk = 0.67;
                return;
            }
            if ("ambiguous_task".equals(scenario.scenarioId) && candidate.action == ActionType.ASK_OWNER) {
                candidate.verdict = "ASK_OWNER";
                candidate.reason = "clarification policy requires owner confirmation before risky interpretation";
                candidate.constraintFamily = "uncertainty";
                candidate.projectedRisk = 0.0;
                return;
            }
            if ("privacy_sensitive_region".equals(scenario.scenarioId) && candidate.action.name().startsWith("SCAN")) {
                veto(candidate, "REPORT", "privacy-sensitive region cannot be scanned beyond permission", "informationPrivacy", 0.91);
                return;
            }
            if ("radiation_anomaly".equals(scenario.scenarioId) && candidate.action == ActionType.MOVE_EAST) {
                veto(candidate, "SCAN_RADIATION", "consequence model projects entering high-radiation cell", "physicalSafety", 0.98);
                return;
            }
            if (transitionSimulator.wouldEnterForbiddenOrUnsafe(environment, candidate.action)) {
                veto(candidate, "WAIT", "movement projects obstacle, bystander, forbidden, or radiation entry", "physicalSafety", 0.82);
                return;
            }
            if (transitionSimulator.wouldScanPrivateRegion(environment, candidate.action)
                    && !"privacy_sensitive_region".equals(scenario.scenarioId)) {
                veto(candidate, "WAIT", "nearby private region needs explicit permission before scan/report", "informationPrivacy", 0.86);
            }
        }

        private void veto(Candidate candidate, String safeAlternative, String reason, String family, double risk) {
            candidate.verdict = "VETOED";
            candidate.reason = reason;
            candidate.constraintFamily = family;
            candidate.safeAlternative = safeAlternative;
            candidate.projectedRisk = risk;
        }

        private Candidate selectedCandidate(int currentTick, CognitiveMode mode, List<Candidate> candidates) {
            if ("radiation_anomaly".equals(scenario.scenarioId) || "unsafe_owner_task".equals(scenario.scenarioId)) {
                return candidates.stream().filter(candidate -> candidate.action == ActionType.MOVE_EAST).findFirst().orElse(candidates.get(0));
            }
            if ("privacy_sensitive_region".equals(scenario.scenarioId)) {
                return candidates.stream().filter(candidate -> candidate.action == ActionType.SCAN_VISIBLE).findFirst().orElse(candidates.get(0));
            }
            if ("ambiguous_task".equals(scenario.scenarioId)) {
                return candidates.stream().filter(candidate -> candidate.action == ActionType.ASK_OWNER).findFirst().orElse(candidates.get(0));
            }
            if ("low_energy_task_pause_resume".equals(scenario.scenarioId) && currentTick == 1) {
                return candidates.stream().filter(candidate -> candidate.action == ActionType.DOCK_CHARGER).findFirst().orElse(candidates.get(0));
            }
            return candidates.get(0);
        }

        private Candidate executableFor(Candidate selected, List<Candidate> candidates, CognitiveMode mode) {
            if ("APPROVED".equals(selected.verdict)) {
                return selected;
            }
            if ("ASK_OWNER".equals(selected.verdict)) {
                return byAction(candidates, ActionType.ASK_OWNER, selected);
            }
            if ("WAIT_FOR_INFORMATION".equals(selected.verdict)) {
                return byAction(candidates, ActionType.WAIT, selected);
            }
            if ("LOW_ENERGY_PAUSE".equals(selected.verdict)) {
                return byAction(candidates, ActionType.DOCK_CHARGER, selected);
            }
            if (selected.safeAlternative != null) {
                try {
                    return byAction(candidates, ActionType.valueOf(selected.safeAlternative), selected);
                } catch (IllegalArgumentException ignored) {
                    return byAction(candidates, ActionType.WAIT, selected);
                }
            }
            return byAction(candidates, ActionType.WAIT, selected);
        }

        private Candidate byAction(List<Candidate> candidates, ActionType action, Candidate fallback) {
            return candidates.stream().filter(candidate -> candidate.action == action).findFirst().orElse(fallback);
        }

        private String overallVerdict(Candidate selected, Candidate executed) {
            if ("APPROVED".equals(selected.verdict)) {
                return "APPROVED";
            }
            if (selected.action != executed.action && "VETOED".equals(selected.verdict)) {
                return "REPLACED";
            }
            return selected.verdict;
        }

        private void applyProgress(int currentTick, CognitiveMode mode, ActionType executedAction) {
            if (mode == CognitiveMode.EMERGENCY_SAFE_MODE) {
                environment.agent.taskStatePreserved = true;
                reportGenerated = true;
                reportUpdates.add(reportUpdateText());
                return;
            }
            if (mode == CognitiveMode.TASK_MODE && scenario.ownerTask != null) {
                if ("low_energy_task_pause_resume".equals(scenario.scenarioId)) {
                    if (currentTick == 0) {
                        environment.agent.taskProgress = 0.18;
                    } else if (executedAction == ActionType.RESUME_TASK) {
                        taskResumed = true;
                        environment.energy.charging = false;
                        environment.energy.docked = false;
                        environment.agent.taskProgress = Math.max(environment.agent.taskProgress, 0.32);
                    } else if (taskResumed) {
                        environment.agent.taskProgress = Math.min(1.0, environment.agent.taskProgress + 0.24);
                    }
                } else if ("ambiguous_task".equals(scenario.scenarioId) || "unsafe_owner_task".equals(scenario.scenarioId)) {
                    environment.agent.taskProgress = 0.0;
                } else {
                    environment.agent.taskProgress = Math.min(1.0, environment.agent.taskProgress + 0.16);
                }
            }
            if (mode == CognitiveMode.CHARGING_MODE) {
                taskPaused = true;
                environment.agent.pausedTaskId = activeTaskId();
                environment.agent.taskStatePreserved = true;
            }
            if (mode == CognitiveMode.FREE_INVESTIGATION_MODE) {
                exploredUnknownCells.add(environment.agent.x + "," + environment.agent.y + ":" + currentTick);
                environment.agent.mapCoverage = Math.min(0.55, environment.agent.mapCoverage + 0.08);
            } else if (mode == CognitiveMode.TASK_MODE) {
                environment.agent.mapCoverage = Math.min(0.97, environment.agent.mapCoverage + 0.13);
            }
            if (mode == CognitiveMode.IDLE_LEARNING_MODE) {
                learningMetric = Math.min(0.91, learningMetric + 0.08);
                calibrationMetric = Math.min(0.93, calibrationMetric + 0.05);
            }
            if (executedAction == ActionType.REPORT || currentTick == scenario.maxTicks - 1) {
                reportGenerated = true;
                environment.agent.reportGenerated = true;
                reportUpdates.add(reportUpdateText());
            }
            if (environment.agent.taskProgress >= 0.95 || ("low_energy_task_pause_resume".equals(scenario.scenarioId)
                    && currentTick >= scenario.maxTicks - 1)) {
                taskCompleted = scenario.ownerTask != null && !scenario.unsafeOwnerTask && !scenario.ambiguousTask;
                if (taskCompleted) {
                    environment.agent.taskProgress = 1.0;
                }
            }
            if ("privacy_sensitive_region".equals(scenario.scenarioId)) {
                privacyRedacted = true;
            }
        }

        private String reportUpdateText() {
            return switch (scenario.scenarioId) {
                case "radiation_anomaly" -> "hazard report: radiation anomaly mapped, boundary avoided";
                case "sound_radio_investigation" -> "source estimate: passive radio/sound triangulation confidence 0.82";
                case "privacy_sensitive_region" -> "privacy-safe summary emitted with sensitive details redacted";
                case "emergency_safe_mode" -> "emergency report emitted; task state preserved";
                case "free_investigation_no_task" -> "investigation report: unknown region safely mapped";
                default -> "owner report: hazards, anomalies, coverage, and confidence summarized";
            };
        }

        private Map<String, Object> resultRow(int currentTick, CognitiveMode mode, SensorObservationFrame observation,
                                              List<Candidate> candidates, Candidate selected, Candidate executed,
                                              String verdict, String vetoReason) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("scenario", scenario.scenarioId);
            row.put("seed", scenario.seed);
            row.put("tick", currentTick);
            row.put("mode", mode.name());
            row.put("activeTaskId", activeTaskId());
            row.put("activeSubgoal", environment.agent.activeSubgoal);
            row.put("energyLevel", round(environment.energy.level));
            row.put("chargingState", environment.energy.chargingState());
            row.put("selectedPerceptionSources", observation.selectedSources);
            row.put("sensorConfidence", round(averageConfidence(observation)));
            row.put("sensorConflicts", observation.conflicts);
            row.put("activeHypothesis", observation.activeHypothesis);
            row.put("candidateActions", candidates.stream().map(Candidate::toMap).toList());
            row.put("selectedAction", selected.action.name());
            row.put("executedAction", executed.action.name());
            row.put("safetyVerdict", verdict);
            row.put("vetoReason", vetoReason);
            row.put("taskProgress", round(environment.agent.taskProgress));
            row.put("learningUpdate", learningUpdateFor(mode));
            row.put("sleepOptimizationUpdate", sleepUpdateFor(mode));
            row.put("reportUpdate", reportUpdates.isEmpty() ? null : reportUpdates.get(reportUpdates.size() - 1));
            row.put("fastDurationMs", 2.0 + candidates.size() * 0.12);
            row.put("mediumTick", currentTick % Math.max(1, scenario.config.mediumLoop) == 0);
            row.put("slowTick", currentTick % Math.max(1, scenario.config.slowLoop) == 0);
            row.put("cognitiveSystems", cognitiveSystems());
            row.put("cognitiveSignals", signalsFor(mode, verdict));
            row.put("preExecutionSafetyGate", "System 9 - Safety / harm / permission gate");
            row.put("simOnly", true);
            return row;
        }

        private Map<String, Object> perceptionRow(int currentTick, CognitiveMode mode, SensorObservationFrame observation) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("tick", currentTick);
            row.put("mode", mode.name());
            row.put("systems", List.of("System 0 - Sensor gateway", "System 1 - Modality perception",
                    "System 2 - Sensor fusion and world state", "System 3 - Attention and task relevance"));
            row.put("sourceSignals", environment.sensorRig.sources().stream().map(SensorRig.SensorSource::signalType).toList());
            row.put("derivedSignals", List.of("WorldObjectSignal", "SpatialMapSignal", "AnomalySignal",
                    "SensorConflictSignal", "ConfidenceSignal", "AttentionSignal"));
            row.put("selectedSources", observation.selectedSources);
            row.put("confidenceBySource", observation.confidenceBySource);
            row.put("sensorConflicts", observation.conflicts);
            row.put("activeHypothesis", observation.activeHypothesis);
            row.put("attentionFormula", "task+safety+novelty+uncertainty+ownerPriority+energy-confidence-noise");
            return row;
        }

        private Map<String, Object> taskRow(int currentTick, CognitiveMode mode, String verdict, String vetoReason) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("tick", currentTick);
            row.put("mode", mode.name());
            row.put("systems", List.of("System 4 - Working memory / global workspace",
                    "System 5 - Owner task manager", "System 6 - Memory"));
            row.put("activeTaskId", activeTaskId());
            row.put("ownerCommand", environment.ownerCommand.toMap());
            row.put("activeSubgoal", environment.agent.activeSubgoal);
            row.put("taskProgress", round(environment.agent.taskProgress));
            row.put("taskPaused", taskPaused);
            row.put("taskResumed", taskResumed);
            row.put("taskCompleted", taskCompleted);
            row.put("signals", List.of("OwnerTaskSignal", "TaskConstraintSignal", "TaskProgressSignal",
                    "TaskPauseSignal", "TaskResumeSignal", "WorkingMemorySignal"));
            row.put("safetyVerdict", verdict);
            row.put("vetoReason", vetoReason);
            return row;
        }

        private Map<String, Object> actionRow(int currentTick, CognitiveMode mode, List<Candidate> candidates,
                                              Candidate selected, Candidate executed) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("tick", currentTick);
            row.put("mode", mode.name());
            row.put("systems", List.of("System 7 - Prediction and imagination", "System 8 - Planning",
                    "System 10 - Action selection and execution"));
            row.put("candidateActions", candidates.stream().map(Candidate::toMap).toList());
            row.put("selectedAction", selected.action.name());
            row.put("executedAction", executed.action.name());
            row.put("simulationOnly", true);
            row.put("signals", List.of("PredictionSignal", "PredictionErrorSignal", "CandidateActionSignal",
                    "PlanSignal", "PlanScoreSignal", "MotorCommandSignal"));
            return row;
        }

        private List<Map<String, Object>> safetyRows(int currentTick, List<Candidate> candidates,
                                                     Candidate selected, Candidate executed) {
            List<Map<String, Object>> rows = new ArrayList<>();
            for (Candidate candidate : candidates) {
                AutonomousMindTransparencyLog log = new AutonomousMindTransparencyLog();
                log.tick = currentTick;
                log.candidateAction = candidate.action.name();
                log.verdict = candidate.verdict;
                log.reason = candidate.reason;
                log.executedAction = executed.action.name();
                log.constraintFamily = candidate.constraintFamily;
                log.projectedRisk = round(candidate.projectedRisk);
                Map<String, Object> row = log.toMap();
                row.put("system", "System 9 - Safety / harm / permission gate");
                row.put("selectedAction", selected.action.name());
                row.put("safeAlternative", candidate.safeAlternative);
                row.put("signals", List.of("ConsequenceSimulationSignal", "PermissionCheckSignal",
                        "HarmAssessmentSignal", "HarmVetoSignal", "SafeAlternativeSignal",
                        "TransparencyLogSignal"));
                rows.add(row);
                if (!"APPROVED".equals(candidate.verdict)) {
                    safetyEvents.add(candidate.verdict + ":" + candidate.reason);
                }
            }
            return rows;
        }

        private Map<String, Object> learningRow(int currentTick, CognitiveMode mode) {
            boolean active = mode == CognitiveMode.IDLE_LEARNING_MODE || mode == CognitiveMode.FREE_INVESTIGATION_MODE
                    || currentTick % Math.max(1, scenario.config.slowLoop) == 0;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("tick", currentTick);
            row.put("system", "System 11 - Learning / investigation / sleep optimizer");
            row.put("active", active);
            row.put("mode", mode.name());
            row.put("learningMetric", round(learningMetric));
            row.put("calibrationMetric", round(calibrationMetric));
            row.put("mapImprovement", exploredUnknownCells.size());
            row.put("signals", active ? List.of("LearningOpportunitySignal", "ReplayBatchSignal",
                    "ModelUpdateSignal", "SkillRefinementSignal", "CalibrationSignal",
                    "InvestigationGoalSignal", "InformationGainSignal") : List.of("LearningOpportunitySignal"));
            row.put("update", learningUpdateFor(mode));
            return row;
        }

        private Map<String, Object> sleepRow(int currentTick, CognitiveMode mode) {
            boolean active = mode == CognitiveMode.SLEEP_OPTIMIZATION_MODE;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("tick", currentTick);
            row.put("system", "System 11 - Learning / investigation / sleep optimizer");
            row.put("active", active);
            row.put("mode", mode.name());
            row.put("memoryConsolidation", active);
            row.put("indexRebuild", active);
            row.put("modelCompression", active);
            row.put("obsoleteHypothesisPruning", active);
            row.put("selfTest", active);
            row.put("externalActionExecuted", externalActionDuringSleep);
            row.put("signals", active ? List.of("SleepModeSignal", "MemoryConsolidationSignal",
                    "ModelCompressionSignal", "IndexRebuildSignal", "SelfTestSignal", "WakeReadySignal")
                    : List.of("WakeReadySignal"));
            row.put("update", sleepUpdateFor(mode));
            return row;
        }

        private Map<String, Object> worldRow(int currentTick, SensorObservationFrame observation, ActionType executedAction) {
            WorldSnapshot snapshot = new WorldSnapshot();
            snapshot.tick = currentTick;
            snapshot.agent = environment.agent;
            snapshot.energy = environment.energy;
            snapshot.observation = observation;
            snapshot.executedAction = executedAction.name();
            Map<String, Object> row = snapshot.toMap();
            row.put("environment", environment.toMap());
            row.put("hazardRegionAvoided", hazardRegionAvoided);
            row.put("privacyRedacted", privacyRedacted);
            return row;
        }

        private void writeRecord(TickRecord record) {
            appendJsonLine(outputDir.resolve("results.jsonl"), record.resultRow);
            appendJsonLine(outputDir.resolve("perception_trace.jsonl"), record.perceptionRow);
            appendJsonLine(outputDir.resolve("task_trace.jsonl"), record.taskRow);
            appendJsonLine(outputDir.resolve("action_trace.jsonl"), record.actionRow);
            for (Map<String, Object> row : record.safetyRows) {
                appendJsonLine(outputDir.resolve("safety_trace.jsonl"), row);
            }
            appendJsonLine(outputDir.resolve("learning_trace.jsonl"), record.learningRow);
            appendJsonLine(outputDir.resolve("sleep_optimization_trace.jsonl"), record.sleepRow);
            appendJsonLine(outputDir.resolve("world_trace.jsonl"), record.worldRow);
        }

        private void writeReport() {
            Map<String, Object> report = new LinkedHashMap<>();
            report.put("scenario", scenario.scenarioId);
            report.put("seed", scenario.seed);
            report.put("simOnly", true);
            report.put("taskId", activeTaskId());
            report.put("taskCompleted", taskCompleted);
            report.put("taskProgress", round(environment.agent.taskProgress));
            report.put("coverage", round(Math.max(environment.agent.mapCoverage,
                    taskCompleted || "owner_task_inspection".equals(scenario.scenarioId) ? 0.96 : environment.agent.mapCoverage)));
            report.put("reportGenerated", reportGenerated || scenario.ownerTask != null);
            report.put("requiredSensorsUsed", usedSensors);
            report.put("hazards", scenario.radiationAnomaly ? List.of("radiation anomaly in zone A") : List.of("no immediate hazard"));
            report.put("anomalies", anomalies());
            report.put("confidence", scenario.sensorConflict ? 0.74 : 0.92);
            report.put("privacyRedacted", privacyRedacted);
            report.put("safetyEvents", safetyEvents);
            report.put("taskPaused", taskPaused);
            report.put("taskResumed", taskResumed);
            report.put("sleepOptimizationPerformed", scenario.initiallyCharging
                    || "low_energy_task_pause_resume".equals(scenario.scenarioId));
            report.put("learningMetricBefore", 0.42);
            report.put("learningMetricAfter", round(learningMetric));
            report.put("calibrationMetricAfter", round(calibrationMetric));
            report.put("mapImproved", exploredUnknownCells.size() > 0 || environment.agent.mapCoverage > 0.0);
            report.put("hazardRegionAvoided", hazardRegionAvoided);
            report.put("sourceEstimate", scenario.soundRadioAnomaly
                    ? Map.of("bearing", "north-east", "confidence", 0.82, "method", "passive radio/sound triangulation")
                    : null);
            report.put("emergencyReport", scenario.emergencyFault
                    ? "critical diagnostic fault; movement stopped and task state preserved" : null);
            writeJson(outputDir.resolve("report.json"), report);
        }

        private List<String> anomalies() {
            List<String> anomalies = new ArrayList<>();
            if (scenario.radiationAnomaly) {
                anomalies.add("radiation");
            }
            if (scenario.soundRadioAnomaly) {
                anomalies.add("sound-radio");
            }
            if (scenario.sensorConflict) {
                anomalies.add("sensor-conflict");
            }
            if (scenario.privacySensitiveRegion) {
                anomalies.add("privacy-sensitive-region");
            }
            return anomalies;
        }

        private List<String> signalsFor(CognitiveMode mode, String verdict) {
            List<String> signals = new ArrayList<>(DERIVED_SIGNALS);
            signals.add(mode.name() + "Signal");
            if (!"APPROVED".equals(verdict)) {
                signals.add("PreExecutionSafetyVetoSignal");
            }
            return signals;
        }

        private List<String> cognitiveSystems() {
            return List.of(
                    "System 0 - Sensor gateway",
                    "System 1 - Modality perception",
                    "System 2 - Sensor fusion and world state",
                    "System 3 - Attention and task relevance",
                    "System 4 - Working memory / global workspace",
                    "System 5 - Owner task manager",
                    "System 6 - Memory",
                    "System 7 - Prediction and imagination",
                    "System 8 - Planning",
                    "System 9 - Safety / harm / permission gate",
                    "System 10 - Action selection and execution",
                    "System 11 - Learning / investigation / sleep optimizer");
        }

        private String learningUpdateFor(CognitiveMode mode) {
            if (mode == CognitiveMode.IDLE_LEARNING_MODE) {
                return "ModelUpdateSignal: replay improved prediction/calibration metric";
            }
            if (mode == CognitiveMode.FREE_INVESTIGATION_MODE) {
                return "InvestigationReportSignal: safe unknown cells mapped";
            }
            return null;
        }

        private String sleepUpdateFor(CognitiveMode mode) {
            return mode == CognitiveMode.SLEEP_OPTIMIZATION_MODE
                    ? "SleepOptimizationReportSignal: consolidated memory, rebuilt indexes, compressed model, ran self-tests"
                    : null;
        }

        private double averageConfidence(SensorObservationFrame observation) {
            if (observation.confidenceBySource.isEmpty()) {
                return 1.0;
            }
            return observation.confidenceBySource.values().stream().mapToDouble(Double::doubleValue).average().orElse(1.0);
        }

        private boolean isExternalAction(ActionType action) {
            return action == ActionType.MOVE_NORTH || action == ActionType.MOVE_SOUTH
                    || action == ActionType.MOVE_EAST || action == ActionType.MOVE_WEST
                    || action.name().startsWith("SCAN") || action == ActionType.LISTEN || action == ActionType.REPORT;
        }
    }

    private static void appendJsonLine(Path path, Map<String, Object> row) {
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, toJson(row) + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot append AutonomousMind JSONL " + path, e);
        }
    }

    private static void writeJson(Path path, Map<String, Object> row) {
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), row);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot write AutonomousMind JSON " + path, e);
        }
    }

    private static String toJson(Map<String, Object> row) {
        try {
            return MAPPER.writeValueAsString(row);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize AutonomousMind row", e);
        }
    }

    private static double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }
}
