package com.rakovpublic.jneuropallium.worker.demo.uavsingle.movement;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Temporal action-selection neuron for one discrete movement action.
 *
 * <p>Processor features are dendrite inputs. Fast harm assessment is a veto input. Dopamine and
 * acetylcholine timing decide whether the chosen action learns from the outcome or receives only a
 * transient vigor boost. This keeps movement policy state inside neurons instead of a separate
 * policy-scoring class.
 */
public class MovementActionNeuron extends ModulatableNeuron {
    private static final double WEIGHT_LIMIT = 6.0;
    private static final double BIAS_LIMIT = 3.0;
    private static final double ACTION_VALUE_LIMIT = 4.0;
    private static final double TRACE_LIMIT = 4.0;
    private static final double VETO_SCORE = -1_000_000.0;

    private final MovementAction action;
    private final Map<String, Double> weights = new LinkedHashMap<>();
    private double bias;
    private int reinforcementUpdates;

    private double fastEvidence;
    private double eligibilityTrace;
    private double learnedActionValue;
    private double phasicVigor;
    private double fatigue;
    private double lastLearningGate;
    private double lastVigorGate;
    private double lastScore;
    private double lastHarmRisk;
    private boolean lastVetoed;
    private String lastVetoReason = "clear";
    private long lastActivationFrame = -1L;
    private long lastSelectedFrame = -1L;
    private long lastAchDipFrame = Long.MIN_VALUE;
    private long lastAchBurstFrame = Long.MIN_VALUE;
    private long lastDopamineFrame = Long.MIN_VALUE;
    private MovementHarmAssessment lastHarmAssessment = MovementHarmAssessment.safe("none");

    public MovementActionNeuron(MovementAction action, Map<String, Double> weights, double bias) {
        super();
        this.currentNeuronClass = MovementActionNeuron.class;
        this.action = action;
        if (weights != null) {
            this.weights.putAll(weights);
        }
        this.bias = bias;
    }

    public MovementAction getAction() { return action; }
    public Map<String, Double> getWeights() { return weights; }
    public double getBias() { return bias; }
    public void setBias(double bias) { this.bias = clamp(bias, BIAS_LIMIT); }
    public int getReinforcementUpdates() { return reinforcementUpdates; }
    public double getFastEvidence() { return fastEvidence; }
    public double getEligibilityTrace() { return eligibilityTrace; }
    public double getLearnedActionValue() { return learnedActionValue; }
    public double getLastScore() { return lastScore; }
    public double getLastHarmRisk() { return lastHarmRisk; }
    public boolean isLastVetoed() { return lastVetoed; }
    public String getLastVetoReason() { return lastVetoReason; }
    public MovementHarmAssessment getLastHarmAssessment() { return lastHarmAssessment; }

    /**
     * Activates this action neuron from current feature dendrites and fast harm-veto input.
     * Harm veto is evaluated before dopamine/vigor, so unsafe actions cannot be selected by reward.
     */
    public double activate(Map<String, Double> features, MovementHarmAssessment harm,
                           MovementRuntimeConfig config, long frame) {
        lastActivationFrame = frame;
        lastHarmAssessment = harm == null ? MovementHarmAssessment.safe(action.getActionId()) : harm;
        lastHarmRisk = lastHarmAssessment.getRisk();
        lastVetoed = lastHarmAssessment.isVetoed();
        lastVetoReason = lastHarmAssessment.getReason();

        fastEvidence = clamp(0.52 * fastEvidence + dendritePotential(features), 12.0);
        eligibilityTrace *= 0.84;
        phasicVigor *= 0.58;
        fatigue = clampPositive(fatigue * 0.92 - 0.015, 3.0);
        double vigorMultiplier = 1.0 + 0.35 * lastVigorGate + phasicVigor;
        double harmInhibition = lastHarmRisk * 7.0 + getInhibitionLevel();
        lastScore = bias + learnedActionValue + fastEvidence * vigorMultiplier - harmInhibition - fatigue;
        if (lastVetoed) {
            lastScore = VETO_SCORE - lastHarmRisk;
        }
        setChanged(lastVetoed);
        return lastScore;
    }

    /** Compatibility path for older callers. Prefer {@link #activate}. */
    public double score(Map<String, Double> features) {
        return activate(features, MovementHarmAssessment.safe(action.getActionId()),
                MovementRuntimeConfig.defaults(), lastActivationFrame + 1L);
    }

    public void markSelected(long frame) {
        lastSelectedFrame = frame;
        eligibilityTrace = clampPositive(eligibilityTrace + 1.0 + Math.max(0.0, fastEvidence) * 0.03,
                TRACE_LIMIT);
        fatigue = clampPositive(fatigue + 0.05, 3.0);
        setChanged(true);
    }

    public void receiveAcetylcholine(AcetylcholinePhaseSignal signal) {
        if (signal == null) {
            return;
        }
        setAchLevel(signal.getIntensity());
        if (signal.getPhase() == AcetylcholinePhaseSignal.Phase.DIP) {
            lastAchDipFrame = signal.getTick();
        } else if (signal.getPhase() == AcetylcholinePhaseSignal.Phase.BURST) {
            lastAchBurstFrame = signal.getTick();
        }
    }

    public void receiveHomeostasis(HomeostasisSignal signal) {
        if (signal == null) {
            return;
        }
        fatigue = clampPositive(0.8 * fatigue + 0.2 * Math.max(0.0, signal.getFatigue()), 3.0);
        inhibitionLevel = clampPositive(0.8 * inhibitionLevel + 0.2 * Math.max(0.0, signal.getStress()), 3.0);
        norepinephrineLevel = 1.0 + Math.max(0.0, signal.getExplorationPressure());
    }

    /**
     * Reinforces the selected neuron using ACh/DA temporal gates.
     *
     * <p>ACh dip before dopamine opens the learning gate. ACh burst coincident with dopamine opens
     * the vigor gate. Negative reward always opens learning so harm and missed movement are learned.
     */
    public Map<String, Double> reinforce(Map<String, Double> features, double reward, double learningRate,
                                         long frame, String reason, Map<String, Object> extras) {
        long achFrame = Math.max(0L, frame - 1L);
        if (opensLearningGate(reward, extras)) {
            receiveAcetylcholine(new AcetylcholinePhaseSignal(AcetylcholinePhaseSignal.Phase.DIP,
                    Math.min(1.0, Math.abs(reward)), achFrame));
        } else if (reward > 0.0) {
            receiveAcetylcholine(new AcetylcholinePhaseSignal(AcetylcholinePhaseSignal.Phase.BURST,
                    Math.min(1.0, reward), frame));
        } else {
            receiveAcetylcholine(new AcetylcholinePhaseSignal(AcetylcholinePhaseSignal.Phase.BASELINE, 0.0, frame));
        }
        return receiveDopamine(new PhasicDopamineSignal(reward, reward, frame, reason), features, learningRate);
    }

    /** Compatibility path for older callers. */
    public Map<String, Double> reinforce(Map<String, Double> features, double reward, double learningRate) {
        return reinforce(features, reward, learningRate,
                Math.max(lastSelectedFrame + 1L, lastActivationFrame + 1L),
                "MOVEMENT_REWARD", Map.of());
    }

    public Map<String, Double> receiveDopamine(PhasicDopamineSignal signal, Map<String, Double> features,
                                               double learningRate) {
        Map<String, Double> deltas = new LinkedHashMap<>();
        if (signal == null) {
            return deltas;
        }
        long tick = signal.getTick();
        double predictionError = signal.getPredictionError();
        setDopamineLevel(1.0 + signal.getConcentration());
        lastDopamineFrame = tick;
        boolean dipBeforeDopamine = lastAchDipFrame <= tick && tick - lastAchDipFrame <= 4L;
        boolean burstAtDopamine = Math.abs(tick - lastAchBurstFrame) <= 1L;
        lastLearningGate = dipBeforeDopamine || predictionError < 0.0 ? 1.0 : 0.18;
        lastVigorGate = burstAtDopamine && predictionError > 0.0 ? 1.0 : 0.0;

        if (lastVigorGate > 0.0) {
            phasicVigor = clampPositive(phasicVigor + Math.min(0.55, predictionError * 0.18), 1.2);
        }
        double trace = Math.max(0.15, eligibilityTrace);
        double gatedRate = learningRate * lastLearningGate * trace;
        for (Map.Entry<String, Double> entry : features.entrySet()) {
            double delta = gatedRate * predictionError * entry.getValue();
            double updated = clamp(weights.getOrDefault(entry.getKey(), 0.0) + delta, WEIGHT_LIMIT);
            weights.put(entry.getKey(), updated);
            deltas.put(entry.getKey(), round(delta));
        }
        double valueDelta = learningRate * predictionError * trace;
        learnedActionValue = clamp(0.97 * learnedActionValue + valueDelta, ACTION_VALUE_LIMIT);
        bias = clamp(bias + valueDelta * 0.08, BIAS_LIMIT);
        eligibilityTrace = clampPositive(eligibilityTrace * 0.72, TRACE_LIMIT);
        reinforcementUpdates++;
        setChanged(true);
        return deltas;
    }

    public void resetEpisodeState() {
        fastEvidence = 0.0;
        eligibilityTrace = 0.0;
        phasicVigor = 0.0;
        fatigue = 0.0;
        lastLearningGate = 0.0;
        lastVigorGate = 0.0;
        lastScore = 0.0;
        lastHarmRisk = 0.0;
        lastVetoed = false;
        lastVetoReason = "clear";
        lastActivationFrame = -1L;
        lastSelectedFrame = -1L;
        lastAchDipFrame = Long.MIN_VALUE;
        lastAchBurstFrame = Long.MIN_VALUE;
        lastDopamineFrame = Long.MIN_VALUE;
        lastHarmAssessment = MovementHarmAssessment.safe(action.getActionId());
        setChanged(false);
    }

    public void restoreLearnedState(Map<String, ?> neuronMap) {
        Object temporal = neuronMap.get("temporalState");
        if (temporal instanceof Map<?, ?> state) {
            learnedActionValue = number((Map<?, ?>) state, "learnedActionValue", learnedActionValue);
            fastEvidence = number((Map<?, ?>) state, "fastEvidence", fastEvidence);
            eligibilityTrace = number((Map<?, ?>) state, "eligibilityTrace", eligibilityTrace);
            fatigue = number((Map<?, ?>) state, "fatigue", fatigue);
        }
    }

    public Map<String, Object> snapshot(List<IMovementSignalProcessor> processors) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("neuronId", neuronName());
        map.put("neuronModel", "temporal-action-selection-perceptron");
        map.put("actionId", action.getActionId());
        map.put("actionLabel", action.getLabel());
        map.put("movementAction", action.asModelMap());
        List<String> interfaces = new ArrayList<>();
        List<Map<String, String>> processorDescriptors = new ArrayList<>();
        for (IMovementSignalProcessor processor : processors) {
            if (!interfaces.contains(processor.interfaceName())) {
                interfaces.add(processor.interfaceName());
            }
            Map<String, String> descriptor = new LinkedHashMap<>();
            descriptor.put("processorId", processor.processorId());
            descriptor.put("interface", processor.interfaceName());
            descriptor.put("feature", processor.featureName());
            processorDescriptors.add(descriptor);
        }
        interfaces.add("FastMovementHarmVeto");
        interfaces.add("PhasicDopamineSignal");
        interfaces.add("AcetylcholinePhaseSignal");
        interfaces.add("HomeostasisSignal");
        map.put("interfaces", interfaces);
        map.put("processors", processorDescriptors);
        Map<String, Object> dendrites = new LinkedHashMap<>();
        Map<String, Object> roundedWeights = new LinkedHashMap<>();
        weights.forEach((key, value) -> roundedWeights.put(key, round(value)));
        dendrites.put("weights", roundedWeights);
        dendrites.put("fastHarmVetoInput", "movement-harm-gate-lidar-fast-loop");
        map.put("dendrites", dendrites);
        map.put("axon", Map.of("motorActionId", action.getActionId(), "loop", 1));
        map.put("bias", round(bias));
        Map<String, Object> temporal = new LinkedHashMap<>();
        temporal.put("fastEvidence", round(fastEvidence));
        temporal.put("eligibilityTrace", round(eligibilityTrace));
        temporal.put("learnedActionValue", round(learnedActionValue));
        temporal.put("phasicVigor", round(phasicVigor));
        temporal.put("fatigue", round(fatigue));
        temporal.put("lastLearningGate", round(lastLearningGate));
        temporal.put("lastVigorGate", round(lastVigorGate));
        temporal.put("lastScore", round(lastScore));
        temporal.put("lastDopamineFrame", lastDopamineFrame);
        map.put("temporalState", temporal);
        Map<String, Object> harm = new LinkedHashMap<>();
        harm.put("lastRisk", round(lastHarmRisk));
        harm.put("lastVetoed", lastVetoed);
        harm.put("lastReason", lastVetoReason);
        harm.put("lastAssessment", lastHarmAssessment.asMap());
        map.put("harmGate", harm);
        map.put("reinforcementUpdates", reinforcementUpdates);
        return map;
    }

    public String neuronName() {
        return "movement-neuron-" + action.getActionId();
    }

    private double dendritePotential(Map<String, Double> features) {
        double sum = 0.0;
        for (Map.Entry<String, Double> entry : features.entrySet()) {
            sum += weights.getOrDefault(entry.getKey(), 0.0) * entry.getValue();
        }
        return sum;
    }

    private static boolean opensLearningGate(double reward, Map<String, Object> extras) {
        if (reward < 0.0) {
            return true;
        }
        if (extras == null) {
            return reward > 0.25;
        }
        return intExtra(extras, "newPhotos") > 0
                || intExtra(extras, "newCoverageCells") > 0
                || intExtra(extras, "recentOcclusionSignals") > 0
                || reward > 0.45;
    }

    private static int intExtra(Map<String, Object> extras, String key) {
        Object value = extras.get(key);
        return value instanceof Number number ? number.intValue() : 0;
    }

    private static double number(Map<?, ?> map, String key, double fallback) {
        Object value = map.get(key);
        return value instanceof Number number ? number.doubleValue() : fallback;
    }

    private static double clamp(double value, double limit) {
        return Math.max(-limit, Math.min(limit, value));
    }

    private static double clampPositive(double value, double limit) {
        return Math.max(0.0, Math.min(limit, value));
    }

    private static double round(double value) {
        return Math.round(value * 1_000_000.0) / 1_000_000.0;
    }
}
