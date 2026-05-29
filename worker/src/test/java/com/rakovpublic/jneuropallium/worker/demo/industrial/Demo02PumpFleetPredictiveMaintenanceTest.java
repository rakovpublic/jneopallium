/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.demo.industrial;

import com.google.gson.Gson;
import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeSafetyMode;
import com.rakovpublic.jneuropallium.worker.bridge.mqtt.MqttBridgeConfig;
import com.rakovpublic.jneuropallium.worker.bridge.mqtt.MqttClientService;
import com.rakovpublic.jneuropallium.worker.demo.industrial.Demo02PumpFleetPredictiveMaintenance.Harness;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.AlarmPriority;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.AlarmSignal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end acceptance test for {@code demo-02-pump-fleet-predictive-maintenance.md}.
 *
 * <p>Drives the full read-and-advise loop (simulator → MQTT bridge →
 * pump-health sub-net → aggregator → audit) and asserts every bullet from
 * the demo's "Acceptance" section. The MQTT transport is the in-process
 * {@link SimulatedPumpFleetMqttTransport}; everything else is production
 * code.
 */
class Demo02PumpFleetPredictiveMaintenanceTest {

    private static final Gson GSON = new Gson();
    private static final String WEARING = "P01";
    private static final String FLAT    = "P10";

    /* ---------- RUL decays monotonically with vibration trend ---------- */
    @Test
    void rul_decaysMonotonicallyOnRampedPump_andStaysHigherOnFlatPump(@TempDir Path tmp) {
        Harness h = harness(tmp, 1L);
        h.transport.emitDbirthAll();
        h.simulator.setVibrationRamp(WEARING, Demo02Config.WEARING_VIB_RAMP_PER_SEC);

        double rulStartWearing = h.subnet.rulHours(WEARING);
        double rulStartFlat    = h.subnet.rulHours(FLAT);

        h.run(1500);

        double rulEndWearing = h.subnet.rulHours(WEARING);
        double rulEndFlat    = h.subnet.rulHours(FLAT);

        assertTrue(rulEndWearing < rulStartWearing,
                "wearing pump RUL should decrease: start=" + rulStartWearing
                        + " end=" + rulEndWearing);
        assertTrue(rulEndWearing < rulEndFlat,
                "wearing pump RUL should fall below flat pump RUL: wearing="
                        + rulEndWearing + " flat=" + rulEndFlat);
        // The flat pump still loses a baseline amount of RUL per tick (1 mm/s
        // baseline vibration is the model's "running" wear). It must stay
        // above the scheduling horizon for the demo's 1500-tick window.
        assertTrue(rulEndFlat > Demo02Config.SCHEDULING_HORIZON_HOURS,
                "flat pump RUL must stay above horizon: " + rulEndFlat);
        h.close();
    }

    /* ---------- Maintenance window proposed only at the horizon crossing ---------- */
    @Test
    void maintenanceWindow_proposedAtHorizonCrossing_publishedToAdvisoryTopic(@TempDir Path tmp) throws Exception {
        Harness h = harness(tmp, 2L);
        h.transport.emitDbirthAll();
        h.simulator.setVibrationRamp(WEARING, Demo02Config.WEARING_VIB_RAMP_PER_SEC);
        h.run(1500);

        assertEquals(1, h.subnet.proposalsFor(WEARING),
                "exactly one proposal expected at horizon crossing for " + WEARING);
        assertEquals(0, h.subnet.proposalsFor(FLAT),
                "no proposal expected for flat pump " + FLAT);

        var advisoryPublishes = h.transport.publishesTo(Demo02Config.advisoryTopic(WEARING));
        assertEquals(1, advisoryPublishes.size(),
                "advisory topic should receive exactly one publish");

        // The advisory must go to the per-pump topic, never to a live DCMD actuator topic.
        assertTrue(advisoryPublishes.get(0).topic().contains("/advisory/maint_window"),
                "publishes must land on the advisory namespace: "
                        + advisoryPublishes.get(0).topic());
        assertFalse(h.transport.allPublishes().stream()
                        .map(SimulatedPumpFleetMqttTransport.Published::topic)
                        .anyMatch(t -> !t.contains("/advisory/") && !t.contains("/audit")),
                "no publish may target a live actuator topic outside the advisory namespace");

        // The proposed value should be a positive horizon (hours-ahead) and below the clamp.
        List<Map<String, Object>> audit = audit(h);
        Map<String, Object> applied = audit.stream()
                .filter(r -> "APPLIED".equals(r.get("verdict")))
                .filter(r -> Demo02Config.maintenanceTag(WEARING).equals(r.get("tag")))
                .findFirst().orElse(null);
        assertNotNull(applied, "expected one APPLIED audit line for the proposal");
        double proposed = num(applied.get("proposed"));
        assertTrue(proposed > 0.0 && proposed <= Demo02Config.MAX_ADVISORY_HOURS,
                "proposed hours-ahead must be in (0, 8760]: " + proposed);
        h.close();
    }

    /* ---------- Structural ADVISORY ceiling: AUTONOMOUS rejected ---------- */
    @Test
    void autonomousPromotion_rejectedByLoader() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                new MqttBridgeConfig(
                        new MqttBridgeConfig.ConnectionConfig(
                                Demo02Config.BROKER_URL, Demo02Config.CLIENT_ID,
                                false, Demo02Config.KEEP_ALIVE, Demo02Config.ADVISORY_QUEUE_SIZE),
                        null,
                        new MqttBridgeConfig.SparkplugConfig(
                                true, Demo02Config.GROUP_ID, Demo02Config.JNEOPALLIUM_EDGE,
                                Demo02Config.ADVISORY_NAMESPACE),
                        List.of(),
                        List.of(new MqttBridgeConfig.WriteBindingConfig(
                                Demo02Config.maintenanceBindingId(WEARING),
                                Demo02Config.advisoryTopic(WEARING),
                                Demo02Config.maintenanceTag(WEARING),
                                null, 0.0, Demo02Config.MAX_ADVISORY_HOURS, 1)),
                        new MqttBridgeConfig.AuditConfig(
                                "/tmp/test-audit.jsonl", null, 1),
                        Map.of(Demo02Config.maintenanceBindingId(WEARING),
                                BridgeSafetyMode.AUTONOMOUS),
                        Map.of(),
                        Demo02Config.TICK));
        assertTrue(ex.getMessage().contains("ADVISORY"),
                "rejection must reference the ADVISORY ceiling: " + ex.getMessage());
    }

    /* ---------- ISA-18.2 alarm suppression / rate-limiting ---------- */
    @Test
    void alarmStorms_areSuppressedByAggregator(@TempDir Path tmp) {
        Harness h = harness(tmp, 3L);
        long ts = 1L;
        // Same tag + code repeated within the 600-tick suppression window.
        AlarmSignal first = new AlarmSignal(AlarmPriority.HIGH, "PUMP.P01", "HIGH_VIB", ts);
        AlarmSignal echo  = new AlarmSignal(AlarmPriority.HIGH, "PUMP.P01", "HIGH_VIB", ts + 100);

        assertNotNull(h.subnet.alarmAggregator().observe(first),
                "first alarm must be forwarded");
        for (int i = 0; i < 10; i++) {
            assertNull(h.subnet.alarmAggregator().observe(echo),
                    "alarm storm (same tag+code, < suppression window) must be suppressed");
        }
        // A distinct condition still passes — suppression is per (tag, code).
        assertNotNull(h.subnet.alarmAggregator().observe(
                        new AlarmSignal(AlarmPriority.LOW, "PUMP.P01", "DEVICE_OFFLINE", ts + 50)),
                "a distinct condition code must not be suppressed");
        h.close();
    }

    /* ---------- DDEATH raises DEVICE_OFFLINE on the event channel ---------- */
    @Test
    void ddeath_emitsDeviceOfflineAlarm(@TempDir Path tmp) {
        Harness h = harness(tmp, 4L);
        h.transport.emitDbirthAll();
        h.run(5);
        h.svc.drainEvents();        // discard any BIRTH-time bookkeeping events

        h.transport.emitDdeath(WEARING);

        boolean offline = h.svc.drainEvents().stream()
                .anyMatch(s -> s instanceof AlarmSignal a
                        && MqttClientService.DEVICE_OFFLINE.equals(a.getConditionCode()));
        assertTrue(offline, "the bridge must emit a DEVICE_OFFLINE alarm");
        h.close();
    }

    /* ---------- Reconnect surfaces a BRIDGE_RECONNECTED advisory ---------- */
    @Test
    void reconnect_emitsBridgeReconnectedAdvisory(@TempDir Path tmp) {
        Harness h = harness(tmp, 5L);
        h.transport.emitDbirthAll();
        h.run(2);
        h.svc.onReconnected();
        boolean reconnected = h.svc.drainEvents().stream()
                .anyMatch(s -> s instanceof AlarmSignal a
                        && MqttClientService.BRIDGE_RECONNECTED.equals(a.getConditionCode()));
        assertTrue(reconnected,
                "reconnect must emit BRIDGE_RECONNECTED on the event channel");
        h.close();
    }

    /* ---------- Audit lines conform to 00-FRAMEWORK §4 ---------- */
    @Test
    void audit_everyLineHasFrameworkShape(@TempDir Path tmp) throws Exception {
        Harness h = harness(tmp, 6L);
        h.transport.emitDbirthAll();
        h.simulator.setVibrationRamp(WEARING, Demo02Config.WEARING_VIB_RAMP_PER_SEC);
        h.run(1500);

        List<Map<String, Object>> recs = audit(h);
        assertFalse(recs.isEmpty(), "audit must contain at least one record");
        for (Map<String, Object> r : recs) {
            for (String k : List.of("ts", "run", "bridge", "verdict", "tag", "safetyMode")) {
                assertTrue(r.containsKey(k), "audit line missing key '" + k + "': " + r);
            }
            assertNotNull(r.get("ts"));
            assertNotNull(r.get("verdict"));
            assertEquals("mqtt", r.get("bridge"));
        }
        h.close();
    }

    /* ---------- Subscription topology: Sparkplug wildcards registered ---------- */
    @Test
    void subscriptions_coverSparkplugGroup(@TempDir Path tmp) {
        Harness h = harness(tmp, 7L);
        // Group-level subscriptions cover NBIRTH/NDEATH/DBIRTH/DDEATH/NDATA/DDATA.
        assertEquals(6, h.transport.subscriptionCount(),
                "expected the six Sparkplug group-level filters");
        h.close();
    }

    /* ===================== helpers ===================== */

    private static Harness harness(Path tmp, long run) {
        String auditFile = tmp.resolve("demo02-audit.jsonl").toString();
        return new Harness(auditFile, run, Demo02Config.DEFAULT_FLEET_SIZE);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> audit(Harness h) throws Exception {
        Path resolved = Path.of(h.config.audit().localAuditFile());
        h.audit.close();   // flush the buffered writer before reading
        return Files.readAllLines(resolved).stream()
                .filter(l -> !l.isBlank())
                .map(l -> (Map<String, Object>) GSON.fromJson(l, Map.class))
                .collect(Collectors.toList());
    }

    private static double num(Object o) {
        return o == null ? Double.NaN : ((Number) o).doubleValue();
    }
}
