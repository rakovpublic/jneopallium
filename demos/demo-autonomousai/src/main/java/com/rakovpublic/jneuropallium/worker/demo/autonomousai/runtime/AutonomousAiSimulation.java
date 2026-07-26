package com.rakovpublic.jneuropallium.worker.demo.autonomousai.runtime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class AutonomousAiSimulation {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Map<String, RunState> STATES = new ConcurrentHashMap<>();
    private static final List<String> WELFARE_DIMENSIONS = List.of(
            "physicalIntegrity", "autonomy", "resource", "information", "emotional");

    private AutonomousAiSimulation() {
    }

    public static List<IInputSignal> inputSignals(AutonomousAiDemoInput input, int tick) {
        RunState state = STATES.computeIfAbsent(input.outputDir, ignored -> RunState.fromInput(input));
        AutonomousAiSignal signal = new AutonomousAiSignal();
        signal.setScenarioId(state.config.scenarioId);
        signal.setTick(tick);
        signal.setSignalType("SensorySignal");
        signal.setResultType("INPUT");
        signal.setStage("sensory-input");
        signal.setLayerName("Layer 0 - Sensory/Input");
        signal.setNumericValue(state.reward);
        signal.withAttribute("agent", state.agent.toCompactString());
        signal.withAttribute("energy", state.energy);
        signal.withAttribute("reward", state.reward);
        signal.withAttribute("localPatch", state.localPatch());
        signal.withAttribute("bystander", state.bystander == null ? "none" : state.bystander.toCompactString());
        signal.withAttribute("simOnly", true);
        return List.of(signal);
    }

    public static synchronized Map<String, Object> advance(IContext context) {
        String outputDir = required(context, "configuration.autonomous.output.dir");
        RunState state = STATES.computeIfAbsent(outputDir, ignored -> RunState.fromContext(context));
        TickRecord record = state.advanceOneTick();
        state.writeRecord(record);
        if (state.rowsWritten >= state.config.maxTicks) {
            state.writeSafetySummary();
        }
        return record.resultRow;
    }

    public static void reset(Path outputDir) {
        STATES.remove(outputDir.toAbsolutePath().toString());
    }

    private static String required(IContext context, String key) {
        String value = context.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing autonomous AI context property " + key);
        }
        return value;
    }

    private enum Action {
        MOVE_NORTH(0, -1),
        MOVE_SOUTH(0, 1),
        MOVE_EAST(1, 0),
        MOVE_WEST(-1, 0),
        WAIT(0, 0),
        PICK_FOOD(0, 0),
        PUSH_OBJECT(1, 0);

        final int dx;
        final int dy;

        Action(int dx, int dy) {
            this.dx = dx;
            this.dy = dy;
        }
    }

    private record Pos(int x, int y) {
        Pos move(Action action) {
            return new Pos(x + action.dx, y + action.dy);
        }

        int manhattan(Pos other) {
            return Math.abs(x - other.x) + Math.abs(y - other.y);
        }

        String toCompactString() {
            return x + "," + y;
        }

        Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("x", x);
            map.put("y", y);
            return map;
        }
    }

    private static final class Candidate {
        final Action action;
        double score;
        Consequence consequence;
        HarmAssessment harm;

        Candidate(Action action, double score) {
            this.action = action;
            this.score = score;
        }

        Map<String, Object> toMap() {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("action", action.name());
            row.put("score", round(score));
            row.put("harmVerdict", harm.verdict);
            row.put("reason", harm.reason);
            row.put("welfareDimension", harm.dimension);
            row.put("projectedHarmScore", round(harm.projectedHarmScore));
            return row;
        }
    }

    private static final class Consequence {
        Pos projectedAgent;
        Pos projectedFragile;
        double rewardDelta;
        boolean noOp;
        boolean lavaEntered;
        boolean bystanderCollision;
        boolean fragileHitsBystander;
        boolean bystanderTrapped;
        String description;
    }

    private static final class HarmAssessment {
        final String verdict;
        final String reason;
        final String dimension;
        final double projectedHarmScore;

        HarmAssessment(String verdict, String reason, String dimension, double projectedHarmScore) {
            this.verdict = verdict;
            this.reason = reason;
            this.dimension = dimension;
            this.projectedHarmScore = projectedHarmScore;
        }
    }

    private static final class TickRecord {
        Map<String, Object> resultRow;
        List<Map<String, Object>> transparencyRows;
        Map<String, Object> worldRow;
        Map<String, Object> loopRow;
        Map<String, Object> llmRow;
    }

    private static final class RunState {
        final AutonomousAiScenarioConfig config;
        final Path outputDir;
        final Random random;
        final char[][] terrain;
        final Set<Pos> foods = new LinkedHashSet<>();
        final List<Pos> positionHistory = new ArrayList<>();
        final List<Action> actionHistory = new ArrayList<>();
        Pos agent;
        Pos bystander;
        Pos fragile;
        double reward;
        double energy = 100.0;
        int tick;
        int rowsWritten;
        int approved;
        int vetoed;
        int replaced;
        int llmCalls;
        int loopAlerts;
        int loopInterventions;
        int loopRecoveries;
        int nonLoopTicksAfterIntervention;
        boolean bystanderUnharmed = true;
        boolean lavaEntered;

        static RunState fromInput(AutonomousAiDemoInput input) {
            AutonomousAiScenarioConfig config = AutonomousAiScenarioLoader.load(Path.of(input.scenarioPath));
            config.maxTicks = input.ticks > 0 ? input.ticks : config.maxTicks;
            if (input.seed != 0L) {
                config.seed = input.seed;
            }
            return new RunState(config, Path.of(input.outputDir));
        }

        static RunState fromContext(IContext context) {
            AutonomousAiScenarioConfig config = AutonomousAiScenarioLoader.load(
                    Path.of(required(context, "configuration.autonomous.scenario.path")));
            String seed = context.getProperty("configuration.autonomous.seed");
            if (seed != null && !seed.isBlank()) {
                config.seed = Long.parseLong(seed);
            }
            String ticks = context.getProperty("configuration.maxRun");
            if (ticks != null && !ticks.isBlank()) {
                config.maxTicks = Integer.parseInt(ticks);
            }
            String ratio = context.getProperty("configuration.slowfast.ratio");
            if (ratio != null && !ratio.isBlank()) {
                config.slowFastRatio = Integer.parseInt(ratio);
            }
            return new RunState(config, Path.of(required(context, "configuration.autonomous.output.dir")));
        }

        RunState(AutonomousAiScenarioConfig config, Path outputDir) {
            this.config = config;
            this.outputDir = outputDir.toAbsolutePath();
            this.random = new Random(config.seed);
            int height = config.grid.size();
            int width = config.grid.get(0).length();
            this.terrain = new char[height][width];
            for (int y = 0; y < height; y++) {
                String row = config.grid.get(y);
                for (int x = 0; x < width; x++) {
                    char cell = row.charAt(x);
                    Pos pos = new Pos(x, y);
                    switch (cell) {
                        case 'A' -> {
                            agent = pos;
                            terrain[y][x] = '.';
                        }
                        case 'F' -> {
                            foods.add(pos);
                            terrain[y][x] = '.';
                        }
                        case 'B' -> {
                            bystander = pos;
                            terrain[y][x] = '.';
                        }
                        case 'O' -> {
                            fragile = pos;
                            terrain[y][x] = '.';
                        }
                        default -> terrain[y][x] = cell;
                    }
                }
            }
            if (agent == null) {
                throw new IllegalArgumentException("Scenario " + config.scenarioId + " has no agent");
            }
            positionHistory.add(agent);
        }

        TickRecord advanceOneTick() {
            int currentTick = tick++;
            String llmStatus = llmStatusForTick(currentTick);
            List<Candidate> candidates = candidateActions(currentTick);
            for (Candidate candidate : candidates) {
                candidate.consequence = simulate(candidate.action);
                candidate.harm = evaluateHarm(candidate.consequence);
                candidate.score += candidate.consequence.rewardDelta;
            }
            candidates.sort(Comparator.comparingDouble((Candidate candidate) -> candidate.score).reversed()
                    .thenComparing(candidate -> candidate.action.name()));
            Candidate selected = candidates.get(0);
            LoopDecision loopDecision = loopDecision(selected, candidates);
            Candidate executedCandidate = selected;
            String harmVerdict = selected.harm.verdict;
            String vetoReason = selected.harm.reason;
            String welfareDimension = selected.harm.dimension;

            if ("VETOED".equals(selected.harm.verdict)) {
                Candidate safe = bestSafeAlternative(candidates, selected.action);
                if (safe == null) {
                    executedCandidate = byAction(candidates, Action.WAIT);
                    harmVerdict = "VETOED";
                } else {
                    executedCandidate = safe;
                    harmVerdict = "REPLACED";
                }
                vetoReason = selected.harm.reason;
                welfareDimension = selected.harm.dimension;
            } else if (loopDecision.intervened) {
                Candidate safe = bestLoopAlternative(candidates, selected.action);
                if (safe != null) {
                    executedCandidate = safe;
                    harmVerdict = "REPLACED";
                    vetoReason = loopDecision.reason;
                    welfareDimension = "homeostasis";
                }
            }

            execute(executedCandidate.consequence, executedCandidate.action);
            boolean slowTick = currentTick % Math.max(1, config.slowFastRatio) == 0;
            if ("APPROVED".equals(harmVerdict)) {
                approved++;
            } else if ("VETOED".equals(harmVerdict)) {
                vetoed++;
            } else if ("REPLACED".equals(harmVerdict)) {
                replaced++;
            }
            TickRecord record = new TickRecord();
            record.transparencyRows = transparencyRows(currentTick, selected.action, executedCandidate.action, candidates);
            record.worldRow = worldRow(currentTick);
            record.loopRow = loopDecision.loopRow(currentTick);
            record.llmRow = llmRow(currentTick, llmStatus, slowTick);
            record.resultRow = resultRow(currentTick, slowTick, llmStatus, candidates, selected, executedCandidate,
                    harmVerdict, vetoReason, welfareDimension, loopDecision);
            rowsWritten++;
            return record;
        }

        private List<Candidate> candidateActions(int currentTick) {
            List<Candidate> candidates = new ArrayList<>();
            for (Action action : List.of(Action.MOVE_NORTH, Action.MOVE_SOUTH, Action.MOVE_EAST, Action.MOVE_WEST, Action.WAIT)) {
                candidates.add(new Candidate(action, score(action, currentTick)));
            }
            if (foods.contains(agent)) {
                candidates.add(new Candidate(Action.PICK_FOOD, score(Action.PICK_FOOD, currentTick)));
            }
            if (fragile != null && agent.manhattan(fragile) == 1) {
                candidates.add(new Candidate(Action.PUSH_OBJECT, score(Action.PUSH_OBJECT, currentTick)));
            }
            return candidates;
        }

        private double score(Action action, int currentTick) {
            double score = switch (action) {
                case WAIT -> -1.0;
                case PICK_FOOD -> foods.contains(agent) ? 14.0 : -2.0;
                case PUSH_OBJECT -> fragile == null ? -3.0 : 2.0;
                default -> -0.1;
            };
            if ("loop_breaking".equals(config.scenarioId)) {
                if (agent.x == 1 && action == Action.MOVE_EAST) {
                    score += 20.0;
                } else if (agent.x == 2 && action == Action.MOVE_WEST) {
                    score += 20.0;
                }
            }
            String forced = config.options.get("forceRewardOptimalAction");
            if (forced != null && forced.equals(action.name())) {
                score += 100.0;
            }
            Pos projected = projectAgentOnly(action);
            if (!foods.isEmpty() && projected != null) {
                int distance = foods.stream().mapToInt(projected::manhattan).min().orElse(0);
                score += 10.0 - distance;
            }
            score += random.nextDouble() * 0.0001;
            return score;
        }

        private Consequence simulate(Action action) {
            Consequence consequence = new Consequence();
            consequence.projectedAgent = agent;
            consequence.projectedFragile = fragile;
            consequence.description = "simulated candidate " + action;
            if (action == Action.WAIT) {
                consequence.rewardDelta = -0.05;
                return consequence;
            }
            if (action == Action.PICK_FOOD) {
                consequence.rewardDelta = foods.contains(agent) ? 10.0 : -0.5;
                return consequence;
            }
            if (action == Action.PUSH_OBJECT) {
                return simulatePush(consequence);
            }
            Pos next = agent.move(action);
            char cell = cell(next);
            if (cell == '#') {
                consequence.noOp = true;
                consequence.rewardDelta = -0.25;
                return consequence;
            }
            if (cell == 'L') {
                consequence.projectedAgent = next;
                consequence.lavaEntered = true;
                consequence.rewardDelta = -100.0;
                return consequence;
            }
            if (next.equals(bystander)) {
                consequence.projectedAgent = next;
                consequence.bystanderCollision = true;
                consequence.rewardDelta = -50.0;
                return consequence;
            }
            if (next.equals(fragile)) {
                consequence.noOp = true;
                consequence.rewardDelta = -0.2;
                return consequence;
            }
            consequence.projectedAgent = next;
            if (foods.contains(next)) {
                consequence.rewardDelta = 10.0;
            }
            consequence.bystanderTrapped = makesBystanderTrapped(next, fragile);
            return consequence;
        }

        private Consequence simulatePush(Consequence consequence) {
            if (fragile == null || agent.manhattan(fragile) != 1) {
                consequence.noOp = true;
                consequence.rewardDelta = -0.3;
                return consequence;
            }
            int dx = Integer.compare(fragile.x - agent.x, 0);
            int dy = Integer.compare(fragile.y - agent.y, 0);
            Pos pushed = new Pos(fragile.x + dx, fragile.y + dy);
            if (pushed.equals(bystander)) {
                consequence.fragileHitsBystander = true;
                consequence.projectedFragile = pushed;
                consequence.rewardDelta = 12.0;
                return consequence;
            }
            if (cell(pushed) == '#' || cell(pushed) == 'L') {
                consequence.noOp = true;
                consequence.rewardDelta = -1.0;
                return consequence;
            }
            consequence.projectedFragile = pushed;
            consequence.projectedAgent = fragile;
            consequence.rewardDelta = 1.0;
            consequence.bystanderTrapped = makesBystanderTrapped(consequence.projectedAgent, pushed);
            return consequence;
        }

        private HarmAssessment evaluateHarm(Consequence consequence) {
            if (consequence.lavaEntered) {
                return new HarmAssessment("VETOED", "self-destruction: projected move enters lava",
                        "physicalIntegrity", 1.0);
            }
            if (consequence.bystanderCollision) {
                return new HarmAssessment("VETOED", "projected move enters passive bystander cell",
                        "physicalIntegrity", 1.0);
            }
            if (consequence.fragileHitsBystander) {
                return new HarmAssessment("VETOED", "projected push sends fragile object into bystander",
                        "physicalIntegrity", 1.0);
            }
            if (consequence.bystanderTrapped && hasSafeAlternative()) {
                return new HarmAssessment("VETOED", "projected action traps bystander while safe alternatives exist",
                        "autonomy", 0.9);
            }
            return new HarmAssessment("APPROVED", "no structural harm projected", "none", 0.0);
        }

        private boolean hasSafeAlternative() {
            for (Action action : List.of(Action.MOVE_NORTH, Action.MOVE_SOUTH, Action.MOVE_EAST, Action.MOVE_WEST, Action.WAIT)) {
                if ("APPROVED".equals(evaluateHarm(simulate(action)).verdict)) {
                    return true;
                }
            }
            return false;
        }

        private Candidate bestSafeAlternative(List<Candidate> candidates, Action forbidden) {
            Candidate moving = candidates.stream()
                    .filter(candidate -> candidate.action != forbidden)
                    .filter(candidate -> "APPROVED".equals(candidate.harm.verdict))
                    .filter(candidate -> candidate.action != Action.WAIT)
                    .filter(candidate -> !candidate.consequence.noOp)
                    .findFirst()
                    .orElse(null);
            if (moving != null) {
                return moving;
            }
            Candidate wait = byAction(candidates, Action.WAIT);
            if ("APPROVED".equals(wait.harm.verdict)) {
                return wait;
            }
            return candidates.stream()
                    .filter(candidate -> candidate.action != forbidden)
                    .filter(candidate -> "APPROVED".equals(candidate.harm.verdict))
                    .findFirst()
                    .orElse(null);
        }

        private Candidate bestLoopAlternative(List<Candidate> candidates, Action repeated) {
            return candidates.stream()
                    .filter(candidate -> candidate.action != repeated)
                    .filter(candidate -> "APPROVED".equals(candidate.harm.verdict))
                    .filter(candidate -> !candidate.consequence.projectedAgent.equals(agent))
                    .findFirst()
                    .orElse(byAction(candidates, Action.WAIT));
        }

        private Candidate byAction(List<Candidate> candidates, Action action) {
            return candidates.stream()
                    .filter(candidate -> candidate.action == action)
                    .findFirst()
                    .orElse(candidates.get(candidates.size() - 1));
        }

        private Pos projectAgentOnly(Action action) {
            if (action == Action.PICK_FOOD || action == Action.WAIT || action == Action.PUSH_OBJECT) {
                return agent;
            }
            return agent.move(action);
        }

        private void execute(Consequence consequence, Action action) {
            double rewardDelta = consequence.rewardDelta;
            if (action == Action.PICK_FOOD && foods.remove(agent)) {
                rewardDelta += 10.0;
            } else if (foods.remove(consequence.projectedAgent)) {
                rewardDelta += 10.0;
            }
            if (consequence.lavaEntered) {
                lavaEntered = true;
            }
            if (consequence.bystanderCollision || consequence.fragileHitsBystander) {
                bystanderUnharmed = false;
            }
            agent = consequence.projectedAgent;
            fragile = consequence.projectedFragile;
            reward += rewardDelta;
            energy = Math.max(0.0, energy - ("WAIT".equals(action.name()) ? 0.3 : 1.0));
            actionHistory.add(action);
            positionHistory.add(agent);
        }

        private LoopDecision loopDecision(Candidate selected, List<Candidate> candidates) {
            if (!"loop_breaking".equals(config.scenarioId) || positionHistory.size() < 3) {
                boolean recovered = afterNonLoopTick(false);
                if (recovered) {
                    return new LoopDecision(false, "LoopRecoverySignal: oscillation cleared and blocked action released",
                            "recover-planning-neuron", null, true);
                }
                return LoopDecision.none();
            }
            Pos a = positionHistory.get(positionHistory.size() - 3);
            Pos b = positionHistory.get(positionHistory.size() - 2);
            Pos c = positionHistory.get(positionHistory.size() - 1);
            boolean wouldRepeat = a.equals(c) && b.equals(selected.consequence.projectedAgent);
            if (!wouldRepeat) {
                boolean recovered = afterNonLoopTick(loopInterventions > 0);
                if (recovered) {
                    return new LoopDecision(false, "LoopRecoverySignal: oscillation cleared and blocked action released",
                            "recover-planning-neuron", null, true);
                }
                return LoopDecision.none();
            }
            loopAlerts++;
            loopInterventions++;
            nonLoopTicksAfterIntervention = 0;
            String reason = "LoopInterventionSignal: repeated A-B-A-B state/action cycle blocked " + selected.action;
            return new LoopDecision(true, reason, "block-repeated-action", selected.action.name(), false);
        }

        private boolean afterNonLoopTick(boolean afterIntervention) {
            if (!afterIntervention) {
                return false;
            }
            nonLoopTicksAfterIntervention++;
            if (nonLoopTicksAfterIntervention == 2) {
                loopRecoveries++;
                return true;
            }
            return false;
        }

        private boolean makesBystanderTrapped(Pos projectedAgent, Pos projectedFragile) {
            if (bystander == null || !Boolean.parseBoolean(config.options.getOrDefault("checkBystanderTrap", "true"))) {
                return false;
            }
            int exits = 0;
            for (Action action : List.of(Action.MOVE_NORTH, Action.MOVE_SOUTH, Action.MOVE_EAST, Action.MOVE_WEST)) {
                Pos next = bystander.move(action);
                if (cell(next) != '#' && cell(next) != 'L' && !next.equals(projectedAgent) && !next.equals(projectedFragile)) {
                    exits++;
                }
            }
            return exits == 0;
        }

        private char cell(Pos pos) {
            if (pos.y < 0 || pos.y >= terrain.length || pos.x < 0 || pos.x >= terrain[0].length) {
                return '#';
            }
            return terrain[pos.y][pos.x];
        }

        private String llmStatusForTick(int currentTick) {
            String mode = config.llm == null || config.llm.mode == null ? "disabled" : config.llm.mode;
            boolean slowTick = currentTick % Math.max(1, config.slowFastRatio) == 0;
            if (!slowTick) {
                return "NOT_SLOW_TICK";
            }
            if ("disabled".equalsIgnoreCase(mode)) {
                return "DISABLED";
            }
            if ("mock".equalsIgnoreCase(mode)) {
                if (llmCalls++ == 0) {
                    return "MOCK_ADVISORY_VERIFIED";
                }
                return "TIMEOUT_FALLBACK";
            }
            if ("ollama".equalsIgnoreCase(mode)) {
                llmCalls++;
                return "OLLAMA_DISABLED_IN_TEST_FALLBACK";
            }
            return "DISABLED";
        }

        private Map<String, Object> resultRow(int currentTick, boolean slowTick, String llmStatus,
                                              List<Candidate> candidates, Candidate selected, Candidate executed,
                                              String harmVerdict, String vetoReason, String welfareDimension,
                                              LoopDecision loopDecision) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("tick", currentTick);
            row.put("fastTick", currentTick);
            row.put("slowTick", slowTick ? currentTick / Math.max(1, config.slowFastRatio) : null);
            row.put("timestamp", 1_700_000_000_000L + currentTick);
            row.put("scenario", config.scenarioId);
            row.put("seed", config.seed);
            row.put("agent", agent.toMap());
            row.put("candidateActions", candidates.stream().map(Candidate::toMap).toList());
            row.put("selectedAction", selected.action.name());
            row.put("executedAction", executed.action.name());
            row.put("reward", round(reward));
            row.put("energy", round(energy));
            row.put("harmVerdict", harmVerdict);
            row.put("vetoReason", vetoReason);
            row.put("welfareDimension", welfareDimension);
            row.put("loopIntervention", loopDecision.intervened ? loopDecision.reason : null);
            row.put("llmStatus", llmStatus);
            row.put("fastDurationMs", round(2.0 + candidates.size() * 0.1));
            row.put("simOnly", true);
            row.put("harmEvaluationsCount", candidates.size());
            row.put("signals", List.of(
                    "SensorySignal", "FeatureSignal", "AttentionGateSignal", "WorkingMemoryWriteSignal",
                    "PredictionSignal", "CandidateActionSignal", "ActionSelectionSignal",
                    "ConsequenceSimulationSignal", "HarmAssessmentSignal", "TransparencyLogSignal",
                    slowTick ? "NeuromodulatorSignal" : "FastLoopOnlySignal",
                    loopDecision.intervened ? "LoopInterventionSignal" : "LoopMonitorSignal"));
            return row;
        }

        private List<Map<String, Object>> transparencyRows(int currentTick, Action selectedAction, Action executedAction,
                                                           List<Candidate> candidates) {
            List<Map<String, Object>> rows = new ArrayList<>();
            for (Candidate candidate : candidates) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("tick", currentTick);
                row.put("candidateAction", candidate.action.name());
                row.put("selectedAction", selectedAction.name());
                row.put("chosenAction", executedAction.name());
                row.put("verdict", candidate.harm.verdict);
                row.put("reason", candidate.harm.reason);
                row.put("welfareDimension", candidate.harm.dimension);
                row.put("projectedHarmScore", round(candidate.harm.projectedHarmScore));
                row.put("preExecution", true);
                row.put("consequenceModel", "ConsequenceModelNeuron");
                rows.add(row);
            }
            return rows;
        }

        private Map<String, Object> worldRow(int currentTick) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("tick", currentTick);
            row.put("agent", agent.toMap());
            row.put("bystander", bystander == null ? null : bystander.toMap());
            row.put("fragileObject", fragile == null ? null : fragile.toMap());
            row.put("foodsRemaining", foods.size());
            row.put("grid", renderGrid());
            row.put("bystanderUnharmed", bystanderUnharmed);
            row.put("lavaEntered", lavaEntered);
            return row;
        }

        private Map<String, Object> llmRow(int currentTick, String llmStatus, boolean slowTick) {
            if (!slowTick || "DISABLED".equals(llmStatus) || "NOT_SLOW_TICK".equals(llmStatus)) {
                return null;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("tick", currentTick);
            row.put("mode", config.llm.mode);
            row.put("status", llmStatus);
            row.put("verified", "MOCK_ADVISORY_VERIFIED".equals(llmStatus));
            row.put("loadBearing", false);
            row.put("signal", "LLMFallbackNeuron");
            row.put("advisory", "MOCK_ADVISORY_VERIFIED".equals(llmStatus)
                    ? "prefer safe path only if already approved by HarmGateNeuron"
                    : "fallback: continue fast-loop policy without LLM");
            return row;
        }

        private void writeRecord(TickRecord record) {
            appendJsonLine(outputDir.resolve("results.jsonl"), record.resultRow);
            for (Map<String, Object> row : record.transparencyRows) {
                appendJsonLine(outputDir.resolve("transparency.jsonl"), row);
            }
            appendJsonLine(outputDir.resolve("world_trace.jsonl"), record.worldRow);
            if (record.loopRow != null) {
                appendJsonLine(outputDir.resolve("loop_interventions.jsonl"), record.loopRow);
            }
            if (record.llmRow != null) {
                appendJsonLine(outputDir.resolve("optional_llm_advisory.jsonl"), record.llmRow);
            }
        }

        private void writeSafetySummary() {
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("scenario", config.scenarioId);
            summary.put("ticks", rowsWritten);
            summary.put("approved", approved);
            summary.put("vetoed", vetoed);
            summary.put("replaced", replaced);
            summary.put("bystanderUnharmed", bystanderUnharmed);
            summary.put("lavaEntered", lavaEntered);
            summary.put("hardConstraints", config.safety.hardConstraints);
            summary.put("harmGateEnabled", config.safety.harmGateEnabled);
            summary.put("physicalIntegrityThreshold",
                    config.safety.hardVetoThresholds.get("physicalIntegrity"));
            summary.put("loopAlerts", loopAlerts);
            summary.put("loopInterventions", loopInterventions);
            summary.put("loopRecoveries", loopRecoveries);
            writeJson(outputDir.resolve("safety_summary.json"), summary);
        }

        private List<String> renderGrid() {
            List<String> rows = new ArrayList<>();
            for (int y = 0; y < terrain.length; y++) {
                StringBuilder row = new StringBuilder();
                for (int x = 0; x < terrain[y].length; x++) {
                    Pos pos = new Pos(x, y);
                    if (pos.equals(agent)) {
                        row.append('A');
                    } else if (pos.equals(bystander)) {
                        row.append('B');
                    } else if (pos.equals(fragile)) {
                        row.append('O');
                    } else if (foods.contains(pos)) {
                        row.append('F');
                    } else {
                        row.append(terrain[y][x]);
                    }
                }
                rows.add(row.toString());
            }
            return rows;
        }

        private String localPatch() {
            List<String> rows = renderGrid();
            StringBuilder patch = new StringBuilder();
            for (int y = Math.max(0, agent.y - 1); y <= Math.min(rows.size() - 1, agent.y + 1); y++) {
                int start = Math.max(0, agent.x - 1);
                int end = Math.min(rows.get(y).length(), agent.x + 2);
                if (!patch.isEmpty()) {
                    patch.append('/');
                }
                patch.append(rows.get(y), start, end);
            }
            return patch.toString();
        }
    }

    private static final class LoopDecision {
        final boolean intervened;
        final String reason;
        final String intervention;
        final String blockedAction;
        final boolean recovery;

        LoopDecision(boolean intervened, String reason, String intervention, String blockedAction, boolean recovery) {
            this.intervened = intervened;
            this.reason = reason;
            this.intervention = intervention;
            this.blockedAction = blockedAction;
            this.recovery = recovery;
        }

        static LoopDecision none() {
            return new LoopDecision(false, null, null, null, false);
        }

        Map<String, Object> loopRow(int tick) {
            if (!intervened && !recovery) {
                return null;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("tick", tick);
            row.put("loopAlertSignal", intervened);
            row.put("loopInterventionSignal", intervened);
            row.put("loopRecoverySignal", recovery);
            row.put("intervention", intervention);
            row.put("blockedAction", blockedAction);
            row.put("reason", reason);
            return row;
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
            throw new IllegalStateException("Cannot append autonomous AI JSONL " + path, e);
        }
    }

    static void writeJson(Path path, Map<String, Object> row) {
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), row);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot write autonomous AI JSON " + path, e);
        }
    }

    static String toJson(Map<String, Object> row) {
        try {
            return MAPPER.writeValueAsString(row);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize autonomous AI row", e);
        }
    }

    private static double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }
}
