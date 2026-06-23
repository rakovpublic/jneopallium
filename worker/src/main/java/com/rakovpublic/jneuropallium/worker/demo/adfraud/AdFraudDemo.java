/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.demo.adfraud;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.adfraud.AdFraudDecision;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.adfraud.AdFraudEvent;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.adfraud.AdFraudEventType;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.adfraud.AdFraudRuntimeScorer;

public final class AdFraudDemo {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private AdFraudDemo() {
    }

    public static void main(String[] args) throws Exception {
        AdFraudEvent event = new AdFraudEvent("demo-click-1", AdFraudEventType.CLICK, 1_800_000_000_000L);
        event.sessionId = "s-demo";
        event.publisherId = "pub-demo";
        event.campaignId = "camp-demo";
        event.signaturePresent = false;
        event.automationFlag = true;
        event.headlessFlag = true;
        event.adsTxtAuthorized = false;
        event.pointerEventCount = 1;
        event.pointerVelocityEntropy = 0.01;
        event.dwellMs = 25L;
        AdFraudDecision decision = new AdFraudRuntimeScorer().score(event);
        System.out.println(MAPPER.writeValueAsString(decision));
    }
}
