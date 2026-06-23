/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.adfraud;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.Neuron;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AdFraudRuntimeScorer extends Neuron implements IAdFraudScoringNeuron {
    private static final long DEFAULT_TTL_MS = 86_400_000L;

    private final AdFraudModelBundle bundle;
    private final Set<String> seenEventIds = new HashSet<>();
    private final Set<String> seenNonces = new HashSet<>();
    private final Set<String> seenPostbackKeys = new HashSet<>();
    private final Map<String, SessionState> sessions = new HashMap<>();
    private final Map<String, Integer> entityDegree = new HashMap<>();
    private final ArrayDeque<TimedKey> ttlQueue = new ArrayDeque<>();
    private long ttlMs = DEFAULT_TTL_MS;
    private AdFraudRuntimeMode mode = AdFraudRuntimeMode.ADVISORY;

    public AdFraudRuntimeScorer() {
        this(AdFraudModelBundle.loadDefault());
    }

    public AdFraudRuntimeScorer(AdFraudModelBundle bundle) {
        super();
        this.bundle = bundle == null ? AdFraudModelBundle.rulesOnlyFallback() : bundle;
        this.currentNeuronClass = AdFraudRuntimeScorer.class;
    }

    public AdFraudRuntimeScorer(Long neuronId, ISignalChain chain, Long run) {
        super(neuronId, chain, run);
        this.bundle = AdFraudModelBundle.loadDefault();
        this.currentNeuronClass = AdFraudRuntimeScorer.class;
    }

    @Override
    public AdFraudDecision score(AdFraudEvent event) {
        long now = event == null ? System.currentTimeMillis() : Math.max(event.ingestTime, event.eventTime);
        evictExpired(now);
        if (event == null) {
            AdFraudDecision empty = new AdFraudDecision();
            empty.setModelVersion(bundle.getVersion());
            empty.setMode(mode);
            empty.setMlFallbackUsed(!bundle.isVerified());
            empty.addReason("missing event payload");
            empty.setRecommendedAction(AdFraudResponseAction.REQUEST_MORE_EVIDENCE);
            return empty;
        }

        List<String> reasons = new ArrayList<>();
        boolean duplicate = false;
        if (event.eventId != null && !seenEventIds.add(event.eventId)) {
            duplicate = true;
            reasons.add("duplicate event id");
        } else if (event.eventId != null) {
            ttlQueue.addLast(new TimedKey("event", event.eventId, now));
        }
        if (event.nonce != null && !seenNonces.add(event.nonce)) {
            event.nonceReused = true;
            reasons.add("nonce reused");
        } else if (event.nonce != null) {
            ttlQueue.addLast(new TimedKey("nonce", event.nonce, now));
        }

        SessionState session = sessions.computeIfAbsent(nullToUnknown(event.sessionId), ignored -> new SessionState());
        updateSession(event, session, reasons);
        Map<String, Double> features = extractFeatures(event, session, duplicate, reasons);
        Map<String, Double> probabilities = new LinkedHashMap<>();
        for (String label : bundle.getLabels()) {
            double learned = bundle.score(label, features);
            double ruleBoost = ruleBoost(label, features);
            probabilities.put(label, clamp(Math.max(learned, ruleBoost)));
        }
        double overall = 1.0;
        for (double p : probabilities.values()) overall *= (1.0 - p);
        overall = 1.0 - overall;

        AdFraudDecision decision = new AdFraudDecision();
        decision.setEventId(event.eventId);
        decision.setModelVersion(bundle.getVersion());
        decision.setMode(mode);
        decision.setMlFallbackUsed(!bundle.isVerified());
        decision.setDuplicateEvent(duplicate);
        decision.setProbabilities(probabilities);
        decision.setOverallInvalidTrafficProbability(overall);
        decision.setExpectedFinancialLoss(overall * Math.max(0.1, nullToZero(event.purchaseValue) + nullToZero(event.refundValue)));
        decision.setEvidenceCompleteness(evidenceCompleteness(event));
        decision.setUncertainty(clamp(1.0 - decision.getEvidenceCompleteness() + (bundle.isVerified() ? 0.05 : 0.25)));
        for (String reason : reasons) decision.addReason(reason);
        if (decision.getReasons().isEmpty()) decision.addReason("score produced from available behavioural, integrity, supply-chain and quality evidence");
        decision.setRecommendedAction(actionFor(overall, probabilities, decision.getEvidenceCompleteness()));
        return decision;
    }

    public Map<String, Double> extractFeatures(AdFraudEvent event, SessionState session, boolean duplicate, List<String> reasons) {
        Map<String, Double> f = new LinkedHashMap<>();
        double integrity = 0.0;
        if (!event.signaturePresent) { integrity += 0.30; reasons.add("signature missing"); }
        if (event.signaturePresent && !event.signatureValid) { integrity += 0.45; reasons.add("signature invalid"); }
        if (event.nonceReused) integrity += 0.35;
        if (duplicate) integrity += 0.55;
        if (!event.clientEventPresent || !event.serverEventPresent) { integrity += 0.20; reasons.add("client/server event disagreement"); }
        if (event.deviceAttestationPresent && !event.deviceAttestationValid) { integrity += 0.20; reasons.add("device attestation invalid"); }
        if (event.sourceTimestamp != null && event.serverReceiveTimestamp != null
                && Math.abs(event.serverReceiveTimestamp - event.sourceTimestamp) > 3_600_000L) {
            integrity += 0.18;
            reasons.add("source and server timestamps differ by more than one hour");
        }
        if (event.eventType == AdFraudEventType.POSTBACK) {
            String key = nullToUnknown(event.clickId) + "|" + nullToUnknown(event.campaignId) + "|" + nullToUnknown(event.deviceIdHash);
            if (!seenPostbackKeys.add(key)) {
                integrity += 0.30;
                reasons.add("duplicated postback payload");
            }
        }
        f.put("integrity_risk", clamp(integrity));

        double bot = 0.0;
        if (event.automationFlag) { bot += 0.35; reasons.add("automation flag present"); }
        if (event.headlessFlag) { bot += 0.30; reasons.add("headless browser flag present"); }
        if (nullToZero(event.pointerVelocityEntropy) < 0.15 && nullToZero(event.pointerEventCount) > 0) bot += 0.20;
        if (nullToZero(event.dwellMs) < 120 && event.eventType == AdFraudEventType.CLICK) bot += 0.18;
        if (nullToZero(event.cookieAgeSeconds) < 5 && event.eventType == AdFraudEventType.CLICK) bot += 0.10;
        f.put("bot_risk", clamp(bot));

        double sequence = 0.0;
        if (event.eventType == AdFraudEventType.CLICK && !session.hasImpression) {
            sequence += 0.45;
            reasons.add("click observed before impression");
        }
        if (isConversion(event.eventType) && !session.hasClick) {
            sequence += 0.45;
            reasons.add("conversion observed before click");
        }
        if (isConversion(event.eventType) && session.clickTime > 0 && event.eventTime - session.clickTime < 700L) {
            sequence += 0.25;
            reasons.add("conversion too close to click");
        }
        f.put("sequence_risk", clamp(sequence));

        double attribution = 0.0;
        if (session.clickCount > 20) { attribution += 0.35; reasons.add("excessive clicks before conversion"); }
        if (isConversion(event.eventType) && session.clickCount > 8) attribution += 0.25;
        if (isConversion(event.eventType) && session.lastClickToInstallMs >= 0 && session.lastClickToInstallMs < 1_000L) {
            attribution += 0.35;
            reasons.add("click injection timing anomaly");
        }
        f.put("attribution_risk", clamp(attribution));

        double supply = 0.0;
        if (!event.adsTxtAuthorized) { supply += 0.30; reasons.add("ads.txt authorization missing"); }
        if (!event.sellerJsonMatch) { supply += 0.25; reasons.add("sellers.json mismatch"); }
        if (!event.supplyChainComplete) { supply += 0.25; reasons.add("supply chain incomplete"); }
        f.put("supply_chain_risk", clamp(supply));

        String graphKey = event.stableEntityKey() + "|" + nullToUnknown(event.publisherId);
        int degree = entityDegree.merge(graphKey, 1, Integer::sum);
        double graph = degree > 10 ? Math.min(0.55, degree / 100.0) : 0.0;
        if (graph > 0.0) reasons.add("device-publisher graph fanout above rolling baseline");
        f.put("graph_risk", graph);

        double quality = 0.0;
        if (Boolean.FALSE.equals(event.day7Retained) && isConversion(event.eventType)) quality += 0.18;
        if (nullToZero(event.meaningfulActionCount) == 0 && isConversion(event.eventType)) quality += 0.12;
        if (event.chargeback || nullToZero(event.refundValue) > 0.0) { quality += 0.30; reasons.add("refund or chargeback evidence present"); }
        if (event.uninstallDelay != null && event.uninstallDelay >= 0 && event.uninstallDelay < 86_400_000L) quality += 0.20;
        f.put("quality_risk", clamp(quality));

        double unknown = clamp((1.0 - evidenceCompleteness(event)) * 0.35 + Math.max(graph, sequence) * 0.25);
        f.put("unknown_risk", unknown);

        // Behavioural-evidence features (improvement #5/#6): family-specific
        // evidence that separates the conflated fraud labels.
        int sessionEvents = (int) nullToZero(event.sessionEventCount);
        boolean conversion = isConversion(event.eventType);
        boolean lowValue = Boolean.FALSE.equals(event.day7Retained) && nullToZero(event.meaningfulActionCount) == 0;

        double clickVolume = 0.0;
        if (event.eventType == AdFraudEventType.CLICK && sessionEvents > 25) {
            clickVolume = clamp(0.45 + Math.min(0.45, (sessionEvents - 25) / 80.0));
        }
        f.put("click_volume_risk", clickVolume);

        double conversionTiming = 0.0;
        if (conversion && sessionEvents > 25) conversionTiming += 0.55;
        if (session.lastClickToInstallMs >= 0 && session.lastClickToInstallMs < 1_000L) conversionTiming += 0.35;
        f.put("conversion_timing_risk", clamp(conversionTiming));

        double incentive = 0.0;
        if (conversion && lowValue) incentive += 0.55;
        if (lowValue && event.uninstallDelay != null) incentive += 0.25;
        f.put("incentive_risk", clamp(incentive));

        double retention = 0.0;
        if (Boolean.FALSE.equals(event.day7Retained)) retention += 0.30;
        if (nullToZero(event.meaningfulActionCount) == 0) retention += 0.20;
        if (event.chargeback || nullToZero(event.refundValue) > 0.0) retention += 0.35;
        f.put("retention_risk", clamp(retention));

        double accidental = 0.0;
        if (nullToZero(event.dwellMs) < 90 && !event.automationFlag && !event.headlessFlag) accidental += 0.35;
        if (!event.interactionBeforeClick && nullToZero(event.dwellMs) < 200) accidental += 0.30;
        if (event.eventType == AdFraudEventType.CLICK && nullToZero(event.meaningfulActionCount) == 0 && !conversion) accidental += 0.20;
        f.put("accidental_risk", clamp(accidental));

        for (Map.Entry<String, Double> entry : event.numericFeatures.entrySet()) {
            f.put(entry.getKey(), entry.getValue());
        }
        return f;
    }

    @Override
    public void resetRuntimeState() {
        seenEventIds.clear();
        seenNonces.clear();
        seenPostbackKeys.clear();
        sessions.clear();
        entityDegree.clear();
        ttlQueue.clear();
    }

    public void setTtlMs(long ttlMs) { this.ttlMs = Math.max(1_000L, ttlMs); }
    public void setMode(AdFraudRuntimeMode mode) { this.mode = mode == null ? AdFraudRuntimeMode.ADVISORY : mode; }
    public AdFraudModelBundle getBundle() { return bundle; }

    private void updateSession(AdFraudEvent event, SessionState session, List<String> reasons) {
        session.eventCount++;
        if (event.campaignId != null && session.campaignId != null && !session.campaignId.equals(event.campaignId)) {
            reasons.add("campaign changed within session");
        }
        if (event.campaignId != null) session.campaignId = event.campaignId;
        if (event.eventType == AdFraudEventType.IMPRESSION || event.eventType == AdFraudEventType.VIEWABLE_IMPRESSION) {
            session.hasImpression = true;
            session.impressionTime = event.eventTime;
        } else if (event.eventType == AdFraudEventType.CLICK) {
            session.clickCount++;
            session.hasClick = true;
            session.clickTime = event.eventTime;
        } else if (event.eventType == AdFraudEventType.LANDING) {
            session.hasLanding = true;
        } else if (event.eventType == AdFraudEventType.INTERACTION) {
            session.hasInteraction = true;
        } else if (isConversion(event.eventType)) {
            session.hasConversion = true;
            if (session.clickTime > 0) session.lastClickToInstallMs = event.eventTime - session.clickTime;
        }
    }

    private void evictExpired(long now) {
        while (!ttlQueue.isEmpty() && now - ttlQueue.peekFirst().timeMs > ttlMs) {
            TimedKey key = ttlQueue.removeFirst();
            if ("event".equals(key.kind)) seenEventIds.remove(key.value);
            if ("nonce".equals(key.kind)) seenNonces.remove(key.value);
        }
    }

    private static AdFraudResponseAction actionFor(double overall, Map<String, Double> p, double completeness) {
        if (completeness < 0.45) return AdFraudResponseAction.REQUEST_MORE_EVIDENCE;
        if (overall < 0.25) return AdFraudResponseAction.ALLOW;
        if (overall < 0.50) return AdFraudResponseAction.MONITOR;
        if (maxOf(p, "eventSpoofing", "inventorySpoofing") >= 0.80) return AdFraudResponseAction.REJECT_EVENT_CANDIDATE;
        if (maxOf(p, "clickFarm", "incentivized") >= 0.75) return AdFraudResponseAction.HOLD_PAYOUT_CANDIDATE;
        if (maxOf(p, "clickSpam", "clickInjection", "attributionHijack") >= 0.65) return AdFraudResponseAction.DISCOUNT_CANDIDATE;
        return AdFraudResponseAction.ADVISORY_REVIEW;
    }

    private static double maxOf(Map<String, Double> p, String... keys) {
        double result = 0.0;
        for (String key : keys) result = Math.max(result, p.getOrDefault(key, 0.0));
        return result;
    }

    private static double ruleBoost(String label, Map<String, Double> f) {
        return switch (label) {
            case "bot" -> f.getOrDefault("bot_risk", 0.0);
            case "eventSpoofing" -> f.getOrDefault("integrity_risk", 0.0);
            case "clickSpam" -> f.getOrDefault("attribution_risk", 0.0) * 0.8;
            case "clickInjection" -> Math.max(f.getOrDefault("attribution_risk", 0.0), f.getOrDefault("sequence_risk", 0.0)) * 0.85;
            case "attributionHijack" -> Math.max(f.getOrDefault("attribution_risk", 0.0), f.getOrDefault("sequence_risk", 0.0)) * 0.75;
            case "inventorySpoofing" -> f.getOrDefault("supply_chain_risk", 0.0);
            case "clickFarm" -> Math.max(f.getOrDefault("graph_risk", 0.0), f.getOrDefault("quality_risk", 0.0) * 0.8);
            case "incentivized" -> f.getOrDefault("quality_risk", 0.0);
            case "accidentalOrLowValue" -> f.getOrDefault("quality_risk", 0.0) * 0.6;
            case "unknownSuspicious" -> f.getOrDefault("unknown_risk", 0.0);
            default -> 0.0;
        };
    }

    private static double evidenceCompleteness(AdFraudEvent e) {
        int present = 0;
        int total = 10;
        if (e.signaturePresent) present++;
        if (e.sourceTimestamp != null) present++;
        if (e.serverReceiveTimestamp != null) present++;
        if (e.deviceAttestationPresent) present++;
        if (e.adsTxtAuthorized && e.sellerJsonMatch && e.supplyChainComplete) present++;
        if (e.dwellMs != null) present++;
        if (e.pointerEventCount != null || e.touchEventCount != null) present++;
        if (e.cookieAgeSeconds != null) present++;
        if (e.day7Retained != null || e.meaningfulActionCount != null) present++;
        if (e.publisherId != null && e.campaignId != null) present++;
        return clamp((double) present / total);
    }

    private static boolean isConversion(AdFraudEventType type) {
        return type == AdFraudEventType.INSTALL
                || type == AdFraudEventType.REGISTRATION
                || type == AdFraudEventType.PURCHASE
                || type == AdFraudEventType.POSTBACK
                || type == AdFraudEventType.PAYOUT;
    }

    private static double nullToZero(Number n) {
        return n == null ? 0.0 : n.doubleValue();
    }

    private static String nullToUnknown(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }

    private static double clamp(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) return 0.0;
        return Math.max(0.0, Math.min(1.0, value));
    }

    public static final class SessionState {
        boolean hasImpression;
        boolean hasClick;
        boolean hasLanding;
        boolean hasInteraction;
        boolean hasConversion;
        int clickCount;
        int eventCount;
        long impressionTime = -1L;
        long clickTime = -1L;
        long lastClickToInstallMs = -1L;
        String campaignId;
    }

    private record TimedKey(String kind, String value, long timeMs) {
    }
}
