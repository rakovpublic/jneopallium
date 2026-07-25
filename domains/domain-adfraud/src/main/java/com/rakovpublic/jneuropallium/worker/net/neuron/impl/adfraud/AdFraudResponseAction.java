/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.adfraud;

public enum AdFraudResponseAction {
    ALLOW,
    MONITOR,
    REQUEST_MORE_EVIDENCE,
    ADVISORY_REVIEW,
    DISCOUNT_CANDIDATE,
    HOLD_PAYOUT_CANDIDATE,
    REJECT_EVENT_CANDIDATE
}
