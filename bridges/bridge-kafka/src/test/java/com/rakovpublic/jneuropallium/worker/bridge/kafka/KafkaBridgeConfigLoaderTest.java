/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.kafka;

import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeSafetyMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Loader tests for {@link KafkaBridgeConfigLoader} and validation rules in
 * {@link KafkaBridgeConfig} (08-KAFKA.md §5).
 */
class KafkaBridgeConfigLoaderTest {

    @Test
    void loadsCanonicalSpecYaml() throws Exception {
        String yaml = """
                cluster:
                  bootstrapServers: "kafka01.sec.local:9093"
                  consumerGroupId: "jneopallium-bridge-prod"
                  enableAutoCommit: false
                  maxPollRecords: 500
                reads:
                  - bindingId: "AUTH-LOGS"
                    topic: "logs.security"
                    payloadFormat: "JSON"
                    decoder: "LOGSTASH"
                    targetSignal: "LOG_EVENT"
                    signalTagPrefix: "SEC.AUTH"
                writes:
                  - bindingId: "INCIDENTS"
                    topic: "jneo.advisory.incidents"
                    payloadFormat: "JSON"
                    signalTag: "SEC.INCIDENT"
                audit:
                  localAuditFile: "/tmp/audit.jsonl"
                perTagSafetyMode:
                  INCIDENTS: ADVISORY
                """;
        KafkaBridgeConfig cfg = KafkaBridgeConfigLoader.load(yaml);
        assertEquals("kafka01.sec.local:9093", cfg.cluster().bootstrapServers());
        assertEquals(500, cfg.cluster().maxPollRecords());
        assertEquals(1, cfg.reads().size());
        assertEquals("logs.security", cfg.reads().get(0).topic());
        assertEquals(KafkaBridgeConfig.TargetSignal.LOG_EVENT, cfg.reads().get(0).targetSignal());
        assertEquals(BridgeSafetyMode.ADVISORY, cfg.perTagSafetyMode().get("INCIDENTS"));
    }

    @Test
    void rejectsAutonomousMode() {
        // 08-KAFKA.md §1: this bridge is capped at ADVISORY.
        String yaml = """
                cluster:
                  bootstrapServers: "kafka:9092"
                  consumerGroupId: "g"
                audit:
                  localAuditFile: "/tmp/a.jsonl"
                perTagSafetyMode:
                  INCIDENTS: AUTONOMOUS
                """;
        assertThrows(Exception.class, () -> KafkaBridgeConfigLoader.load(yaml));
    }

    @Test
    void rejectsUnknownProperty() {
        String yaml = """
                cluster:
                  bootstrapServers: "kafka:9092"
                  consumerGroupId: "g"
                  thisFieldDoesNotExist: "oops"
                audit:
                  localAuditFile: "/tmp/a.jsonl"
                """;
        // 00-FRAMEWORK §3: typos must fail loading.
        assertThrows(Exception.class, () -> KafkaBridgeConfigLoader.load(yaml));
    }
}
