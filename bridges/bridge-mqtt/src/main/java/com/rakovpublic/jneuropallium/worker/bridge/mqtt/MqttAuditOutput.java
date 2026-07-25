/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.rakovpublic.jneuropallium.worker.bridge.common.AbstractBridgeAuditOutput;
import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeAuditRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * JSONL audit sink for the MQTT bridge (00-FRAMEWORK §4, §6;
 * 02-MQTT-SPARKPLUG.md §6 {@code audit.localAuditFile}).
 *
 * <p>If {@code audit.mqttAuditTopic} is configured, each audit record is
 * also published to the given MQTT topic via a {@link MqttClientService}
 * supplied through {@link #attach(MqttClientService, String, int)}. The
 * mirror is best-effort and never blocks the local file write.
 */
public final class MqttAuditOutput extends AbstractBridgeAuditOutput {

    private static final Logger log = LoggerFactory.getLogger(MqttAuditOutput.class);
    private static final ObjectMapper JSON =
            new ObjectMapper().disable(SerializationFeature.INDENT_OUTPUT);

    private MqttClientService svc;
    private String topic;
    private int qos = 1;

    public MqttAuditOutput(Path file) { super(file); }

    /** Wire up the optional MQTT mirror channel. {@code topic == null} disables it. */
    public synchronized void attach(MqttClientService svc, String topic, int qos) {
        this.svc = svc;
        this.topic = topic;
        this.qos = qos;
    }

    @Override
    protected void mirror(BridgeAuditRecord record) {
        MqttClientService s = this.svc;
        String t = this.topic;
        if (s == null || t == null || t.isBlank()) return;
        try {
            s.publishRaw(t, JSON.writeValueAsBytes(record), qos);
        } catch (Exception e) {
            log.debug("Audit MQTT mirror skipped: {}", e.getMessage());
        }
    }
}
