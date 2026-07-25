/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.ditto;

import com.rakovpublic.jneuropallium.worker.bridge.common.AbstractBridgeAuditOutput;

import java.nio.file.Path;

/**
 * JSONL audit sink for the Ditto bridge (00-FRAMEWORK §4, §6;
 * 10-DITTO.md §5 {@code audit.localAuditFile}).
 *
 * <p>Ditto does not have a generic external audit channel comparable to
 * Kafka or OpenTelemetry, so this sink only writes locally; the local
 * JSONL conforms to 00-FRAMEWORK §4 and is consumed by the SOC/historian.
 */
public final class DittoAuditOutput extends AbstractBridgeAuditOutput {
    public DittoAuditOutput(Path file) { super(file); }
}
