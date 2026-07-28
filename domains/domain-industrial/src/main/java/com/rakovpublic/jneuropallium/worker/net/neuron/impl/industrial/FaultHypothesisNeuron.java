/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.DomainShiftSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.FaultHypothesisSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MachineFeatureSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MachineWaveformSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.OperatingRegimeSignal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Correlates recent acoustic/vibration/context features into fault hypotheses. */
public class FaultHypothesisNeuron extends ModulatableNeuron implements IFaultHypothesisNeuron {

    public static final String BEARING_DAMAGE = "bearingDamage";
    public static final String CAVITATION = "cavitation";
    public static final String IMBALANCE = "imbalance";
    public static final String SENSOR_FAULT = "sensorFault";

    private final Map<String, Map<String, MachineFeatureSignal>> featuresByAsset = new HashMap<>();
    private final Map<String, DomainShiftSignal> domainByAsset = new HashMap<>();
    private final Map<String, OperatingRegimeSignal> regimeByAsset = new HashMap<>();

    public FaultHypothesisNeuron() { super(); }
    public FaultHypothesisNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    public Map<String, Map<String, MachineFeatureSignal>> getFeaturesByAsset() {
        return featuresByAsset;
    }

    public void setFeaturesByAsset(Map<String, Map<String, MachineFeatureSignal>> featuresByAsset) {
        this.featuresByAsset.clear();
        if (featuresByAsset != null) this.featuresByAsset.putAll(featuresByAsset);
    }

    public Map<String, DomainShiftSignal> getDomainByAsset() {
        return domainByAsset;
    }

    public void setDomainByAsset(Map<String, DomainShiftSignal> domainByAsset) {
        this.domainByAsset.clear();
        if (domainByAsset != null) this.domainByAsset.putAll(domainByAsset);
    }

    public Map<String, OperatingRegimeSignal> getRegimeByAsset() {
        return regimeByAsset;
    }

    public void setRegimeByAsset(Map<String, OperatingRegimeSignal> regimeByAsset) {
        this.regimeByAsset.clear();
        if (regimeByAsset != null) this.regimeByAsset.putAll(regimeByAsset);
    }

    @Override
    public FaultHypothesisSignal hypothesize(MachineFeatureSignal feature) {
        if (feature == null) return null;
        String asset = asset(feature.getAssetId());
        featuresByAsset.computeIfAbsent(asset, ignored -> new HashMap<>())
                .put(feature.getFeatureFamily(), feature);
        MachineFeatureSignal acoustic = feature(asset, MachineWaveformSignal.CHANNEL_ACOUSTIC);
        MachineFeatureSignal vibration = feature(asset, MachineWaveformSignal.CHANNEL_VIBRATION);
        OperatingRegimeSignal regime = regimeByAsset.get(asset);
        DomainShiftSignal shift = domainByAsset.get(asset);

        double acousticScore = score(acoustic);
        double vibrationScore = score(vibration);
        double domain = shift == null ? 0.0 : shift.getDomainShiftScore();
        double pressure = regime == null ? 1.0 : regime.getPressure();
        double flow = regime == null ? 1.0 : regime.getFlow();
        double temp = regime == null ? 35.0 : regime.getTemperature();
        double lowPressure = MachineSignalMath.clamp01((0.42 - pressure) / 0.42);
        double lowFlow = MachineSignalMath.clamp01((0.30 - flow) / 0.30);
        double highTemp = MachineSignalMath.clamp01((temp - 60.0) / 50.0);
        double envelope = vibration == null ? 0.0 : MachineSignalMath.clamp01(vibration.getEnvelopeEnergy() / Math.max(1e-6, vibration.getRms()));
        double crest = Math.max(acoustic == null ? 0.0 : acoustic.getCrestFactor(),
                vibration == null ? 0.0 : vibration.getCrestFactor());
        double crestScore = MachineSignalMath.clamp01((crest - 3.0) / 7.0);

        Map<String, Double> probabilities = new LinkedHashMap<>();
        probabilities.put(BEARING_DAMAGE, MachineSignalMath.clamp01(0.55 * vibrationScore + 0.25 * envelope + 0.20 * highTemp));
        probabilities.put(CAVITATION, MachineSignalMath.clamp01(0.45 * acousticScore + 0.25 * vibrationScore + 0.25 * lowPressure + 0.05 * lowFlow));
        probabilities.put(IMBALANCE, MachineSignalMath.clamp01(0.55 * vibrationScore + 0.25 * crestScore + 0.20 * speedNormalised(vibration)));
        probabilities.put(SENSOR_FAULT, MachineSignalMath.clamp01(domain * 0.70 + (Math.max(acousticScore, vibrationScore) < 0.25 ? 0.15 : 0.0)));

        double maxFault = probabilities.values().stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
        double anomaly = MachineSignalMath.clamp01(0.70 * Math.max(acousticScore, vibrationScore) + 0.30 * maxFault);
        double missingModalities = (acoustic == null ? 0.5 : 0.0) + (vibration == null ? 0.5 : 0.0);
        double uncertainty = MachineSignalMath.clamp01(0.10 + 0.60 * domain + 0.20 * missingModalities);
        if (acousticScore > 0.65 && vibrationScore < 0.25 && lowPressure < 0.20) {
            anomaly = MachineSignalMath.clamp01(anomaly * 0.75);
            uncertainty = MachineSignalMath.clamp01(uncertainty + 0.15);
        }
        double unknown = MachineSignalMath.clamp01(anomaly - maxFault + domain * 0.35 + missingModalities * 0.10);

        List<String> evidence = new ArrayList<>();
        if (acousticScore > 0.50) evidence.add("acoustic anomaly is elevated");
        if (vibrationScore > 0.50) evidence.add("vibration anomaly is elevated");
        if (lowPressure > 0.20) evidence.add("suction pressure is below learned regime");
        if (lowFlow > 0.20) evidence.add("flow is below learned regime");
        if (highTemp > 0.20) evidence.add("bearing or process temperature is elevated");
        if (domain > 0.40) evidence.add("domain shift is elevated");
        return new FaultHypothesisSignal(asset, probabilities, unknown, anomaly, domain,
                uncertainty, evidence, feature.getTimestamp());
    }

    @Override public void observeDomainShift(DomainShiftSignal domainShift) {
        if (domainShift != null) domainByAsset.put(asset(domainShift.getAssetId()), domainShift);
    }

    @Override public void observeRegime(OperatingRegimeSignal regime) {
        if (regime != null) regimeByAsset.put(asset(regime.getAssetId()), regime);
    }

    private MachineFeatureSignal feature(String assetId, String family) {
        Map<String, MachineFeatureSignal> byFamily = featuresByAsset.get(asset(assetId));
        return byFamily == null ? null : byFamily.get(family);
    }

    private static double score(MachineFeatureSignal feature) {
        return feature == null ? 0.0 : feature.getAnomalyScore();
    }

    private static double speedNormalised(MachineFeatureSignal feature) {
        return feature == null ? 0.0 : feature.getSpeedNormalizedScore();
    }

    private static String asset(String assetId) {
        return assetId == null || assetId.isBlank() ? "UNKNOWN" : assetId;
    }
}
