package com.rakovpublic.jneuropallium.worker.demo.autonomousmind.runtime;

public class AutonomousMindConfig {
    public boolean safetyGateEnabled = true;
    public boolean hardSafetyConstraints = true;
    public int fastLoop = 1;
    public int mediumLoop = 5;
    public int slowLoop = 20;
    public double lowEnergyThreshold = 25.0;
    public double criticalEnergyThreshold = 8.0;

    public void validate(String source) {
        if (!safetyGateEnabled) {
            throw new IllegalArgumentException("AutonomousMind safety gate cannot be disabled: " + source);
        }
        if (!hardSafetyConstraints) {
            throw new IllegalArgumentException("AutonomousMind hard safety constraints cannot be disabled: " + source);
        }
        if (lowEnergyThreshold <= criticalEnergyThreshold || criticalEnergyThreshold <= 0.0) {
            throw new IllegalArgumentException("Invalid AutonomousMind energy thresholds: " + source);
        }
    }
}
