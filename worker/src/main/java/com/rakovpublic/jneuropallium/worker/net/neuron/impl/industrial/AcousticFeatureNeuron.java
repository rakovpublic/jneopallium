/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MachineFeatureSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MachineWaveformSignal;

import java.util.HashMap;
import java.util.Map;

/** Local acoustic feature receptor for machine-condition monitoring. */
public class AcousticFeatureNeuron extends ModulatableNeuron implements IAcousticFeatureNeuron {

    private final Map<String, Double> baselineRms = new HashMap<>();
    private double baselineAlpha = 0.05;
    private double minimumNormalRms = 0.04;
    private double anomalyRmsMultiple = 2.5;

    public AcousticFeatureNeuron() { super(); }
    public AcousticFeatureNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    public void setBaselineAlpha(double baselineAlpha) {
        this.baselineAlpha = MachineSignalMath.clamp01(baselineAlpha);
    }
    public void setMinimumNormalRms(double minimumNormalRms) {
        this.minimumNormalRms = Math.max(1e-9, minimumNormalRms);
    }
    public void setAnomalyRmsMultiple(double anomalyRmsMultiple) {
        this.anomalyRmsMultiple = Math.max(1.0, anomalyRmsMultiple);
    }
    public Map<String, Double> getBaselineRms() {
        return new HashMap<>(baselineRms);
    }
    public void setBaselineRms(Map<String, Double> baselineRms) {
        this.baselineRms.clear();
        if (baselineRms != null) this.baselineRms.putAll(baselineRms);
    }

    @Override
    public MachineFeatureSignal extract(MachineWaveformSignal waveform) {
        if (waveform == null) return null;
        String asset = asset(waveform.getAssetId());
        MachineSignalMath.FeatureStats stats = MachineSignalMath.analyse(waveform.getSamples(), waveform.getSampleRateHz());
        double baseline = baselineRms.getOrDefault(asset, Math.max(minimumNormalRms, stats.rms));
        double relative = MachineSignalMath.clamp01(
                (stats.rms - baseline * anomalyRmsMultiple) / Math.max(minimumNormalRms, baseline * anomalyRmsMultiple));
        double absolute = MachineSignalMath.clamp01((stats.rms - minimumNormalRms) / Math.max(minimumNormalRms, minimumNormalRms * 8.0));
        double crest = MachineSignalMath.clamp01((stats.crestFactor - 4.0) / 8.0);
        double speedNormalised = MachineSignalMath.clamp01(
                stats.rms / Math.max(minimumNormalRms, 1.0 + waveform.getRotationalSpeedRpm() / 3600.0));
        double anomaly = MachineSignalMath.clamp01(0.55 * relative + 0.20 * absolute + 0.15 * crest + 0.10 * speedNormalised);
        if (anomaly < 0.55 || !baselineRms.containsKey(asset)) {
            baselineRms.put(asset, baseline + baselineAlpha * (stats.rms - baseline));
        }
        Map<String, Double> features = stats.toMap();
        features.put("baselineRms", baseline);
        features.put("relativeRms", baseline <= 0.0 ? 0.0 : stats.rms / baseline);
        features.put("rotationalSpeedRpm", waveform.getRotationalSpeedRpm());
        features.put("sampleRateHz", waveform.getSampleRateHz());
        return new MachineFeatureSignal(asset, MachineWaveformSignal.CHANNEL_ACOUSTIC, anomaly,
                stats.rms, stats.crestFactor, stats.spectralCentroidHz,
                stats.envelopeEnergy, speedNormalised, features, waveform.getTimestamp());
    }

    @Override public void seedBaseline(String assetId, double rms) {
        baselineRms.put(asset(assetId), Math.max(minimumNormalRms, rms));
    }

    @Override public double baselineRms(String assetId) {
        return baselineRms.getOrDefault(asset(assetId), Double.NaN);
    }

    private static String asset(String assetId) {
        return assetId == null || assetId.isBlank() ? "UNKNOWN" : assetId;
    }
}
