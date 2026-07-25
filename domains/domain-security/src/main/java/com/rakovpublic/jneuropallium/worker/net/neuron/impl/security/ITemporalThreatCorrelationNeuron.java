package com.rakovpublic.jneuropallium.worker.net.neuron.impl.security;

import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.AnomalyScoreSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.SignatureMatchSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.ThreatHypothesisSignal;

import java.util.List;
import java.util.Map;

public interface ITemporalThreatCorrelationNeuron extends IThreatHypothesisNeuron {
    double getFastThreatActivation();
    void setFastThreatActivation(double value);
    double getThreatPosterior();
    void setThreatPosterior(double value);
    double getAssetCriticality();
    void setAssetCriticality(double value);
    boolean isMaintenanceActive();
    void setMaintenanceActive(boolean value);
    double getThreatIntelConfidence();
    void setThreatIntelConfidence(double value);
    Map<String, Long> getLastTechniqueTicks();
    Map<String, Double> getTechniqueEligibility();
    double getEntityBaselineConfidence();
    void setEntityBaselineConfidence(double value);
    double getAnalystTrustAdjustment();
    void setAnalystTrustAdjustment(double value);
    boolean isBaselineFrozen();
    double getImpactScore();
    List<TemporalThreatEvidence> getEvidenceWindow();
    TemporalThreatEvidence observeEvidence(String type, String entityId, long eventTick,
                                           double confidence, String source, String attackTechnique);
    ThreatHypothesisSignal updateFromTemporalSignature(SignatureMatchSignal signal, String hypothesisId,
                                                       String entityId, long eventTick, String attackTechnique);
    ThreatHypothesisSignal updateFromTemporalAnomaly(AnomalyScoreSignal signal, String hypothesisId,
                                                     long eventTick, String attackTechnique);
    ThreatHypothesisSignal correlate(String hypothesisId, ThreatCategory category, String entityId);
    void resumeBaselineIfSafe(long currentTick, long coolingPeriodTicks,
                              boolean incidentClosed, boolean analystTruePositive);
}
