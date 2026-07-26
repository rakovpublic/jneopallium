/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.demo.industrialfmi;

import com.rakovpublic.jneuropallium.worker.net.layers.IResult;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.FaultHypothesisNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MeasurementSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.SetpointSignal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Slow-loop health and advisory calculation for P-101. */
public final class EquipmentHealthProcessor {
    private double vibrationRms = 0.55;
    private double bearingTemperature = 36.0;
    private double pumpPowerKw = 1.0;
    private double lastRisk;
    private double lastRecommendedPumpSpeed = 45.0;

    public EquipmentHealthSignal observe(List<? extends IInputSignal> signals, long timestamp) {
        if (signals != null) {
            for (IInputSignal signal : signals) {
                if (signal instanceof MeasurementSignal m) {
                    switch (m.getTag()) {
                        case IndustrialFmiTags.VIBRATION -> vibrationRms = m.getMeasurement();
                        case IndustrialFmiTags.BEARING_TEMP -> bearingTemperature = m.getMeasurement();
                        case IndustrialFmiTags.PUMP_POWER_KW -> pumpPowerKw = m.getMeasurement();
                        default -> { }
                    }
                }
            }
        }
        lastRisk = calculateRisk(vibrationRms, bearingTemperature, pumpPowerKw);
        Map<String, Double> faults = calculateFaultProbabilities(vibrationRms, bearingTemperature, pumpPowerKw, lastRisk);
        double maxFault = faults.values().stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
        double anomaly = clamp(0.70 * lastRisk + 0.30 * maxFault, 0.0, 1.0);
        double domainShift = clamp((pumpPowerKw > 6.0 && vibrationRms < 2.0 ? 0.30 : 0.0)
                + (bearingTemperature > 75.0 && vibrationRms < 2.5 ? 0.20 : 0.0), 0.0, 1.0);
        double uncertainty = clamp(0.12 + domainShift * 0.45 + (maxFault < 0.20 && anomaly > 0.35 ? 0.15 : 0.0), 0.0, 1.0);
        double healthScore = clamp(1.0 - (0.65 * anomaly + 0.25 * maxFault + 0.10 * uncertainty), 0.0, 1.0);
        return new EquipmentHealthSignal("P-101", vibrationRms, bearingTemperature, pumpPowerKw,
                lastRisk, healthScore, anomaly, faults,
                clamp(anomaly - maxFault + domainShift * 0.35, 0.0, 1.0),
                domainShift, uncertainty, recommendedAction(anomaly, maxFault, domainShift),
                evidence(vibrationRms, bearingTemperature, pumpPowerKw, faults, domainShift), timestamp);
    }

    public List<IResult> advisories(EquipmentHealthSignal health, IndustrialFmiControllerConfig config) {
        if (health == null) return List.of();
        double risk = health.getRisk();
        double healthPumpBoost = risk > 0.65 ? 10.0 : risk > 0.40 ? 5.0 : 0.0;
        double energyTrim = risk < 0.25 ? -4.0 : 0.0;
        lastRecommendedPumpSpeed = clamp(45.0 + healthPumpBoost + energyTrim,
                30.0, 45.0 + config.slowLoopPumpAdjustmentLimit());
        double maintenancePriority = clamp(risk * 100.0, 0.0, 100.0);
        double energyMode = risk > 0.65 ? 0.0 : 1.0;

        List<IResult> out = new ArrayList<>();
        out.add(new IndustrialFmiResult(new SetpointSignal(
                IndustrialFmiTags.ADVISORY_PUMP_SPEED, lastRecommendedPumpSpeed, 0.0, "EquipmentHealthProcessor"), 220L));
        out.add(new IndustrialFmiResult(new SetpointSignal(
                IndustrialFmiTags.ADVISORY_MAINTENANCE_PRIORITY, maintenancePriority, 0.0, "EquipmentHealthProcessor"), 221L));
        out.add(new IndustrialFmiResult(new SetpointSignal(
                IndustrialFmiTags.ADVISORY_BEARING_RISK, risk, 0.0, "EquipmentHealthProcessor"), 222L));
        out.add(new IndustrialFmiResult(new SetpointSignal(
                IndustrialFmiTags.ADVISORY_ENERGY_MODE, energyMode, 0.0, "EquipmentHealthProcessor"), 223L));
        out.add(new IndustrialFmiResult(health.toMachineHealthAdvisorySignal(), 224L));
        return out;
    }

    public static double calculateRisk(double vibrationRms, double bearingTemperature, double pumpPowerKw) {
        double vib = clamp((vibrationRms - 2.0) / 7.0, 0.0, 1.0);
        double bearing = clamp((bearingTemperature - 45.0) / 45.0, 0.0, 1.0);
        double power = clamp((pumpPowerKw - 2.0) / 8.0, 0.0, 1.0);
        return clamp(0.45 * vib + 0.35 * bearing + 0.20 * power, 0.0, 1.0);
    }

    public static Map<String, Double> calculateFaultProbabilities(double vibrationRms,
                                                                  double bearingTemperature,
                                                                  double pumpPowerKw,
                                                                  double risk) {
        double vib = clamp((vibrationRms - 2.0) / 7.0, 0.0, 1.0);
        double bearing = clamp((bearingTemperature - 45.0) / 45.0, 0.0, 1.0);
        double power = clamp((pumpPowerKw - 2.0) / 8.0, 0.0, 1.0);
        Map<String, Double> out = new LinkedHashMap<>();
        out.put(FaultHypothesisNeuron.BEARING_DAMAGE, clamp(0.55 * vib + 0.35 * bearing + 0.10 * power, 0.0, 1.0));
        out.put(FaultHypothesisNeuron.CAVITATION, clamp(0.45 * vib + 0.35 * power + 0.20 * risk, 0.0, 1.0));
        out.put(FaultHypothesisNeuron.IMBALANCE, clamp(0.75 * vib + 0.10 * bearing + 0.15 * power, 0.0, 1.0));
        out.put(FaultHypothesisNeuron.SENSOR_FAULT, clamp((bearing > 0.45 && vib < 0.15 ? 0.35 : 0.05)
                + (power > 0.60 && risk < 0.30 ? 0.25 : 0.0), 0.0, 1.0));
        return out;
    }

    public double lastRisk() { return lastRisk; }
    public double lastRecommendedPumpSpeed() { return lastRecommendedPumpSpeed; }

    private static String recommendedAction(double anomaly, double maxFault, double domainShift) {
        if (domainShift > 0.65 && anomaly < 0.85) return "COLLECT_SITE_BASELINE";
        if (anomaly > 0.85 || maxFault > 0.75) return "INSPECT_WITHIN_24_HOURS";
        if (anomaly > 0.60 || maxFault > 0.55) return "INSPECT_WITHIN_72_HOURS";
        if (anomaly > 0.35) return "MONITOR_CLOSELY";
        return "MONITOR";
    }

    private static List<String> evidence(double vibrationRms, double bearingTemperature, double pumpPowerKw,
                                         Map<String, Double> faults, double domainShift) {
        List<String> evidence = new ArrayList<>();
        if (vibrationRms > 4.0) evidence.add("vibration RMS is above normal skid baseline");
        if (bearingTemperature > 65.0) evidence.add("bearing temperature is elevated");
        if (pumpPowerKw > 5.0) evidence.add("pump power per unit operation is elevated");
        if (faults.getOrDefault(FaultHypothesisNeuron.CAVITATION, 0.0) > 0.50) {
            evidence.add("vibration and power pattern is consistent with cavitation risk");
        }
        if (faults.getOrDefault(FaultHypothesisNeuron.BEARING_DAMAGE, 0.0) > 0.50) {
            evidence.add("vibration and bearing-temperature pattern is consistent with bearing damage");
        }
        if (domainShift > 0.25) evidence.add("telemetry pattern differs from the scalar skid reference domain");
        return evidence;
    }

    private static double clamp(double value, double low, double high) {
        return Math.max(low, Math.min(high, value));
    }
}
