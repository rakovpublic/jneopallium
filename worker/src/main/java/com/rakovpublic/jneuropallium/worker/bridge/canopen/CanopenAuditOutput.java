/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.canopen;

import com.rakovpublic.jneuropallium.worker.bridge.common.AbstractBridgeAuditOutput;

import java.nio.file.Path;

/**
 * JSONL audit sink for the CANopen bridge (00-FRAMEWORK §4, §6;
 * 13-CANOPEN.md §6 {@code audit.localAuditFile}).
 *
 * <p>Per §3 the bridge audits every SDO write — they are slow enough
 * (millisecond-scale, bus-arbitration limited) that a per-write JSONL
 * record is not a throughput problem. PDO writes go through the universal
 * §2.2 algorithm in {@link CanopenAdvisoryOutputAggregator}; that algorithm
 * already audits accepted, suppressed, clamped, rate-limited, and rejected
 * verdicts — so the only extra audit responsibility this sink has is the
 * runtime backstop for the index allow-list, which
 * {@link AbstractCanopenClientService} writes directly when it refuses a
 * disallowed write.
 */
public final class CanopenAuditOutput extends AbstractBridgeAuditOutput {
    public CanopenAuditOutput(Path file) { super(file); }
}
