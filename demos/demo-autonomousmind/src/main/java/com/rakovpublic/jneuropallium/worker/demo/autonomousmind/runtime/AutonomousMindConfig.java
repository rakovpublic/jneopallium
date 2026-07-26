package com.rakovpublic.jneuropallium.worker.demo.autonomousmind.runtime;

import java.util.LinkedHashMap;
import java.util.Map;

public class AutonomousMindConfig {
    public boolean safetyGateEnabled = true;
    public boolean hardSafetyConstraints = true;
    public boolean harmHardConstraints = true;
    public boolean harmGateNeuronPresent = true;
    public int fastLoop = 1;
    public int mediumLoop = 5;
    public int slowLoop = 20;
    public int slowFastRatio = 10;
    public double lowEnergyThreshold = 25.0;
    public double criticalEnergyThreshold = 8.0;
    public Map<String, Double> hardVetoThresholds = defaultHardVetoThresholds();

    public void validate(String source) {
        if (!safetyGateEnabled) {
            throw new IllegalArgumentException("AutonomousMind safety gate cannot be disabled: " + source);
        }
        if (!hardSafetyConstraints) {
            throw new IllegalArgumentException("AutonomousMind hard safety constraints cannot be disabled: " + source);
        }
        if (!harmHardConstraints) {
            throw new IllegalArgumentException("AutonomousMind harm hard constraints cannot be disabled: " + source);
        }
        if (!harmGateNeuronPresent) {
            throw new IllegalArgumentException("AutonomousMind HarmGateNeuron cannot be removed: " + source);
        }
        if (lowEnergyThreshold <= criticalEnergyThreshold || criticalEnergyThreshold <= 0.0) {
            throw new IllegalArgumentException("Invalid AutonomousMind energy thresholds: " + source);
        }
        Double physicalIntegrity = hardVetoThresholds == null ? null : hardVetoThresholds.get("physicalIntegrity");
        if (physicalIntegrity == null || physicalIntegrity < 0.25) {
            throw new IllegalArgumentException("AutonomousMind physicalIntegrity hard-veto threshold below structural minimum: " + source);
        }
    }

    private static Map<String, Double> defaultHardVetoThresholds() {
        Map<String, Double> thresholds = new LinkedHashMap<>();
        thresholds.put("physicalIntegrity", 0.8);
        thresholds.put("autonomy", 0.7);
        thresholds.put("resource", 0.6);
        thresholds.put("information", 0.6);
        thresholds.put("emotional", 0.5);
        thresholds.put("longTermRisk", 0.65);
        thresholds.put("selfPreservation", 0.8);
        return thresholds;
    }
}
