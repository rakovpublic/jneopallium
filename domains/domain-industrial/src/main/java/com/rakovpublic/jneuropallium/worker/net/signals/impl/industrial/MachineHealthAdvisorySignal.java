/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Advisory-only machine-health output for pumps, fans, valves, motors, and bearings. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MachineHealthAdvisorySignal extends AbstractSignal<Map<String, Object>>
        implements ISignal<Map<String, Object>>, IResultSignal<Map<String, Object>> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(5L, 2);

    private String assetId;
    private String modelVersion;
    private String mode;
    private double healthScore;
    private double anomalyProbability;
    private Map<String, Double> faultProbabilities;
    private double unknownAnomalyProbability;
    private double domainShiftScore;
    private double uncertainty;
    private String recommendedAction;
    private List<String> evidence;
    private boolean autonomousAction;
    private long timestamp;

    public MachineHealthAdvisorySignal() {
        super();
        this.loop = 2;
        this.epoch = 5L;
        this.timeAlive = 0;
        this.modelVersion = "1.0.0-machine-health";
        this.mode = "ADVISORY";
        this.recommendedAction = "MONITOR";
        this.faultProbabilities = new LinkedHashMap<>();
        this.evidence = new ArrayList<>();
    }

    public MachineHealthAdvisorySignal(String assetId, String modelVersion, String mode,
                                       double healthScore, double anomalyProbability,
                                       Map<String, Double> faultProbabilities,
                                       double unknownAnomalyProbability, double domainShiftScore,
                                       double uncertainty, String recommendedAction,
                                       List<String> evidence, boolean autonomousAction,
                                       long timestamp) {
        this();
        this.assetId = assetId;
        this.modelVersion = modelVersion == null ? this.modelVersion : modelVersion;
        this.mode = mode == null ? "ADVISORY" : mode;
        this.healthScore = clamp(healthScore);
        this.anomalyProbability = clamp(anomalyProbability);
        setFaultProbabilities(faultProbabilities);
        this.unknownAnomalyProbability = clamp(unknownAnomalyProbability);
        this.domainShiftScore = clamp(domainShiftScore);
        this.uncertainty = clamp(uncertainty);
        this.recommendedAction = recommendedAction == null ? "MONITOR" : recommendedAction;
        setEvidence(evidence);
        this.autonomousAction = autonomousAction;
        this.timestamp = timestamp;
    }

    public String getAssetId() { return assetId; }
    public void setAssetId(String assetId) { this.assetId = assetId; }
    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public double getHealthScore() { return healthScore; }
    public void setHealthScore(double healthScore) { this.healthScore = clamp(healthScore); }
    public double getAnomalyProbability() { return anomalyProbability; }
    public void setAnomalyProbability(double anomalyProbability) {
        this.anomalyProbability = clamp(anomalyProbability);
    }
    public Map<String, Double> getFaultProbabilities() { return Collections.unmodifiableMap(faultProbabilities); }
    public void setFaultProbabilities(Map<String, Double> faultProbabilities) {
        this.faultProbabilities = new LinkedHashMap<>();
        if (faultProbabilities == null) return;
        for (Map.Entry<String, Double> entry : faultProbabilities.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                this.faultProbabilities.put(entry.getKey(), clamp(entry.getValue()));
            }
        }
    }
    public double getUnknownAnomalyProbability() { return unknownAnomalyProbability; }
    public void setUnknownAnomalyProbability(double unknownAnomalyProbability) {
        this.unknownAnomalyProbability = clamp(unknownAnomalyProbability);
    }
    public double getDomainShiftScore() { return domainShiftScore; }
    public void setDomainShiftScore(double domainShiftScore) { this.domainShiftScore = clamp(domainShiftScore); }
    public double getUncertainty() { return uncertainty; }
    public void setUncertainty(double uncertainty) { this.uncertainty = clamp(uncertainty); }
    public String getRecommendedAction() { return recommendedAction; }
    public void setRecommendedAction(String recommendedAction) { this.recommendedAction = recommendedAction; }
    public List<String> getEvidence() { return Collections.unmodifiableList(evidence); }
    public void setEvidence(List<String> evidence) {
        this.evidence = new ArrayList<>();
        if (evidence != null) {
            for (String item : evidence) {
                if (item != null && !item.isBlank()) this.evidence.add(item);
            }
        }
    }
    public boolean isAutonomousAction() { return autonomousAction; }
    public void setAutonomousAction(boolean autonomousAction) { this.autonomousAction = autonomousAction; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    @Override public Map<String, Object> getValue() { return getResultObject(); }
    @Override public Class<Map<String, Object>> getParamClass() { return resultClass(); }
    @Override public Map<String, Object> getResultObject() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("assetId", assetId);
        result.put("modelVersion", modelVersion);
        result.put("mode", mode);
        result.put("healthScore", healthScore);
        result.put("anomalyProbability", anomalyProbability);
        result.put("faultProbabilities", new LinkedHashMap<>(faultProbabilities));
        result.put("unknownAnomalyProbability", unknownAnomalyProbability);
        result.put("domainShiftScore", domainShiftScore);
        result.put("uncertainty", uncertainty);
        result.put("recommendedAction", recommendedAction);
        result.put("evidence", new ArrayList<>(evidence));
        result.put("autonomousAction", autonomousAction);
        result.put("timestamp", timestamp);
        return result;
    }
    @Override public Class<Map<String, Object>> getResultObjectClass() { return resultClass(); }
    @Override public Class<? extends ISignal<Map<String, Object>>> getCurrentSignalClass() {
        return MachineHealthAdvisorySignal.class;
    }
    @Override public String getDescription() { return "MachineHealthAdvisorySignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Map<String, Object>>> K copySignal() {
        MachineHealthAdvisorySignal c = new MachineHealthAdvisorySignal(assetId, modelVersion,
                mode, healthScore, anomalyProbability, faultProbabilities,
                unknownAnomalyProbability, domainShiftScore, uncertainty,
                recommendedAction, evidence, autonomousAction, timestamp);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }

    @SuppressWarnings("unchecked")
    private static Class<Map<String, Object>> resultClass() {
        return (Class<Map<String, Object>>) (Class<?>) Map.class;
    }

    private static double clamp(double value) {
        if (!Double.isFinite(value)) return 0.0;
        return Math.max(0.0, Math.min(1.0, value));
    }
}
