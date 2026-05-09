/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.mqtt;

import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeSafetyMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Loader tests for {@link MqttBridgeConfigLoader} and validation rules in
 * {@link MqttBridgeConfig} (02-MQTT-SPARKPLUG.md §6, §9 S9).
 */
class MqttBridgeConfigLoaderTest {

    @Test
    void loadsCanonicalSpecYaml() throws Exception {
        String yaml = """
                connection:
                  brokerUrl: "ssl://broker.plant.local:8883"
                  clientId: "jneopallium-bridge-edge01"
                  cleanSession: false
                  keepAlive: "PT30S"
                security:
                  type: "UsernamePassword"
                  username: "jneopallium"
                  passwordEnv: "MQTT_PASSWORD"
                  trustStore: "/etc/jneopallium/mqtt-truststore.jks"
                sparkplug:
                  enabled: true
                  groupId: "Plant1"
                  edgeNodeId: "Jneopallium-Edge-01"
                reads:
                  - bindingId: "TIC-101"
                    sparkplugMetric: "Plant1/Edge-Reactor/Reactor1/temperature"
                    signalTag: "PLANT.TIC101.PV"
                  - bindingId: "AMBIENT-TEMP"
                    plainMqttTopic: "sensors/ambient/temperature"
                    jsonPath: "$.value"
                    signalTag: "FACILITY.AMBIENT.PV"
                writes:
                  - bindingId: "TIC-101-ADV"
                    advisoryTopic: "spBv1.0/Plant1/DCMD/Edge-Reactor/Reactor1/advisory/setpoint_temperature"
                    signalTag: "PLANT.TIC101.SP"
                    minClampValue: 0.0
                    maxClampValue: 100.0
                audit:
                  localAuditFile: "/var/log/jneopallium/mqtt-audit.jsonl"
                  mqttAuditTopic: "spBv1.0/Plant1/DCMD/Edge-Reactor/Jneopallium/audit"
                perTagSafetyMode:
                  TIC-101-ADV: ADVISORY
                """;
        MqttBridgeConfig cfg = MqttBridgeConfigLoader.load(yaml);
        assertEquals("ssl://broker.plant.local:8883", cfg.connection().brokerUrl());
        assertEquals("Plant1", cfg.sparkplug().groupId());
        assertEquals(2, cfg.reads().size());
        assertEquals("PLANT.TIC101.PV", cfg.reads().get(0).signalTag());
        assertEquals("$.value", cfg.reads().get(1).jsonPath());
        assertEquals(1, cfg.writes().size());
        assertEquals(0.0, cfg.writes().get(0).minClampValue());
        assertEquals(100.0, cfg.writes().get(0).maxClampValue());
        assertEquals(BridgeSafetyMode.ADVISORY, cfg.perTagSafetyMode().get("TIC-101-ADV"));
    }

    /** S9: AUTONOMOUS rejected at config — the bridge ceiling is ADVISORY. */
    @Test
    void rejectsAutonomousMode() {
        String yaml = """
                connection:
                  brokerUrl: "tcp://broker:1883"
                  clientId: "c"
                audit:
                  localAuditFile: "/tmp/a.jsonl"
                perTagSafetyMode:
                  X: AUTONOMOUS
                """;
        Exception e = assertThrows(Exception.class, () -> MqttBridgeConfigLoader.load(yaml));
        // The thrown root cause should mention the bridge ceiling.
        Throwable t = e;
        while (t != null && (t.getMessage() == null || !t.getMessage().contains("ADVISORY"))) {
            t = t.getCause();
        }
        assertNotNull(t, "expected the failure to mention the ADVISORY ceiling");
    }

    /** 00-FRAMEWORK §3: typos in config must fail loading, not be silently dropped. */
    @Test
    void rejectsUnknownProperty() {
        String yaml = """
                connection:
                  brokerUrl: "tcp://broker:1883"
                  clientId: "c"
                  thisFieldDoesNotExist: 42
                audit:
                  localAuditFile: "/tmp/a.jsonl"
                """;
        assertThrows(Exception.class, () -> MqttBridgeConfigLoader.load(yaml));
    }

    /** Read binding must declare exactly one of sparkplugMetric or plainMqttTopic. */
    @Test
    void rejectsBindingWithBothSparkplugAndPlainMqtt() {
        String yaml = """
                connection:
                  brokerUrl: "tcp://broker:1883"
                  clientId: "c"
                reads:
                  - bindingId: "X"
                    sparkplugMetric: "g/e/d/m"
                    plainMqttTopic: "t"
                    jsonPath: "$.value"
                audit:
                  localAuditFile: "/tmp/a.jsonl"
                """;
        assertThrows(Exception.class, () -> MqttBridgeConfigLoader.load(yaml));
    }

    /** plainMqttTopic without jsonPath is rejected for measurement bindings. */
    @Test
    void rejectsPlainTopicWithoutJsonPath() {
        String yaml = """
                connection:
                  brokerUrl: "tcp://broker:1883"
                  clientId: "c"
                reads:
                  - bindingId: "X"
                    plainMqttTopic: "t"
                audit:
                  localAuditFile: "/tmp/a.jsonl"
                """;
        assertThrows(Exception.class, () -> MqttBridgeConfigLoader.load(yaml));
    }
}
