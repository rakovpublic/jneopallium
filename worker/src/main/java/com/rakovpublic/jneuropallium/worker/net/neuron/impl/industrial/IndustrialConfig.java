/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial;

/**
 * Configuration for the industrial-process-control module. Mirrors the
 * YAML section {@code industrial} in spec §9. Defaults are
 * production-safe: advisory mode per-loop, rollback / autonomous opt-in
 * at deployment time, tamper-evident audit trail, operator override
 * always honoured.
 */
public final class IndustrialConfig {

    private boolean enabled = true;
    private int tickRateHz = 100;

    // safety
    private SafetyMode safetyMode = SafetyMode.ADVISORY;
    private String interlocksSource = "srs.yaml";
    private long interlockTestIntervalTicks = 864_000L;

    // oscillation
    private boolean oscillationDetectionEnabled = true;
    private int acfWindowTicks = 200;
    private int circuitBreakerMaxDurationTicks = 6_000;

    // MPC
    private int mpcHorizonTicks = 60;
    private int mpcControlHorizonTicks = 10;

    // maintenance
    private long rulRefreshTicks = 100_000L;
    private int schedulingHorizonDays = 90;

    // audit
    private boolean logEveryDecision = true;
    private boolean hashChainEnabled = true;
    private int retentionDays = 2555;

    // operator override
    private boolean overrideAlwaysHonoured = true;
    private boolean overrideLockoutDuringEmergencyOnly = true;

    // ISA-95
    private int isa95Level = 2;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean v) { this.enabled = v; }
    public int getTickRateHz() { return tickRateHz; }
    public void setTickRateHz(int v) { this.tickRateHz = Math.max(1, v); }

    public SafetyMode getSafetyMode() { return safetyMode; }
    public void setSafetyMode(SafetyMode m) {
        if (m == null) throw new IllegalArgumentException("safety.mode must be SHADOW / ADVISORY / AUTONOMOUS");
        this.safetyMode = m;
    }
    public String getInterlocksSource() { return interlocksSource; }
    public void setInterlocksSource(String s) { this.interlocksSource = s; }
    public long getInterlockTestIntervalTicks() { return interlockTestIntervalTicks; }
    public void setInterlockTestIntervalTicks(long t) { this.interlockTestIntervalTicks = Math.max(1L, t); }

    public boolean isOscillationDetectionEnabled() { return oscillationDetectionEnabled; }
    public void setOscillationDetectionEnabled(boolean v) { this.oscillationDetectionEnabled = v; }
    public int getAcfWindowTicks() { return acfWindowTicks; }
    public void setAcfWindowTicks(int v) { this.acfWindowTicks = Math.max(8, v); }
    public int getCircuitBreakerMaxDurationTicks() { return circuitBreakerMaxDurationTicks; }
    public void setCircuitBreakerMaxDurationTicks(int v) { this.circuitBreakerMaxDurationTicks = Math.max(1, v); }

    public int getMpcHorizonTicks() { return mpcHorizonTicks; }
    public void setMpcHorizonTicks(int v) { this.mpcHorizonTicks = Math.max(1, v); }
    public int getMpcControlHorizonTicks() { return mpcControlHorizonTicks; }
    public void setMpcControlHorizonTicks(int v) {
        this.mpcControlHorizonTicks = Math.max(1, Math.min(mpcHorizonTicks, v));
    }

    public long getRulRefreshTicks() { return rulRefreshTicks; }
    public void setRulRefreshTicks(long v) { this.rulRefreshTicks = Math.max(1L, v); }
    public int getSchedulingHorizonDays() { return schedulingHorizonDays; }
    public void setSchedulingHorizonDays(int v) { this.schedulingHorizonDays = Math.max(1, v); }

    public boolean isLogEveryDecision() { return logEveryDecision; }
    public void setLogEveryDecision(boolean v) { this.logEveryDecision = v; }
    public boolean isHashChainEnabled() { return hashChainEnabled; }
    public void setHashChainEnabled(boolean v) { this.hashChainEnabled = v; }
    public int getRetentionDays() { return retentionDays; }
    public void setRetentionDays(int v) { this.retentionDays = Math.max(1, v); }

    public boolean isOverrideAlwaysHonoured() { return overrideAlwaysHonoured; }
    /** Per spec §11 operator override authority must remain enabled in this module. */
    public void setOverrideAlwaysHonoured(boolean v) {
        if (!v) throw new IllegalArgumentException("operator-override.always-honoured must be true for industrial module");
        this.overrideAlwaysHonoured = true;
    }
    public boolean isOverrideLockoutDuringEmergencyOnly() { return overrideLockoutDuringEmergencyOnly; }
    public void setOverrideLockoutDuringEmergencyOnly(boolean v) { this.overrideLockoutDuringEmergencyOnly = v; }

    public int getIsa95Level() { return isa95Level; }
    public void setIsa95Level(int v) { this.isa95Level = v; }
}
