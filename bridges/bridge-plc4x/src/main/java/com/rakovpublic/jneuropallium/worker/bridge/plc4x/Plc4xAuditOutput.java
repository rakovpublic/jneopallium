/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.plc4x;

import com.rakovpublic.jneuropallium.worker.bridge.common.AbstractBridgeAuditOutput;
import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeAuditRecord;

import java.nio.file.Path;

/**
 * Local-file-only audit output for the PLC4X bridge (01-PLC4X.md §5 audit
 * section; 00-FRAMEWORK §4).
 *
 * <p>PLC4X has no native audit channel (most legacy controllers don't expose
 * one), so {@link #mirror} stays as the inherited no-op. The JSONL file is
 * the single source of truth. Sites that want a SIEM mirror can subclass and
 * override {@link #mirror}.
 */
public final class Plc4xAuditOutput extends AbstractBridgeAuditOutput {

    public Plc4xAuditOutput(Path file) {
        super(file);
    }

    @Override
    protected void mirror(BridgeAuditRecord record) {
        // intentionally no-op — PLC4X bridge uses local JSONL only by default
    }
}
