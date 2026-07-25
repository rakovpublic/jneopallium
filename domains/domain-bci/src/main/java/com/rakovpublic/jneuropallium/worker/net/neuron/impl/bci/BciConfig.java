/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci;

/**
 * Configuration for the BCI / neural-prosthetics module. Mirrors the YAML
 * section {@code bci} in the worker config; default {@code enabled=false}
 * so that existing deployments are unaffected.
 */
public final class BciConfig {

    private boolean enabled;
    private int tickRateHz = 1000;

    // --- stimulation safety envelope ---
    private double maxChargeDensityUCcm2 = 0.5;         // Shannon 1992 / Cogan 2008
    private double netDcToleranceUC = 1e-3;              // 1 nC
    private double maxFrequencyHz = 300.0;
    private double maxPulseWidthUS = 300.0;

    // --- thermal ---
    private double thermalCoolDownDeltaC = 1.0;
    private double thermalShutdownDeltaC = 2.0;

    // --- seizure ---
    private double seizureRiskThreshold = 0.8;
    private long seizureLockoutTicks = 60_000;

    // --- power ---
    private double batteryCapacityMAh = 500.0;
    private double powerConserveFrac = 0.30;
    private double powerEmergencyFrac = 0.10;

    // --- embodiment ---
    private double efferenceCopyTolerance = 0.15;

    // --- calibration ---
    private long calibMinIntervalTicks = 24L * 60 * 60 * 1000;
    private long calibMaxIntervalTicks = 7L * 24 * 60 * 60 * 1000;
    private double calibDriftTrigger = 0.25;
    private double calibErrorTrigger = 0.30;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean v) { this.enabled = v; }
    public int getTickRateHz() { return tickRateHz; }
    public void setTickRateHz(int v) { this.tickRateHz = Math.max(1, v); }

    public double getMaxChargeDensityUCcm2() { return maxChargeDensityUCcm2; }
    public void setMaxChargeDensityUCcm2(double v) { this.maxChargeDensityUCcm2 = v; }
    public double getNetDcToleranceUC() { return netDcToleranceUC; }
    public void setNetDcToleranceUC(double v) { this.netDcToleranceUC = v; }
    public double getMaxFrequencyHz() { return maxFrequencyHz; }
    public void setMaxFrequencyHz(double v) { this.maxFrequencyHz = v; }
    public double getMaxPulseWidthUS() { return maxPulseWidthUS; }
    public void setMaxPulseWidthUS(double v) { this.maxPulseWidthUS = v; }

    public double getThermalCoolDownDeltaC() { return thermalCoolDownDeltaC; }
    public void setThermalCoolDownDeltaC(double v) { this.thermalCoolDownDeltaC = v; }
    public double getThermalShutdownDeltaC() { return thermalShutdownDeltaC; }
    public void setThermalShutdownDeltaC(double v) { this.thermalShutdownDeltaC = v; }

    public double getSeizureRiskThreshold() { return seizureRiskThreshold; }
    public void setSeizureRiskThreshold(double v) { this.seizureRiskThreshold = v; }
    public long getSeizureLockoutTicks() { return seizureLockoutTicks; }
    public void setSeizureLockoutTicks(long v) { this.seizureLockoutTicks = v; }

    public double getBatteryCapacityMAh() { return batteryCapacityMAh; }
    public void setBatteryCapacityMAh(double v) { this.batteryCapacityMAh = v; }
    public double getPowerConserveFrac() { return powerConserveFrac; }
    public void setPowerConserveFrac(double v) { this.powerConserveFrac = v; }
    public double getPowerEmergencyFrac() { return powerEmergencyFrac; }
    public void setPowerEmergencyFrac(double v) { this.powerEmergencyFrac = v; }

    public double getEfferenceCopyTolerance() { return efferenceCopyTolerance; }
    public void setEfferenceCopyTolerance(double v) { this.efferenceCopyTolerance = v; }

    public long getCalibMinIntervalTicks() { return calibMinIntervalTicks; }
    public void setCalibMinIntervalTicks(long v) { this.calibMinIntervalTicks = v; }
    public long getCalibMaxIntervalTicks() { return calibMaxIntervalTicks; }
    public void setCalibMaxIntervalTicks(long v) { this.calibMaxIntervalTicks = v; }
    public double getCalibDriftTrigger() { return calibDriftTrigger; }
    public void setCalibDriftTrigger(double v) { this.calibDriftTrigger = v; }
    public double getCalibErrorTrigger() { return calibErrorTrigger; }
    public void setCalibErrorTrigger(double v) { this.calibErrorTrigger = v; }
}
