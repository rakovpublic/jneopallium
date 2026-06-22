package com.rakovpublic.jneuropallium.worker.demo.uavsingle.movement;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Fast-loop harm output emitted by the movement harm-gate neuron for one candidate action. */
public final class MovementHarmAssessment {
    private final String actionId;
    private final double risk;
    private final boolean vetoed;
    private final String reason;
    private final double confidence;
    private final List<String> triggeringConditions;

    public MovementHarmAssessment(String actionId, double risk, boolean vetoed, String reason,
                                  double confidence, List<String> triggeringConditions) {
        this.actionId = actionId;
        this.risk = clamp01(risk);
        this.vetoed = vetoed;
        this.reason = reason == null ? "none" : reason;
        this.confidence = clamp01(confidence);
        this.triggeringConditions = triggeringConditions == null
                ? new ArrayList<>() : new ArrayList<>(triggeringConditions);
    }

    public static MovementHarmAssessment safe(String actionId) {
        return new MovementHarmAssessment(actionId, 0.0, false, "clear", 1.0, List.of());
    }

    public String getActionId() { return actionId; }
    public double getRisk() { return risk; }
    public boolean isVetoed() { return vetoed; }
    public String getReason() { return reason; }
    public double getConfidence() { return confidence; }
    public List<String> getTriggeringConditions() { return new ArrayList<>(triggeringConditions); }

    public Map<String, Object> asMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("actionId", actionId);
        map.put("risk", round(risk));
        map.put("vetoed", vetoed);
        map.put("reason", reason);
        map.put("confidence", round(confidence));
        map.put("triggeringConditions", new ArrayList<>(triggeringConditions));
        return map;
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static double round(double value) {
        return Math.round(value * 1_000_000.0) / 1_000_000.0;
    }
}
