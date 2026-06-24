/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Probabilistic machine-fault hypotheses emitted before advisory gating. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FaultHypothesisSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(4L, 2);

    private String assetId;
    private Map<String, Double> faultProbabilities;
    private double unknownAnomalyProbability;
    private double anomalyProbability;
    private double domainShiftScore;
    private double uncertainty;
    private List<String> evidence;
    private long timestamp;

    public FaultHypothesisSignal() {
        super();
        this.loop = 2;
        this.epoch = 4L;
        this.timeAlive = 0;
        this.faultProbabilities = new LinkedHashMap<>();
        this.evidence = new ArrayList<>();
    }

    public FaultHypothesisSignal(String assetId, Map<String, Double> faultProbabilities,
                                 double unknownAnomalyProbability, double anomalyProbability,
                                 double domainShiftScore, double uncertainty,
                                 List<String> evidence, long timestamp) {
        this();
        this.assetId = assetId;
        setFaultProbabilities(faultProbabilities);
        this.unknownAnomalyProbability = clamp(unknownAnomalyProbability);
        this.anomalyProbability = clamp(anomalyProbability);
        this.domainShiftScore = clamp(domainShiftScore);
        this.uncertainty = clamp(uncertainty);
        setEvidence(evidence);
        this.timestamp = timestamp;
    }

    public String getAssetId() { return assetId; }
    public void setAssetId(String assetId) { this.assetId = assetId; }
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
    public double getAnomalyProbability() { return anomalyProbability; }
    public void setAnomalyProbability(double anomalyProbability) { this.anomalyProbability = clamp(anomalyProbability); }
    public double getDomainShiftScore() { return domainShiftScore; }
    public void setDomainShiftScore(double domainShiftScore) { this.domainShiftScore = clamp(domainShiftScore); }
    public double getUncertainty() { return uncertainty; }
    public void setUncertainty(double uncertainty) { this.uncertainty = clamp(uncertainty); }
    public List<String> getEvidence() { return Collections.unmodifiableList(evidence); }
    public void setEvidence(List<String> evidence) {
        this.evidence = new ArrayList<>();
        if (evidence != null) {
            for (String item : evidence) {
                if (item != null && !item.isBlank()) this.evidence.add(item);
            }
        }
    }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String mostLikelyFault() {
        String fault = "UNKNOWN_ANOMALY";
        double best = unknownAnomalyProbability;
        for (Map.Entry<String, Double> entry : faultProbabilities.entrySet()) {
            if (entry.getValue() > best) {
                best = entry.getValue();
                fault = entry.getKey();
            }
        }
        return fault;
    }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return FaultHypothesisSignal.class; }
    @Override public String getDescription() { return "FaultHypothesisSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        FaultHypothesisSignal c = new FaultHypothesisSignal(assetId, faultProbabilities,
                unknownAnomalyProbability, anomalyProbability, domainShiftScore,
                uncertainty, evidence, timestamp);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }

    private static double clamp(double value) {
        if (!Double.isFinite(value)) return 0.0;
        return Math.max(0.0, Math.min(1.0, value));
    }
}
