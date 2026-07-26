/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.demo.industrialfmi;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MachineHealthAdvisorySignal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Slow-loop health estimate for P-101. */
public final class EquipmentHealthSignal extends AbstractSignal<Void> implements ISignal<Void>, IInputSignal<Void> {
    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(1L, 2);

    private String assetId;
    private double vibrationRms;
    private double bearingTemperature;
    private double pumpPowerKw;
    private double risk;
    private double healthScore;
    private double anomalyProbability;
    private Map<String, Double> faultProbabilities;
    private double unknownAnomalyProbability;
    private double domainShiftScore;
    private double uncertainty;
    private String recommendedAction;
    private List<String> evidence;
    private long timestamp;

    public EquipmentHealthSignal() {
        super();
        this.loop = 2;
        this.epoch = 1L;
        this.timeAlive = 20;
        this.faultProbabilities = new LinkedHashMap<>();
        this.evidence = new ArrayList<>();
        this.recommendedAction = "MONITOR";
    }

    public EquipmentHealthSignal(String assetId, double vibrationRms, double bearingTemperature,
                                 double pumpPowerKw, double risk, long timestamp) {
        this(assetId, vibrationRms, bearingTemperature, pumpPowerKw, risk,
                Math.max(0.0, Math.min(1.0, 1.0 - risk)), risk, Map.of(),
                0.0, 0.0, 0.15, "MONITOR", List.of(), timestamp);
    }

    public EquipmentHealthSignal(String assetId, double vibrationRms, double bearingTemperature,
                                 double pumpPowerKw, double risk, double healthScore,
                                 double anomalyProbability, Map<String, Double> faultProbabilities,
                                 double unknownAnomalyProbability, double domainShiftScore,
                                 double uncertainty, String recommendedAction,
                                 List<String> evidence, long timestamp) {
        this();
        this.assetId = assetId;
        this.vibrationRms = vibrationRms;
        this.bearingTemperature = bearingTemperature;
        this.pumpPowerKw = pumpPowerKw;
        this.risk = risk;
        this.healthScore = clamp(healthScore);
        this.anomalyProbability = clamp(anomalyProbability);
        setFaultProbabilities(faultProbabilities);
        this.unknownAnomalyProbability = clamp(unknownAnomalyProbability);
        this.domainShiftScore = clamp(domainShiftScore);
        this.uncertainty = clamp(uncertainty);
        this.recommendedAction = recommendedAction == null ? "MONITOR" : recommendedAction;
        setEvidence(evidence);
        this.timestamp = timestamp;
    }

    public String getAssetId() { return assetId; }
    public double getVibrationRms() { return vibrationRms; }
    public double getBearingTemperature() { return bearingTemperature; }
    public double getPumpPowerKw() { return pumpPowerKw; }
    public double getRisk() { return risk; }
    public double getHealthScore() { return healthScore; }
    public double getAnomalyProbability() { return anomalyProbability; }
    public Map<String, Double> getFaultProbabilities() { return Map.copyOf(faultProbabilities); }
    public double getUnknownAnomalyProbability() { return unknownAnomalyProbability; }
    public double getDomainShiftScore() { return domainShiftScore; }
    public double getUncertainty() { return uncertainty; }
    public String getRecommendedAction() { return recommendedAction; }
    public List<String> getEvidence() { return List.copyOf(evidence); }
    public long getTimestamp() { return timestamp; }

    public void setFaultProbabilities(Map<String, Double> faultProbabilities) {
        this.faultProbabilities = new LinkedHashMap<>();
        if (faultProbabilities == null) return;
        for (Map.Entry<String, Double> entry : faultProbabilities.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                this.faultProbabilities.put(entry.getKey(), clamp(entry.getValue()));
            }
        }
    }

    public void setEvidence(List<String> evidence) {
        this.evidence = new ArrayList<>();
        if (evidence == null) return;
        for (String item : evidence) {
            if (item != null && !item.isBlank()) this.evidence.add(item);
        }
    }

    public MachineHealthAdvisorySignal toMachineHealthAdvisorySignal() {
        return new MachineHealthAdvisorySignal(assetId, "1.0.0-machine-health", "ADVISORY",
                healthScore, anomalyProbability, faultProbabilities, unknownAnomalyProbability,
                domainShiftScore, uncertainty, recommendedAction, evidence, false, timestamp);
    }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return EquipmentHealthSignal.class; }
    @Override public String getDescription() { return "EquipmentHealthSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        EquipmentHealthSignal c = new EquipmentHealthSignal(assetId, vibrationRms, bearingTemperature,
                pumpPowerKw, risk, healthScore, anomalyProbability, faultProbabilities,
                unknownAnomalyProbability, domainShiftScore, uncertainty, recommendedAction,
                evidence, timestamp);
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
