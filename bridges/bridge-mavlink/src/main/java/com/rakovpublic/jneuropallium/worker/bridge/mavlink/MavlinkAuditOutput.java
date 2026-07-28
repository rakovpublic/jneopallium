/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.mavlink;

import com.rakovpublic.jneuropallium.worker.bridge.common.AbstractBridgeAuditOutput;

import java.nio.file.Path;

/**
 * JSONL audit sink for the MAVLink bridge (00-FRAMEWORK §4, §6;
 * 12-MAVLINK.md §6 {@code audit.localAuditFile}).
 *
 * <p>The MAVLink bridge does NOT mirror audits onto the flight bus
 * (12-MAVLINK.md §5: "TransparencyLogSignal: dedicated log channel — NOT
 * MAVLink — keep audit out of the flight bus"). The audit pipe is a local
 * JSONL file only; downstream relays (Kafka via the OpenTelemetry/Kafka
 * bridges) consume it out-of-band.
 */
public final class MavlinkAuditOutput extends AbstractBridgeAuditOutput {

    public MavlinkAuditOutput(Path file) { super(file); }
}
