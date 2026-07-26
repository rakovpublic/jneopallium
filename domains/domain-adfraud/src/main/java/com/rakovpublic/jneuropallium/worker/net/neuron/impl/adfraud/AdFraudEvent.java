/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.adfraud;

import java.util.HashMap;
import java.util.Map;

/**
 * Versioned canonical advertising event. Identifier fields are expected to be
 * HMAC/pseudonymous before this object reaches model artifacts or logs.
 */
public class AdFraudEvent {
    public String schemaVersion = "1.0";
    public String eventId;
    public AdFraudEventType eventType = AdFraudEventType.IMPRESSION;
    public long eventTime;
    public long ingestTime;
    public String impressionId;
    public String clickId;
    public String sessionId;
    public String anonymousUserId;
    public String deviceIdHash;
    public String fingerprintHash;
    public String ipPrefixHash;
    public Integer asn;
    public String publisherId;
    public String siteDomain;
    public String appBundle;
    public String placementId;
    public String creativeId;
    public String campaignId;
    public String advertiserId;
    public String exchangeId;
    public String sellerId;
    public String supplyChain;
    public String country;
    public String deviceType;
    public String os;
    public String browser;
    public String sdkVersion;

    public boolean signaturePresent = true;
    public boolean signatureValid = true;
    public String signatureKeyId;
    public String nonce;
    public boolean nonceReused;
    public Long sourceTimestamp;
    public Long serverReceiveTimestamp;
    public boolean clientEventPresent = true;
    public boolean serverEventPresent = true;
    public boolean deviceAttestationPresent = true;
    public boolean deviceAttestationValid = true;
    public boolean adsTxtAuthorized = true;
    public boolean sellerJsonMatch = true;
    public boolean supplyChainComplete = true;

    public Long pageVisibleMs;
    public Long dwellMs;
    public Double scrollDepth;
    public Integer pointerEventCount;
    public Double pointerDistance;
    public Double pointerVelocityEntropy;
    public Integer touchEventCount;
    public Integer keyboardEventCount;
    public Integer focusChangeCount;
    public boolean interactionBeforeClick;
    public Integer viewportWidth;
    public Integer viewportHeight;
    public Integer clickX;
    public Integer clickY;
    public boolean automationFlag;
    public boolean headlessFlag;
    public Long cookieAgeSeconds;
    public Integer sessionEventCount;

    public Boolean day1Retained;
    public Boolean day7Retained;
    public Boolean day30Retained;
    public Integer meaningfulActionCount;
    public Double purchaseValue;
    public Double refundValue;
    public boolean chargeback;
    public Long uninstallDelay;
    public String customerQualityLabel;
    public String analystLabel;

    public String sourceType = "UNKNOWN";
    public String labelOrigin;
    public double labelConfidence;
    public String scenarioId;
    public Map<String, Double> numericFeatures = new HashMap<>();

    public AdFraudEvent() {
    }

    public AdFraudEvent(String eventId, AdFraudEventType eventType, long eventTime) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.eventTime = eventTime;
        this.ingestTime = eventTime;
    }

    public String stableEntityKey() {
        if (deviceIdHash != null && !deviceIdHash.isBlank()) return "device:" + deviceIdHash;
        if (fingerprintHash != null && !fingerprintHash.isBlank()) return "fingerprint:" + fingerprintHash;
        if (anonymousUserId != null && !anonymousUserId.isBlank()) return "user:" + anonymousUserId;
        return "session:" + nullToUnknown(sessionId);
    }

    public String publisherKey() {
        return nullToUnknown(publisherId) + "|" + nullToUnknown(appBundle) + "|" + nullToUnknown(siteDomain);
    }

    private static String nullToUnknown(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }
}
