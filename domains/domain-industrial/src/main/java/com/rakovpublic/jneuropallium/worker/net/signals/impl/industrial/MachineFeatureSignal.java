/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Compressed acoustic, vibration, motor-current, or process feature frame.
 * Feature neurons emit this instead of forwarding raw waveforms into slow
 * supervisory layers. ProcessingFrequency: loop=2, epoch=2.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MachineFeatureSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(2L, 2);

    private String assetId;
    private String featureFamily;
    private double anomalyScore;
    private double rms;
    private double crestFactor;
    private double spectralCentroidHz;
    private double envelopeEnergy;
    private double speedNormalizedScore;
    private Map<String, Double> features;
    private long timestamp;

    public MachineFeatureSignal() {
        super();
        this.loop = 2;
        this.epoch = 2L;
        this.timeAlive = 0;
        this.features = new LinkedHashMap<>();
    }

    public MachineFeatureSignal(String assetId, String featureFamily, double anomalyScore,
                                double rms, double crestFactor, double spectralCentroidHz,
                                double envelopeEnergy, double speedNormalizedScore,
                                Map<String, Double> features, long timestamp) {
        this();
        this.assetId = assetId;
        this.featureFamily = featureFamily;
        this.anomalyScore = clamp(anomalyScore);
        this.rms = Math.max(0.0, rms);
        this.crestFactor = Math.max(0.0, crestFactor);
        this.spectralCentroidHz = Math.max(0.0, spectralCentroidHz);
        this.envelopeEnergy = Math.max(0.0, envelopeEnergy);
        this.speedNormalizedScore = clamp(speedNormalizedScore);
        setFeatures(features);
        this.timestamp = timestamp;
    }

    public String getAssetId() { return assetId; }
    public void setAssetId(String assetId) { this.assetId = assetId; }
    public String getFeatureFamily() { return featureFamily; }
    public void setFeatureFamily(String featureFamily) { this.featureFamily = featureFamily; }
    public double getAnomalyScore() { return anomalyScore; }
    public void setAnomalyScore(double anomalyScore) { this.anomalyScore = clamp(anomalyScore); }
    public double getRms() { return rms; }
    public void setRms(double rms) { this.rms = Math.max(0.0, rms); }
    public double getCrestFactor() { return crestFactor; }
    public void setCrestFactor(double crestFactor) { this.crestFactor = Math.max(0.0, crestFactor); }
    public double getSpectralCentroidHz() { return spectralCentroidHz; }
    public void setSpectralCentroidHz(double spectralCentroidHz) {
        this.spectralCentroidHz = Math.max(0.0, spectralCentroidHz);
    }
    public double getEnvelopeEnergy() { return envelopeEnergy; }
    public void setEnvelopeEnergy(double envelopeEnergy) { this.envelopeEnergy = Math.max(0.0, envelopeEnergy); }
    public double getSpeedNormalizedScore() { return speedNormalizedScore; }
    public void setSpeedNormalizedScore(double speedNormalizedScore) {
        this.speedNormalizedScore = clamp(speedNormalizedScore);
    }
    public Map<String, Double> getFeatures() { return Collections.unmodifiableMap(features); }
    public void setFeatures(Map<String, Double> features) {
        this.features = new LinkedHashMap<>();
        if (features == null) return;
        for (Map.Entry<String, Double> entry : features.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null && Double.isFinite(entry.getValue())) {
                this.features.put(entry.getKey(), entry.getValue());
            }
        }
    }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return MachineFeatureSignal.class; }
    @Override public String getDescription() { return "MachineFeatureSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        MachineFeatureSignal c = new MachineFeatureSignal(assetId, featureFamily, anomalyScore,
                rms, crestFactor, spectralCentroidHz, envelopeEnergy, speedNormalizedScore,
                features, timestamp);
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
