package com.rakovpublic.jneuropallium.worker.demo.uavsingle.movement;

import com.rakovpublic.jneuropallium.worker.demo.uavsingle.UavSingleSignal;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Reward feedback routed to {@link IMovementLearningNeuron}. The network keeps the last decision,
 * so a reinforcement signal only needs the scalar reward plus a reason and optional context extras;
 * the learning processor applies it to the action neuron that produced that decision.
 */
public class MovementReinforcementSignal extends UavSingleSignal {
    private long frame;
    private double reward;
    private String reason = "MOVEMENT_REINFORCEMENT";
    private final Map<String, Object> extras = new LinkedHashMap<>();

    public MovementReinforcementSignal() {
        setEventType("MOVEMENT_REINFORCEMENT");
    }

    public MovementReinforcementSignal(long frame, double reward, String reason) {
        this();
        this.frame = frame;
        this.reward = reward;
        if (reason != null) {
            this.reason = reason;
        }
    }

    public long getFrame() { return frame; }
    public void setFrame(long frame) { this.frame = frame; }
    public double getReward() { return reward; }
    public void setReward(double reward) { this.reward = reward; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason == null ? this.reason : reason; }
    public Map<String, Object> getExtras() { return extras; }

    public MovementReinforcementSignal withExtra(String key, Object value) {
        extras.put(key, value);
        return this;
    }
}
