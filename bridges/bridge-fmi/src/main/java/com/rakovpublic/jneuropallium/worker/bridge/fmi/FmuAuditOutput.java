/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.fmi;

import com.rakovpublic.jneuropallium.worker.bridge.common.AbstractBridgeAuditOutput;
import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeAuditRecord;

import java.nio.file.Path;

/**
 * Local-file-only audit output for the FMI bridge (03-FMI-FMU.md §6 audit
 * section; 00-FRAMEWORK §4).
 *
 * <p>The FMI bridge has no external audit channel (unlike OPC UA or Kafka),
 * so {@link #mirror} is left as the inherited no-op. The JSONL file is the
 * single source of truth for audit records.
 */
public final class FmuAuditOutput extends AbstractBridgeAuditOutput {

    public FmuAuditOutput(Path file) {
        super(file);
    }

    @Override
    protected void mirror(BridgeAuditRecord record) {
        // intentionally no-op — FMI bridge uses local JSONL only
    }
}
