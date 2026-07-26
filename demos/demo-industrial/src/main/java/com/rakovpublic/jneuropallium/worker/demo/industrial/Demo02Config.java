/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.demo.industrial;

import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeSafetyMode;
import com.rakovpublic.jneuropallium.worker.bridge.mqtt.MqttBridgeConfig;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Canonical wiring constants and config factory for demo-02, matching
 * {@code /tmp/demo02-pumps.yaml} in
 * {@code demo-02-pump-fleet-predictive-maintenance.md}.
 *
 * <p>The doc's YAML uses Sparkplug {@code +} wildcards (e.g.
 * {@code Plant1/Edge-Pump/+/vibration_rms}) to describe a fleet binding;
 * the bridge dispatches per-device messages, but the resulting
 * {@code MeasurementSignal#tag} comes from the single binding entry, so to
 * keep per-asset RUL tracking the factory here generates one read /
 * write binding per pump (with {@code PUMP-VIB-Pxx} / {@code PUMP-TEMP-Pxx}
 * / {@code PUMP-MAINT-ADV-Pxx} ids and {@code PLANT.PUMP.Pxx.*} tags).
 *
 * <p>The ceiling is structurally {@link BridgeSafetyMode#ADVISORY} — the
 * loader rejects any per-tag {@code AUTONOMOUS} promotion
 * (02-MQTT-SPARKPLUG.md §3, §9 S9). This factory therefore only emits
 * {@code ADVISORY} for every write binding.
 */
public final class Demo02Config {

    private Demo02Config() {}

    /* ---- Sparkplug topology --------------------------------------------- */

    public static final String GROUP_ID         = "Plant1";
    public static final String EDGE_NODE_ID     = "Edge-Pump";
    public static final String ADVISORY_NAMESPACE = "advisory";
    public static final String JNEOPALLIUM_EDGE = "Jneopallium-Reliability";

    /* ---- Sparkplug metric names ---------------------------------------- */

    public static final String METRIC_VIBRATION = "vibration_rms";
    public static final String METRIC_TEMP      = "bearing_temp";

    /* ---- Connection ------------------------------------------------------ */

    public static final String BROKER_URL = "tcp://localhost:1883";
    public static final String CLIENT_ID  = "jneopallium-pump-fleet";
    public static final Duration KEEP_ALIVE = Duration.ofSeconds(30);
    public static final int ADVISORY_QUEUE_SIZE = 10_000;

    /* ---- Tag conventions ----------------------------------------------- */

    public static String vibrationTag(String pumpId)    { return "PLANT.PUMP." + pumpId + ".VIB"; }
    public static String bearingTempTag(String pumpId)  { return "PLANT.PUMP." + pumpId + ".BTEMP"; }
    public static String maintenanceTag(String pumpId)  { return "PLANT.PUMP." + pumpId + ".MAINT_WINDOW"; }

    public static String vibrationBindingId(String pumpId)    { return "PUMP-VIB-" + pumpId; }
    public static String bearingTempBindingId(String pumpId)  { return "PUMP-TEMP-" + pumpId; }
    public static String maintenanceBindingId(String pumpId)  { return "PUMP-MAINT-ADV-" + pumpId; }

    public static String sparkplugMetric(String pumpId, String metric) {
        return GROUP_ID + "/" + EDGE_NODE_ID + "/" + pumpId + "/" + metric;
    }

    public static String advisoryTopic(String pumpId) {
        return "spBv1.0/" + GROUP_ID + "/DCMD/" + EDGE_NODE_ID + "/" + pumpId + "/" + ADVISORY_NAMESPACE
                + "/maint_window";
    }

    /* ---- Audit ---------------------------------------------------------- */

    public static final String AUDIT_MQTT_TOPIC =
            "spBv1.0/" + GROUP_ID + "/DCMD/" + EDGE_NODE_ID + "/" + JNEOPALLIUM_EDGE + "/audit";

    public static final Duration TICK = Duration.ofSeconds(1);

    /* ---- Per-asset modelling parameters -------------------------------- */

    public static final double INITIAL_RUL_HOURS = 8_760.0;     // 1 year
    public static final double WEAR_PER_VIB_UNIT = 1.0;         // hrs RUL consumed per mm/s per tick
    public static final double SCHEDULING_HORIZON_HOURS = 2_000.0;
    public static final long   TICKS_PER_HOUR = 3_600L;         // 1 tick == 1 second (PT1S)
    public static final long   MIN_LEAD_TIME_TICKS = 360_000L;  // ~100 hours head-room before EOL
    public static final long   ALARM_SUPPRESSION_TICKS = 600L;
    public static final double MAX_ADVISORY_HOURS = 8_760.0;
    public static final double WEARING_VIB_RAMP_PER_SEC = 0.005;

    /* ---- Fleet defaults ------------------------------------------------- */

    public static final int    DEFAULT_FLEET_SIZE = 20;

    /** Pump ids "P01".."Pnn" — matches the demo doc's labels. */
    public static List<String> pumpIds(int n) {
        List<String> out = new ArrayList<>(n);
        for (int i = 1; i <= n; i++) out.add(String.format(Locale.ROOT, "P%02d", i));
        return List.copyOf(out);
    }

    /** Convenience overload — 20-pump fleet (matches the spec). */
    public static MqttBridgeConfig build(String auditFile) {
        return build(auditFile, pumpIds(DEFAULT_FLEET_SIZE));
    }

    /**
     * Build the bridge config for one fleet "generation".
     *
     * <p>{@code auditFile} is the local JSONL audit sink path
     * (00-FRAMEWORK §4). Every pump in {@code pumpIds} gets vibration +
     * temperature read bindings and a maintenance-window advisory write
     * binding (all flagged {@link BridgeSafetyMode#ADVISORY}).
     */
    public static MqttBridgeConfig build(String auditFile, List<String> pumpIds) {
        var connection = new MqttBridgeConfig.ConnectionConfig(
                BROKER_URL, CLIENT_ID, false, KEEP_ALIVE, ADVISORY_QUEUE_SIZE);

        var sparkplug = new MqttBridgeConfig.SparkplugConfig(
                true, GROUP_ID, JNEOPALLIUM_EDGE, ADVISORY_NAMESPACE);

        List<MqttBridgeConfig.ReadBindingConfig> reads = new ArrayList<>(pumpIds.size() * 2);
        List<MqttBridgeConfig.WriteBindingConfig> writes = new ArrayList<>(pumpIds.size());
        Map<String, BridgeSafetyMode> perTag = new LinkedHashMap<>(pumpIds.size());

        for (String pumpId : pumpIds) {
            reads.add(new MqttBridgeConfig.ReadBindingConfig(
                    vibrationBindingId(pumpId),
                    sparkplugMetric(pumpId, METRIC_VIBRATION),
                    null, null,
                    vibrationTag(pumpId),
                    MqttBridgeConfig.ReadSignalKind.MEASUREMENT));
            reads.add(new MqttBridgeConfig.ReadBindingConfig(
                    bearingTempBindingId(pumpId),
                    sparkplugMetric(pumpId, METRIC_TEMP),
                    null, null,
                    bearingTempTag(pumpId),
                    MqttBridgeConfig.ReadSignalKind.MEASUREMENT));
            writes.add(new MqttBridgeConfig.WriteBindingConfig(
                    maintenanceBindingId(pumpId),
                    advisoryTopic(pumpId),
                    maintenanceTag(pumpId),
                    null,           // plain JSON advisory — no Sparkplug encode on egress
                    0.0,
                    MAX_ADVISORY_HOURS,
                    1));
            perTag.put(maintenanceBindingId(pumpId), BridgeSafetyMode.ADVISORY);
        }

        Map<String, MqttBridgeConfig.AlarmPriorityName> severity = new LinkedHashMap<>();
        severity.put("HIGH_VIB", MqttBridgeConfig.AlarmPriorityName.HIGH);

        var audit = new MqttBridgeConfig.AuditConfig(auditFile, AUDIT_MQTT_TOPIC, 1);

        return new MqttBridgeConfig(
                connection, null, sparkplug,
                reads, writes, audit,
                perTag, severity, TICK);
    }
}
