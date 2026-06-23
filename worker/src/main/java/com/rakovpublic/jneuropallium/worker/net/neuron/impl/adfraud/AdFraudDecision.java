/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.adfraud;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AdFraudDecision {
    private String eventId;
    private String modelVersion;
    private AdFraudRuntimeMode mode = AdFraudRuntimeMode.ADVISORY;
    private Map<String, Double> probabilities = new LinkedHashMap<>();
    private double overallInvalidTrafficProbability;
    private double expectedFinancialLoss;
    private double uncertainty;
    private double evidenceCompleteness;
    private AdFraudResponseAction recommendedAction = AdFraudResponseAction.MONITOR;
    private List<String> reasons = new ArrayList<>();
    private boolean duplicateEvent;
    private boolean mlFallbackUsed;

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }
    public AdFraudRuntimeMode getMode() { return mode; }
    public void setMode(AdFraudRuntimeMode mode) { this.mode = mode == null ? AdFraudRuntimeMode.ADVISORY : mode; }
    public Map<String, Double> getProbabilities() { return probabilities; }
    public void setProbabilities(Map<String, Double> probabilities) { this.probabilities = probabilities == null ? new LinkedHashMap<>() : probabilities; }
    public double getOverallInvalidTrafficProbability() { return overallInvalidTrafficProbability; }
    public void setOverallInvalidTrafficProbability(double v) { this.overallInvalidTrafficProbability = clamp(v); }
    public double getExpectedFinancialLoss() { return expectedFinancialLoss; }
    public void setExpectedFinancialLoss(double expectedFinancialLoss) { this.expectedFinancialLoss = Math.max(0.0, expectedFinancialLoss); }
    public double getUncertainty() { return uncertainty; }
    public void setUncertainty(double uncertainty) { this.uncertainty = clamp(uncertainty); }
    public double getEvidenceCompleteness() { return evidenceCompleteness; }
    public void setEvidenceCompleteness(double evidenceCompleteness) { this.evidenceCompleteness = clamp(evidenceCompleteness); }
    public AdFraudResponseAction getRecommendedAction() { return recommendedAction; }
    public void setRecommendedAction(AdFraudResponseAction recommendedAction) { this.recommendedAction = recommendedAction == null ? AdFraudResponseAction.MONITOR : recommendedAction; }
    public List<String> getReasons() { return reasons; }
    public void setReasons(List<String> reasons) { this.reasons = reasons == null ? new ArrayList<>() : reasons; }
    public boolean isDuplicateEvent() { return duplicateEvent; }
    public void setDuplicateEvent(boolean duplicateEvent) { this.duplicateEvent = duplicateEvent; }
    public boolean isMlFallbackUsed() { return mlFallbackUsed; }
    public void setMlFallbackUsed(boolean mlFallbackUsed) { this.mlFallbackUsed = mlFallbackUsed; }

    public void addReason(String reason) {
        if (reason != null && !reason.isBlank() && !reasons.contains(reason)) reasons.add(reason);
    }

    private static double clamp(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) return 0.0;
        return Math.max(0.0, Math.min(1.0, value));
    }
}
