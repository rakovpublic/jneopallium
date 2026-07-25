/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.lti;

import com.rakovpublic.jneuropallium.worker.bridge.common.AbstractBridgeAuditOutput;

import java.nio.file.Path;

/**
 * JSONL audit sink for the LTI / xAPI bridge (00-FRAMEWORK §4, §6;
 * 14-LTI-XAPI.md §6 {@code audit.localAuditFile}).
 *
 * <p>14-LTI-XAPI.md §10 R1 — every audit record carries the pseudonymous
 * actor id, never the raw learner identifier. {@link XapiClientService}
 * and {@link LtiAdvisoryOutputAggregator} are the only producers; the
 * sink does not mirror to an external channel by default.
 */
public final class LtiAuditOutput extends AbstractBridgeAuditOutput {
    public LtiAuditOutput(Path file) { super(file); }
}
