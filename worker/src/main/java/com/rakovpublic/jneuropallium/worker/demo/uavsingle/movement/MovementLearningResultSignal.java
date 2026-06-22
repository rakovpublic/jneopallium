package com.rakovpublic.jneuropallium.worker.demo.uavsingle.movement;

import com.rakovpublic.jneuropallium.worker.demo.uavsingle.UavSingleSignal;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Outcome of applying a {@link MovementReinforcementSignal}: which action neuron was updated, the
 * per-dendrite deltas and the new bias. Mirrors {@code RecognitionLearningResultSignal}.
 */
public class MovementLearningResultSignal extends UavSingleSignal {
    private long frame;
    private boolean applied;
    private String reinforcedNeuronId;
    private String actionId;
    private double reward;
    private double updatedBias;
    private String reason = "MOVEMENT_POLICY_UPDATED";
    private final Map<String, Double> updatedDendrites = new LinkedHashMap<>();
    private final Map<String, Object> extras = new LinkedHashMap<>();

    public MovementLearningResultSignal() {
        setEventType("MOVEMENT_LEARNING_RESULT");
    }

    public long getFrame() { return frame; }
    public void setFrame(long frame) { this.frame = frame; }
    public boolean isApplied() { return applied; }
    public void setApplied(boolean applied) { this.applied = applied; }
    public String getReinforcedNeuronId() { return reinforcedNeuronId; }
    public void setReinforcedNeuronId(String reinforcedNeuronId) { this.reinforcedNeuronId = reinforcedNeuronId; }
    public String getActionId() { return actionId; }
    public void setActionId(String actionId) { this.actionId = actionId; }
    public double getReward() { return reward; }
    public void setReward(double reward) { this.reward = reward; }
    public double getUpdatedBias() { return updatedBias; }
    public void setUpdatedBias(double updatedBias) { this.updatedBias = updatedBias; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason == null ? this.reason : reason; }
    public Map<String, Double> getUpdatedDendrites() { return updatedDendrites; }
    public Map<String, Object> getExtras() { return extras; }

    public Map<String, Object> asMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("frame", frame);
        map.put("applied", applied);
        map.put("reinforcedNeuronId", reinforcedNeuronId);
        map.put("actionId", actionId);
        map.put("reward", reward);
        map.put("updatedBias", updatedBias);
        map.put("learningReason", reason);
        map.put("updatedDendrites", updatedDendrites);
        map.putAll(extras);
        return map;
    }
}
