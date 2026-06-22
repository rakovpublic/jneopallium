/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.security;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.AnomalyScoreSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.SignatureMatchSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.ThreatHypothesisSignal;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Layer 4 temporal correlator. Fast evidence raises a decaying threat
 * activation, while slow context changes how that evidence is interpreted.
 */
public class TemporalThreatCorrelationNeuron extends ThreatHypothesisNeuron
        implements ITemporalThreatCorrelationNeuron {

    private static final int DEFAULT_EVIDENCE_WINDOW = 256;
    private static final double WATCH_THRESHOLD = 0.30;

    private final Deque<TemporalThreatEvidence> evidenceWindow = new ArrayDeque<>();
    private final Map<String, Long> lastTechniqueTicks = new HashMap<>();
    private final Map<String, Double> techniqueEligibility = new HashMap<>();

    private int evidenceWindowLimit = DEFAULT_EVIDENCE_WINDOW;
    private double decayFactor = 0.92;
    private double fastThreatActivation;
    private double threatPosterior;
    private double assetCriticality;
    private boolean maintenanceActive;
    private double threatIntelConfidence;
    private double entityBaselineConfidence = 1.0;
    private double analystTrustAdjustment;
    private boolean baselineFrozen;
    private long lastProcessedTick = Long.MIN_VALUE;
    private long lastSuspiciousTick = Long.MIN_VALUE;

    public TemporalThreatCorrelationNeuron() {
        super();
    }

    public TemporalThreatCorrelationNeuron(Long neuronId, ISignalChain chain, Long run) {
        super(neuronId, chain, run);
    }

    @Override
    public TemporalThreatEvidence observeEvidence(String type, String entityId, long eventTick,
                                                  double confidence, String source, String attackTechnique) {
        decayTo(eventTick);
        TemporalThreatEvidence evidence = new TemporalThreatEvidence(type, entityId, eventTick,
                confidence, source, attackTechnique);
        evidenceWindow.addLast(evidence);
        while (evidenceWindow.size() > evidenceWindowLimit) {
            evidenceWindow.removeFirst();
        }
        applyEvidence(evidence);
        return evidence;
    }

    @Override
    public ThreatHypothesisSignal updateFromTemporalSignature(SignatureMatchSignal signal, String hypothesisId,
                                                              String entityId, long eventTick,
                                                              String attackTechnique) {
        if (signal == null) {
            return null;
        }
        ThreatHypothesisSignal base = super.updateFromSignature(signal, hypothesisId);
        observeEvidence("SIGNATURE", entityId, eventTick, signal.getConfidence(),
                signal.getSignatureId(), attackTechnique == null ? signal.getFamily() : attackTechnique);
        return emitTemporal(hypothesisId, categoryFrom(base), entityId);
    }

    @Override
    public ThreatHypothesisSignal updateFromTemporalAnomaly(AnomalyScoreSignal signal, String hypothesisId,
                                                            long eventTick, String attackTechnique) {
        if (signal == null) {
            return null;
        }
        ThreatHypothesisSignal base = super.updateFromAnomaly(signal, hypothesisId);
        observeEvidence("ANOMALY", signal.getEntityId(), eventTick, signal.getDeviationScore(),
                String.join(",", signal.getContributingFeatures()), attackTechnique);
        return emitTemporal(hypothesisId, categoryFrom(base), signal.getEntityId());
    }

    @Override
    public ThreatHypothesisSignal correlate(String hypothesisId, ThreatCategory category, String entityId) {
        return emitTemporal(hypothesisId, category == null ? ThreatCategory.UNKNOWN : category, entityId);
    }

    @Override
    public void resumeBaselineIfSafe(long currentTick, long coolingPeriodTicks,
                                     boolean incidentClosed, boolean analystTruePositive) {
        if (!incidentClosed || analystTruePositive) {
            return;
        }
        long quietTicks = lastSuspiciousTick == Long.MIN_VALUE
                ? Long.MAX_VALUE : Math.max(0L, currentTick - lastSuspiciousTick);
        if (quietTicks > Math.max(0L, coolingPeriodTicks)) {
            baselineFrozen = false;
        }
    }

    private void applyEvidence(TemporalThreatEvidence evidence) {
        String type = normalise(evidence.type());
        String technique = normalise(evidence.attackTechnique());
        double confidence = evidence.confidence();
        double contribution = evidenceWeight(type, confidence);
        if ("ANOMALY".equals(type) && maintenanceActive) {
            contribution *= 0.25;
        }
        if (("NETWORK_FLOW".equals(type) || "DNS_LOOKUP".equals(type) || "PACKET".equals(type))
                && threatIntelConfidence > 0.0) {
            contribution *= (1.0 + threatIntelConfidence);
        }
        if ("BENIGN_CONTEXT".equals(type) || "MAINTENANCE_WINDOW".equals(type)) {
            contribution = -Math.abs(contribution);
        }
        fastThreatActivation = Math.max(0.0, fastThreatActivation + contribution);
        if (!technique.isBlank()) {
            fastThreatActivation += sequenceBoost(technique, evidence.eventTick(), confidence);
            lastTechniqueTicks.put(technique, evidence.eventTick());
            techniqueEligibility.put(technique, confidence);
        }
        if (contribution > 0.0 || "SIGNATURE".equals(type)) {
            lastSuspiciousTick = Math.max(lastSuspiciousTick, evidence.eventTick());
        }
        updatePosterior();
        if (threatPosterior >= WATCH_THRESHOLD || ("SIGNATURE".equals(type) && confidence >= 0.8)) {
            baselineFrozen = true;
        }
    }

    private double sequenceBoost(String technique, long eventTick, double confidence) {
        double boost = 0.0;
        if ("PRIVILEGE_ESCALATION".equals(technique) && seenRecently("UNUSUAL_LOGIN", eventTick, 300)) {
            boost += 0.18;
        }
        if ("CREDENTIAL_ACCESS".equals(technique) && seenRecently("UNUSUAL_LOGIN", eventTick, 600)) {
            boost += 0.25;
        }
        if ("EXECUTION".equals(technique) && seenRecently("UNUSUAL_LOGIN", eventTick, 600)) {
            boost += 0.20;
        }
        if ("LATERAL_MOVEMENT".equals(technique)
                && (seenRecently("CREDENTIAL_ACCESS", eventTick, 600)
                || seenRecently("EXECUTION", eventTick, 600))) {
            boost += 0.35;
        }
        if ("COMMAND_AND_CONTROL".equals(technique)
                && (seenRecently("LATERAL_MOVEMENT", eventTick, 900)
                || seenRecently("DNS_LOOKUP", eventTick, 900))) {
            boost += 0.35;
        }
        if ("EXFILTRATION".equals(technique) && seenRecently("COMMAND_AND_CONTROL", eventTick, 1800)) {
            boost += 0.40;
        }
        return boost * Math.max(0.25, confidence);
    }

    private boolean seenRecently(String technique, long eventTick, long windowTicks) {
        Long last = lastTechniqueTicks.get(technique);
        return last != null && last <= eventTick && eventTick - last <= windowTicks;
    }

    private void decayTo(long eventTick) {
        if (lastProcessedTick == Long.MIN_VALUE) {
            lastProcessedTick = eventTick;
            return;
        }
        long elapsed = Math.max(0L, eventTick - lastProcessedTick);
        if (elapsed > 0L) {
            fastThreatActivation *= Math.pow(decayFactor, elapsed);
            lastProcessedTick = eventTick;
        }
    }

    private void updatePosterior() {
        double adjusted = fastThreatActivation;
        adjusted += threatIntelConfidence * 0.40;
        adjusted += analystTrustAdjustment;
        adjusted -= maintenanceActive ? 0.35 : 0.0;
        adjusted -= Math.max(0.0, entityBaselineConfidence - 1.0) * 0.25;
        adjusted = Math.max(0.0, adjusted);
        threatPosterior = clamp01(1.0 - Math.exp(-adjusted / 1.8));
        if (threatPosterior >= WATCH_THRESHOLD) {
            baselineFrozen = true;
        }
    }

    private ThreatHypothesisSignal emitTemporal(String hypothesisId, ThreatCategory category, String entityId) {
        List<String> evidenceIds = new ArrayList<>();
        for (TemporalThreatEvidence evidence : evidenceWindow) {
            if (entityId == null || entityId.equals(evidence.entityId())) {
                String technique = evidence.attackTechnique() == null ? "" : ":" + evidence.attackTechnique();
                evidenceIds.add(evidence.type() + "@" + evidence.eventTick() + technique);
            }
        }
        return new ThreatHypothesisSignal(hypothesisId, category, threatPosterior, evidenceIds);
    }

    private static ThreatCategory categoryFrom(ThreatHypothesisSignal base) {
        return base == null ? ThreatCategory.UNKNOWN : base.getCategory();
    }

    private static double evidenceWeight(String type, double confidence) {
        return switch (type) {
            case "SIGNATURE" -> confidence * 1.5;
            case "ANOMALY" -> confidence * 0.7;
            case "SEQUENCE" -> confidence * 2.0;
            case "AUTHENTICATION", "PROCESS", "DNS_LOOKUP", "NETWORK_FLOW", "PACKET" -> confidence * 0.45;
            case "BENIGN_CONTEXT", "MAINTENANCE_WINDOW" -> Math.max(0.20, confidence);
            default -> confidence * 0.30;
        };
    }

    private static String normalise(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    @Override public double getFastThreatActivation() { return fastThreatActivation; }
    @Override public void setFastThreatActivation(double value) { this.fastThreatActivation = Math.max(0.0, value); updatePosterior(); }
    @Override public double getThreatPosterior() { return threatPosterior; }
    @Override public void setThreatPosterior(double value) {
        this.threatPosterior = clamp01(value);
        if (threatPosterior >= WATCH_THRESHOLD) {
            baselineFrozen = true;
        }
    }
    @Override public double getAssetCriticality() { return assetCriticality; }
    @Override public void setAssetCriticality(double value) { this.assetCriticality = clamp01(value); }
    @Override public boolean isMaintenanceActive() { return maintenanceActive; }
    @Override public void setMaintenanceActive(boolean value) { this.maintenanceActive = value; updatePosterior(); }
    @Override public double getThreatIntelConfidence() { return threatIntelConfidence; }
    @Override public void setThreatIntelConfidence(double value) { this.threatIntelConfidence = clamp01(value); updatePosterior(); }
    @Override public Map<String, Long> getLastTechniqueTicks() { return Collections.unmodifiableMap(lastTechniqueTicks); }
    @Override public Map<String, Double> getTechniqueEligibility() { return Collections.unmodifiableMap(techniqueEligibility); }
    @Override public double getEntityBaselineConfidence() { return entityBaselineConfidence; }
    @Override public void setEntityBaselineConfidence(double value) { this.entityBaselineConfidence = Math.max(0.0, value); updatePosterior(); }
    @Override public double getAnalystTrustAdjustment() { return analystTrustAdjustment; }
    @Override public void setAnalystTrustAdjustment(double value) { this.analystTrustAdjustment = Math.max(-1.0, Math.min(1.0, value)); updatePosterior(); }
    @Override public boolean isBaselineFrozen() { return baselineFrozen; }
    @Override public double getImpactScore() { return clamp01(threatPosterior * (0.5 + assetCriticality)); }
    @Override public List<TemporalThreatEvidence> getEvidenceWindow() { return List.copyOf(evidenceWindow); }
}
