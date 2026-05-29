/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.demo.industrial;

import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeSafetyMode;
import com.rakovpublic.jneuropallium.worker.bridge.mqtt.MqttBridgeConfig;
import com.rakovpublic.jneuropallium.worker.bridge.mqtt.MqttBridgeConfigLoader;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Proves the documented demo-02 YAML parses under the bridge loader's strict
 * {@code FAIL_ON_UNKNOWN_PROPERTIES} contract, structurally enforces the
 * ADVISORY ceiling (02-MQTT-SPARKPLUG.md §3, §9 S9), and agrees with the
 * in-code {@link Demo02Config} used by the runner and acceptance test.
 */
class Demo02ConfigYamlTest {

    /* ---------- canonical YAML loads and matches Demo02Config ---------- */
    @Test
    void documentedYaml_loadsAndMatchesInCodeConfig() throws Exception {
        MqttBridgeConfig yaml;
        try (InputStream in = getClass().getResourceAsStream("/demo/demo02-pumps.yaml")) {
            assertNotNull(in, "demo02-pumps.yaml must be on the test classpath");
            yaml = MqttBridgeConfigLoader.load(in);
        }

        assertEquals(Demo02Config.BROKER_URL, yaml.connection().brokerUrl());
        assertEquals(Demo02Config.CLIENT_ID, yaml.connection().clientId());
        assertFalse(yaml.connection().cleanSession());
        assertEquals(Duration.ofSeconds(30), yaml.connection().keepAlive());
        assertEquals(Demo02Config.ADVISORY_QUEUE_SIZE, yaml.connection().advisoryQueueSize());

        assertTrue(yaml.sparkplug().enabled());
        assertEquals(Demo02Config.GROUP_ID, yaml.sparkplug().groupId());
        assertEquals(Demo02Config.JNEOPALLIUM_EDGE, yaml.sparkplug().edgeNodeId());
        assertEquals(Demo02Config.ADVISORY_NAMESPACE, yaml.sparkplug().advisoryNamespace());

        assertEquals(2, yaml.reads().size());
        assertEquals(Demo02Config.vibrationBindingId("P01"), yaml.reads().get(0).bindingId());
        assertEquals(Demo02Config.sparkplugMetric("P01", Demo02Config.METRIC_VIBRATION),
                yaml.reads().get(0).sparkplugMetric());
        assertEquals(Demo02Config.vibrationTag("P01"), yaml.reads().get(0).signalTag());
        assertEquals(Demo02Config.bearingTempBindingId("P01"), yaml.reads().get(1).bindingId());
        assertEquals(Demo02Config.sparkplugMetric("P01", Demo02Config.METRIC_TEMP),
                yaml.reads().get(1).sparkplugMetric());

        assertEquals(1, yaml.writes().size());
        MqttBridgeConfig.WriteBindingConfig w = yaml.writes().get(0);
        assertEquals(Demo02Config.maintenanceBindingId("P01"), w.bindingId());
        assertEquals(Demo02Config.advisoryTopic("P01"), w.advisoryTopic());
        assertEquals(Demo02Config.maintenanceTag("P01"), w.signalTag());
        assertEquals(0.0, w.minClampValue());
        assertEquals(Demo02Config.MAX_ADVISORY_HOURS, w.maxClampValue());
        assertEquals(1, w.qos());

        assertEquals(BridgeSafetyMode.ADVISORY,
                yaml.perTagSafetyMode().get(Demo02Config.maintenanceBindingId("P01")));
        assertEquals(MqttBridgeConfig.AlarmPriorityName.HIGH, yaml.severityMap().get("HIGH_VIB"));
        assertEquals(Demo02Config.TICK, yaml.tickInterval());

        assertNotNull(yaml.audit());
        assertEquals("/tmp/jneopallium-demo02-audit.jsonl", yaml.audit().localAuditFile());

        // The in-code factory (built for a one-pump slice) matches the YAML.
        MqttBridgeConfig coded = Demo02Config.build(
                "/tmp/jneopallium-demo02-audit.jsonl", List.of("P01"));
        assertEquals(yaml.connection().brokerUrl(), coded.connection().brokerUrl());
        assertEquals(yaml.sparkplug().groupId(), coded.sparkplug().groupId());
        assertEquals(yaml.reads().get(0).sparkplugMetric(), coded.reads().get(0).sparkplugMetric());
        assertEquals(yaml.writes().get(0).advisoryTopic(), coded.writes().get(0).advisoryTopic());
        assertEquals(yaml.writes().get(0).signalTag(), coded.writes().get(0).signalTag());
        assertEquals(yaml.perTagSafetyMode().get(Demo02Config.maintenanceBindingId("P01")),
                coded.perTagSafetyMode().get(Demo02Config.maintenanceBindingId("P01")));
    }

    /* ---------- AUTONOMOUS promotion is structurally rejected ---------- */
    @Test
    void autonomousPromotion_isRejectedByLoader() {
        String yaml = """
                connection:
                  brokerUrl: "tcp://localhost:1883"
                  clientId: "jneopallium-pump-fleet"
                audit:
                  localAuditFile: "/tmp/jneopallium-demo02-audit.jsonl"
                perTagSafetyMode:
                  PUMP-MAINT-ADV-P01: AUTONOMOUS
                """;
        Exception ex = assertThrows(Exception.class, () -> MqttBridgeConfigLoader.load(yaml));
        Throwable t = ex;
        while (t != null && (t.getMessage() == null || !t.getMessage().contains("ADVISORY"))) {
            t = t.getCause();
        }
        assertNotNull(t, "expected the failure to mention the ADVISORY ceiling");
    }

    /* ---------- ADVISORY ceiling holds for the full fleet config ---------- */
    @Test
    void buildFleet_preservesAdvisoryCeiling() {
        MqttBridgeConfig cfg = Demo02Config.build(
                "/tmp/jneopallium-demo02-audit.jsonl",
                Demo02Config.pumpIds(Demo02Config.DEFAULT_FLEET_SIZE));
        for (String pumpId : Demo02Config.pumpIds(Demo02Config.DEFAULT_FLEET_SIZE)) {
            assertEquals(BridgeSafetyMode.ADVISORY,
                    cfg.perTagSafetyMode().get(Demo02Config.maintenanceBindingId(pumpId)),
                    "every fleet write binding must default to ADVISORY");
        }
        assertEquals(40, cfg.reads().size(),
                "20 pumps × 2 metric kinds == 40 read bindings");
        assertEquals(20, cfg.writes().size(),
                "one maintenance-advisory write binding per pump");
    }
}
