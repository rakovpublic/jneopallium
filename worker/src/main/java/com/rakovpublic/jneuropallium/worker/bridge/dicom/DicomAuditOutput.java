/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.dicom;

import com.rakovpublic.jneuropallium.worker.bridge.common.AbstractBridgeAuditOutput;

import java.nio.file.Path;

/**
 * JSONL audit sink for the DICOM bridge (00-FRAMEWORK §4, §6;
 * 07-DICOM.md §6 {@code audit.localAuditFile}).
 *
 * <p>07-DICOM.md §10 R1 — every audit record carries the pseudonymous id,
 * never the raw {@code PatientID}. The sink does not mirror to any
 * external channel by default — radiology metadata egress is
 * operator-controlled.
 */
public final class DicomAuditOutput extends AbstractBridgeAuditOutput {
    public DicomAuditOutput(Path file) { super(file); }
}
