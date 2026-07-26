/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.ros2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.rakovpublic.jneuropallium.worker.bridge.common.AbstractBridgeAuditOutput;
import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeAuditRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * JSONL audit sink for the ROS 2 bridge (00-FRAMEWORK §4, §6;
 * 04-ROS2-DDS.md §7 {@code audit.localAuditFile}).
 *
 * <p>If {@code audit.advisoryAuditTopic} is configured, each audit record is
 * also published to that ROS 2 advisory topic (a {@code std_msgs/msg/String})
 * via a {@link Ros2ClientService} supplied through
 * {@link #attach(Ros2ClientService, String)}. The mirror is best-effort and
 * never blocks the local file write.
 */
public final class Ros2AuditOutput extends AbstractBridgeAuditOutput {

    private static final Logger log = LoggerFactory.getLogger(Ros2AuditOutput.class);
    private static final ObjectMapper JSON =
            new ObjectMapper().disable(SerializationFeature.INDENT_OUTPUT);

    private Ros2ClientService svc;
    private String topic;
    private final Ros2MessageMapper mapper = new Ros2MessageMapper();

    public Ros2AuditOutput(Path file) { super(file); }

    /** Wire up the optional ROS 2 mirror channel. {@code topic == null} disables it. */
    public synchronized void attach(Ros2ClientService svc, String topic) {
        this.svc = svc;
        this.topic = topic;
    }

    @Override
    protected void mirror(BridgeAuditRecord record) {
        Ros2ClientService s = this.svc;
        String t = this.topic;
        if (s == null || t == null || t.isBlank()) return;
        try {
            String body = JSON.writeValueAsString(record);
            s.publishRaw(t, mapper.encodeStdString(body));
        } catch (Exception e) {
            log.debug("Audit ROS 2 mirror skipped: {}", e.getMessage());
        }
    }
}
