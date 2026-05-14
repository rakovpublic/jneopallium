/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.integration.nengo;

import com.rakovpublic.jneuropallium.worker.bridge.common.AbstractBridgeAuditOutput;

import java.nio.file.Path;

/**
 * JSONL audit sink for the Nengo integration (00-FRAMEWORK §4, §6;
 * 15-NENGO.md §7 {@code audit.localAuditFile}).
 *
 * <p>The Nengo counterpart is a peer Python process — there is no
 * meaningful external SIEM channel to mirror to, so the default no-op
 * {@code mirror()} from the base class is the right behaviour.
 */
public final class NengoBridgeAuditOutput extends AbstractBridgeAuditOutput {
    public NengoBridgeAuditOutput(Path file) { super(file); }
}
