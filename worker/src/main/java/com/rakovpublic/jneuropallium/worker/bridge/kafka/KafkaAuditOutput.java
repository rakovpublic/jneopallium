/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.kafka;

import com.rakovpublic.jneuropallium.worker.bridge.common.AbstractBridgeAuditOutput;

import java.nio.file.Path;

/**
 * JSONL audit sink for the Kafka bridge (00-FRAMEWORK §4, §6;
 * 08-KAFKA.md §5 {@code audit.localAuditFile}).
 *
 * <p>The Kafka bridge does not currently mirror audit records to a Kafka
 * topic — that would be circular for an advisory bridge whose own consumers
 * are Kafka topics. Sites that want a SIEM-bound mirror subclass this and
 * override {@link AbstractBridgeAuditOutput#mirror}.
 */
public final class KafkaAuditOutput extends AbstractBridgeAuditOutput {
    public KafkaAuditOutput(Path file) { super(file); }
}
