/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for the clinical-decision-support module. Mirrors the
 * YAML section {@code clinical} in the worker config. Unlike the other
 * extension modules the clinical module is intended to default to
 * {@code enabled=true} when this configuration object is instantiated —
 * but a network that never constructs it remains unaffected.
 * <p>
 * The <b>affect</b>, <b>curiosity</b>, and <b>sleep</b> modules are
 * intentionally held off in clinical mode per spec §11.
 */
public final class ClinicalConfig {

    private boolean enabled = true;
    private boolean patientIsolation = true;

    // vital guardrails per VitalType
    private final Map<VitalType, double[]> vitalGuardrails = new EnumMap<>(VitalType.class);

    // differential-diagnosis
    private int differentialMaxCandidates = 10;
    private double differentialPosteriorThreshold = 0.1;

    // contraindication
    private String contraindicationSource = "rxnorm+snomed";
    private long contraindicationRefreshTicks = 100_000L;

    // recommendation
    private String recommendationMode = "advisory"; // never autonomous
    private boolean recommendationConfirmationRequired = true;

    // transparency
    private boolean transparencyLogEveryDecision = true;
    private String clinicianDashboardEndpoint;

    // llm
    private long llmCacheTtlMs = 3_600_000L; // 1 hour for clinical

    // clinical-policy: these modules must remain disabled in clinical mode
    private boolean affectDisabled = true;
    private boolean curiosityDisabled = true;
    private boolean sleepDisabled = true;

    // extra clinical acuity / transparency fields
    private final Map<String, Object> extras = new HashMap<>();

    public ClinicalConfig() {
        vitalGuardrails.put(VitalType.HR, new double[]{40, 150});
        vitalGuardrails.put(VitalType.SPO2, new double[]{88, 100});
        vitalGuardrails.put(VitalType.BP_SYS, new double[]{80, 200});
        vitalGuardrails.put(VitalType.BP_DIA, new double[]{40, 120});
        vitalGuardrails.put(VitalType.TEMP, new double[]{35.0, 39.5});
        vitalGuardrails.put(VitalType.RESP, new double[]{8, 30});
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean v) { this.enabled = v; }
    public boolean isPatientIsolation() { return patientIsolation; }
    public void setPatientIsolation(boolean v) { this.patientIsolation = v; }

    public Map<VitalType, double[]> getVitalGuardrails() { return vitalGuardrails; }
    public void setVitalGuardrail(VitalType t, double min, double max) {
        if (t == null) return;
        vitalGuardrails.put(t, new double[]{Math.min(min, max), Math.max(min, max)});
    }

    public int getDifferentialMaxCandidates() { return differentialMaxCandidates; }
    public void setDifferentialMaxCandidates(int v) { this.differentialMaxCandidates = Math.max(1, v); }
    public double getDifferentialPosteriorThreshold() { return differentialPosteriorThreshold; }
    public void setDifferentialPosteriorThreshold(double v) {
        this.differentialPosteriorThreshold = Math.max(0.0, Math.min(1.0, v));
    }

    public String getContraindicationSource() { return contraindicationSource; }
    public void setContraindicationSource(String v) { this.contraindicationSource = v; }
    public long getContraindicationRefreshTicks() { return contraindicationRefreshTicks; }
    public void setContraindicationRefreshTicks(long v) {
        this.contraindicationRefreshTicks = Math.max(1L, v);
    }

    public String getRecommendationMode() { return recommendationMode; }
    /** Setter enforces advisory-only: any non-"advisory" value throws. */
    public void setRecommendationMode(String v) {
        if (v == null || !"advisory".equalsIgnoreCase(v)) {
            throw new IllegalArgumentException("clinical.recommendation.mode must be 'advisory' (SaMD constraint)");
        }
        this.recommendationMode = "advisory";
    }
    public boolean isRecommendationConfirmationRequired() { return recommendationConfirmationRequired; }
    public void setRecommendationConfirmationRequired(boolean v) {
        if (!v) throw new IllegalArgumentException("clinical.recommendation.confirmation-required must be true");
        this.recommendationConfirmationRequired = true;
    }

    public boolean isTransparencyLogEveryDecision() { return transparencyLogEveryDecision; }
    public void setTransparencyLogEveryDecision(boolean v) { this.transparencyLogEveryDecision = v; }
    public String getClinicianDashboardEndpoint() { return clinicianDashboardEndpoint; }
    public void setClinicianDashboardEndpoint(String v) { this.clinicianDashboardEndpoint = v; }

    public long getLlmCacheTtlMs() { return llmCacheTtlMs; }
    public void setLlmCacheTtlMs(long v) { this.llmCacheTtlMs = Math.max(0L, v); }

    public boolean isAffectDisabled() { return affectDisabled; }
    public boolean isCuriosityDisabled() { return curiosityDisabled; }
    public boolean isSleepDisabled() { return sleepDisabled; }

    public Map<String, Object> getExtras() { return extras; }
}
