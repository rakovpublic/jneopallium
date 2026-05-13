/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.fhir;

import com.rakovpublic.jneuropallium.worker.bridge.common.AbstractBridgeAuditOutput;

import java.nio.file.Path;

/**
 * JSONL audit sink for the FHIR bridge (00-FRAMEWORK §4, §6;
 * 06-FHIR.md §6 {@code audit.localAuditFile}).
 *
 * <p>06-FHIR.md §10 R1 — every audit record carries the pseudonymous id,
 * never the raw cohort identifier; {@link FhirAdvisoryOutputAggregator}
 * is the single producer. The sink does not mirror to any external
 * channel by default — clinical-data egress is operator-controlled per
 * §11 (regulatory posture).
 */
public final class FhirAuditOutput extends AbstractBridgeAuditOutput {
    public FhirAuditOutput(Path file) { super(file); }
}
