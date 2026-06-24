/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.DomainShiftSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MachineFeatureSignal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Maintains site-adapted feature baselines and emits domain-shift scores. */
public class MachineBaselineNeuron extends ModulatableNeuron implements IMachineBaselineNeuron {

    private static final class Baseline {
        double rms;
        double centroid;
        double anomaly;
        int samples;
    }

    private final Map<String, Baseline> baselines = new HashMap<>();
    private double alpha = 0.03;

    public MachineBaselineNeuron() { super(); }
    public MachineBaselineNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    public void setAlpha(double alpha) { this.alpha = MachineSignalMath.clamp01(alpha); }

    @Override
    public DomainShiftSignal observe(MachineFeatureSignal feature) {
        if (feature == null) return null;
        String key = key(feature.getAssetId(), feature.getFeatureFamily());
        Baseline baseline = baselines.get(key);
        List<String> evidence = new ArrayList<>();
        if (baseline == null) {
            seedBaseline(feature.getAssetId(), feature.getFeatureFamily(), feature.getRms(),
                    feature.getSpectralCentroidHz(), feature.getAnomalyScore());
            evidence.add("baseline initialized for " + key);
            return new DomainShiftSignal(feature.getAssetId(), feature.getAnomalyScore(),
                    0.60, key, key, evidence, feature.getTimestamp());
        }
        double rmsDeviation = Math.abs(feature.getRms() - baseline.rms) / Math.max(1e-9, baseline.rms);
        double centroidDeviation = Math.abs(feature.getSpectralCentroidHz() - baseline.centroid)
                / Math.max(1.0, baseline.centroid);
        double score = MachineSignalMath.clamp01(
                0.45 * rmsDeviation + 0.25 * centroidDeviation + 0.30 * feature.getAnomalyScore());
        if (rmsDeviation > 0.40) evidence.add("rms differs from site baseline");
        if (centroidDeviation > 0.40) evidence.add("spectral centroid differs from site baseline");
        if (feature.getAnomalyScore() > 0.50) evidence.add("feature anomaly is elevated");
        if (score < 0.45) {
            baseline.rms += alpha * (feature.getRms() - baseline.rms);
            baseline.centroid += alpha * (feature.getSpectralCentroidHz() - baseline.centroid);
            baseline.anomaly += alpha * (feature.getAnomalyScore() - baseline.anomaly);
            baseline.samples++;
        }
        double uncertainty = MachineSignalMath.clamp01(0.50 / Math.sqrt(Math.max(1, baseline.samples)) + score * 0.40);
        return new DomainShiftSignal(feature.getAssetId(), score, uncertainty, key, key,
                evidence, feature.getTimestamp());
    }

    @Override
    public void seedBaseline(String assetId, String featureFamily, double rms, double spectralCentroidHz, double anomalyScore) {
        Baseline baseline = new Baseline();
        baseline.rms = Math.max(1e-9, rms);
        baseline.centroid = Math.max(0.0, spectralCentroidHz);
        baseline.anomaly = MachineSignalMath.clamp01(anomalyScore);
        baseline.samples = 1;
        baselines.put(key(assetId, featureFamily), baseline);
    }

    @Override
    public double baselineRms(String assetId, String featureFamily) {
        Baseline baseline = baselines.get(key(assetId, featureFamily));
        return baseline == null ? Double.NaN : baseline.rms;
    }

    private static String key(String assetId, String featureFamily) {
        String asset = assetId == null || assetId.isBlank() ? "UNKNOWN" : assetId;
        String family = featureFamily == null || featureFamily.isBlank() ? "UNKNOWN" : featureFamily;
        return asset + ":" + family;
    }
}
