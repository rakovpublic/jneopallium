/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.security;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Configuration for the cybersecurity / immune-system module. Mirrors
 * spec §7. Defaults reflect production-safe values: enforcing response
 * mode, rollback off, quarantine maxed at 10 minutes at 100 Hz.
 * <p>
 * Spec §11 explicitly forbids {@code affect} and {@code curiosity} for
 * this module; flags here are program-level reminders.
 */
public final class SecurityConfig {

    // ingestion
    private boolean enabled = true;
    private long packetRateLimitPerSec = 1_000_000L;
    private String syscallSource = "ebpf";
    private final List<String> logSources = new ArrayList<>();

    // signatures
    private String signatureEngine = "hyperscan";
    private final List<String> signatureRulesets = new ArrayList<>();
    private long signatureUpdateIntervalTicks = 36_000L;

    // anomaly
    private long baselineWindowTicks = 864_000L;
    private boolean baselineFreezeDuringAlert = true;
    private double anomalyScoreThresholdSoft = 0.7;
    private double anomalyScoreThresholdHard = 0.9;

    // response
    private String responseMode = "enforcing";  // or monitor-only / alert-only
    private int quarantineDefaultTicks = 18_000;
    private int quarantineMaxTicks = 360_000;
    private boolean rollbackEnabled = false;

    // tolerance
    private String allowListSource = "cmdb";
    private String criticalAssetTag = "critical=true";

    // homeostasis
    private int maxAlertsPerMin = 100;
    private boolean adaptiveSuppression = true;

    // spec §11: these modules must NOT be mixed into security
    private boolean affectDisabled = true;
    private boolean curiosityDisabled = true;

    public SecurityConfig() {
        logSources.add("syslog");
        logSources.add("windows-event");
        logSources.add("cloudtrail");
        signatureRulesets.add("emerging-threats");
        signatureRulesets.add("sigma-converted");
        signatureRulesets.add("custom");
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean v) { this.enabled = v; }
    public long getPacketRateLimitPerSec() { return packetRateLimitPerSec; }
    public void setPacketRateLimitPerSec(long v) { this.packetRateLimitPerSec = Math.max(1L, v); }
    public String getSyscallSource() { return syscallSource; }
    public void setSyscallSource(String v) { this.syscallSource = v; }
    public List<String> getLogSources() { return Collections.unmodifiableList(logSources); }
    public void addLogSource(String s) { if (s != null) logSources.add(s); }

    public String getSignatureEngine() { return signatureEngine; }
    public void setSignatureEngine(String s) { this.signatureEngine = s; }
    public List<String> getSignatureRulesets() { return Collections.unmodifiableList(signatureRulesets); }
    public void addSignatureRuleset(String s) { if (s != null) signatureRulesets.add(s); }
    public long getSignatureUpdateIntervalTicks() { return signatureUpdateIntervalTicks; }
    public void setSignatureUpdateIntervalTicks(long v) { this.signatureUpdateIntervalTicks = Math.max(1L, v); }

    public long getBaselineWindowTicks() { return baselineWindowTicks; }
    public void setBaselineWindowTicks(long v) { this.baselineWindowTicks = Math.max(1L, v); }
    public boolean isBaselineFreezeDuringAlert() { return baselineFreezeDuringAlert; }
    public void setBaselineFreezeDuringAlert(boolean v) { this.baselineFreezeDuringAlert = v; }
    public double getAnomalyScoreThresholdSoft() { return anomalyScoreThresholdSoft; }
    public void setAnomalyScoreThresholdSoft(double v) { this.anomalyScoreThresholdSoft = clamp01(v); }
    public double getAnomalyScoreThresholdHard() { return anomalyScoreThresholdHard; }
    public void setAnomalyScoreThresholdHard(double v) { this.anomalyScoreThresholdHard = clamp01(v); }

    public String getResponseMode() { return responseMode; }
    public void setResponseMode(String m) {
        if (m == null) throw new IllegalArgumentException("mode");
        String l = m.toLowerCase();
        if (!l.equals("enforcing") && !l.equals("monitor-only") && !l.equals("alert-only"))
            throw new IllegalArgumentException("mode must be enforcing / monitor-only / alert-only");
        this.responseMode = l;
    }
    public int getQuarantineDefaultTicks() { return quarantineDefaultTicks; }
    public void setQuarantineDefaultTicks(int v) { this.quarantineDefaultTicks = Math.max(1, v); }
    public int getQuarantineMaxTicks() { return quarantineMaxTicks; }
    public void setQuarantineMaxTicks(int v) { this.quarantineMaxTicks = Math.max(this.quarantineDefaultTicks, v); }
    public boolean isRollbackEnabled() { return rollbackEnabled; }
    public void setRollbackEnabled(boolean v) { this.rollbackEnabled = v; }

    public String getAllowListSource() { return allowListSource; }
    public void setAllowListSource(String s) { this.allowListSource = s; }
    public String getCriticalAssetTag() { return criticalAssetTag; }
    public void setCriticalAssetTag(String s) { this.criticalAssetTag = s; }

    public int getMaxAlertsPerMin() { return maxAlertsPerMin; }
    public void setMaxAlertsPerMin(int v) { this.maxAlertsPerMin = Math.max(1, v); }
    public boolean isAdaptiveSuppression() { return adaptiveSuppression; }
    public void setAdaptiveSuppression(boolean v) { this.adaptiveSuppression = v; }

    public boolean isAffectDisabled() { return affectDisabled; }
    public boolean isCuriosityDisabled() { return curiosityDisabled; }

    private static double clamp01(double v) { return Math.max(0.0, Math.min(1.0, v)); }
}
