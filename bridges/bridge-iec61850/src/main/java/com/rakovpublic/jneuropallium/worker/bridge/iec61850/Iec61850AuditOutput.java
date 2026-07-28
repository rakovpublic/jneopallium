/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.iec61850;

import com.rakovpublic.jneuropallium.worker.bridge.common.AbstractBridgeAuditOutput;

import java.nio.file.Path;

/**
 * JSONL audit sink for the IEC 61850 bridge (00-FRAMEWORK §4, §6;
 * 11-IEC61850.md §6 {@code audit.localAuditFile}).
 *
 * <p>The sink does not mirror to any external channel by default —
 * substation-side audit egress is operator-controlled. Operators that
 * want to mirror events to a SCADA historian or the IT side can extend
 * this class and override {@link AbstractBridgeAuditOutput#mirror}.
 */
public final class Iec61850AuditOutput extends AbstractBridgeAuditOutput {
    public Iec61850AuditOutput(Path file) { super(file); }
}
