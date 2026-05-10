/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.lsl;

import com.rakovpublic.jneuropallium.worker.bridge.common.AbstractBridgeAuditOutput;

import java.nio.file.Path;

/**
 * JSONL audit sink for the LSL bridge (00-FRAMEWORK §4, §6;
 * 05-LSL.md §6 {@code audit.localAuditFile}).
 *
 * <p>05-LSL.md §10 R3 — privacy of physiological data — local-only,
 * ephemeral storage, no cloud egress. The bridge therefore does not
 * mirror records to an external channel by default; subclasses may
 * override {@link AbstractBridgeAuditOutput#mirror} for an air-gapped
 * SIEM if a deployment requires it, but the operator runbook MUST
 * document the consent capture and the destination's retention.
 */
public final class LslAuditOutput extends AbstractBridgeAuditOutput {
    public LslAuditOutput(Path file) { super(file); }
}
