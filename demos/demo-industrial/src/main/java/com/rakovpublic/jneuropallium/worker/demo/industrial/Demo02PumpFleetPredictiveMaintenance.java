/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.demo.industrial;

import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeSafetyMode;
import com.rakovpublic.jneuropallium.worker.bridge.mqtt.MqttAdvisoryOutputAggregator;
import com.rakovpublic.jneuropallium.worker.bridge.mqtt.MqttAuditOutput;
import com.rakovpublic.jneuropallium.worker.bridge.mqtt.MqttBridgeConfig;
import com.rakovpublic.jneuropallium.worker.bridge.mqtt.MqttClientService;
import com.rakovpublic.jneuropallium.worker.bridge.mqtt.MqttEventInput;
import com.rakovpublic.jneuropallium.worker.bridge.mqtt.MqttMetricInput;
import com.rakovpublic.jneuropallium.worker.bridge.mqtt.MqttSignalMapper;
import com.rakovpublic.jneuropallium.worker.net.layers.IResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Runnable narrative of demo-02 — the MQTT/Sparkplug pump-fleet predictive
 * maintenance demo from
 * {@code demo-02-pump-fleet-predictive-maintenance.md}.
 *
 * <p>Runs the full read-and-advise loop in-process against
 * {@link PumpFleetSimulator} through the real MQTT bridge classes, walking
 * the documented run procedure: DBIRTH → ramp vibration → RUL falls →
 * maintenance window proposal published to the advisory namespace → device
 * offline → audit. Every decision is written to the audit JSONL
 * ({@code argv[0]}, default {@code /tmp/jneopallium-demo02-audit.jsonl}).
 *
 * <p>For a real over-the-wire run, point a plain {@code DefaultMqttTransport}
 * at a Mosquitto/EMQX/HiveMQ broker (with Sparkplug emitters publishing on
 * {@code spBv1.0/Plant1/DDATA/Edge-Pump/Pxx}) instead of
 * {@link SimulatedPumpFleetMqttTransport} — the rest is identical.
 *
 * <pre>{@code  mvn -q -pl worker exec:java \
 *      -Dexec.mainClass=com.rakovpublic.jneuropallium.worker.demo.industrial.Demo02PumpFleetPredictiveMaintenance }</pre>
 */
public final class Demo02PumpFleetPredictiveMaintenance {

    private static final double DT_SECONDS = 1.0;             // 1 tick == 1 s
    private static final String WEARING_PUMP = "P01";

    public static void main(String[] args) {
        String auditFile = args.length > 0 ? args[0] : "/tmp/jneopallium-demo02-audit.jsonl";
        long run = System.currentTimeMillis();
        Harness h = new Harness(auditFile, run, Demo02Config.DEFAULT_FLEET_SIZE);

        System.out.println("== demo-02 pump fleet predictive maintenance ==");
        System.out.println("audit: " + auditFile);
        System.out.println("fleet: " + h.pumpIds.size() + " pumps, bridge ceiling=ADVISORY\n");

        // [1] DBIRTH every pump so the bridge populates its Sparkplug session cache.
        System.out.println("[1] Emit DBIRTH for all " + h.pumpIds.size() + " pumps");
        h.transport.emitDbirthAll();

        // [2] One pump drifts upward; the others stay flat. Watch the RUL diverge.
        System.out.printf("[2] Ramp vibration_rms on %s by %.3f mm/s/tick — fleet runs 1500 ticks%n",
                WEARING_PUMP, Demo02Config.WEARING_VIB_RAMP_PER_SEC);
        h.simulator.setVibrationRamp(WEARING_PUMP, Demo02Config.WEARING_VIB_RAMP_PER_SEC);
        h.run(1500);
        double rulWearing = h.subnet.rulHours(WEARING_PUMP);
        double rulHealthy = h.subnet.rulHours("P10");
        System.out.printf("    RUL  %s=%.0fh  P10=%.0fh%n",
                WEARING_PUMP, rulWearing, rulHealthy);

        // [3] Confirm a maintenance window proposal was published when RUL crossed the horizon.
        int publishedToWearing = h.transport.publishesTo(Demo02Config.advisoryTopic(WEARING_PUMP)).size();
        System.out.printf("[3] Advisory publishes to %s topic=%d (RUL crossed %.0fh horizon)%n",
                WEARING_PUMP, publishedToWearing, Demo02Config.SCHEDULING_HORIZON_HOURS);

        // [4] The structural ceiling: any AUTONOMOUS promotion must fail at load.
        System.out.println("[4] Prove the ADVISORY ceiling — AUTONOMOUS is structural");
        try {
            new MqttBridgeConfig(
                    h.config.connection(), null, h.config.sparkplug(),
                    h.config.reads(), h.config.writes(), h.config.audit(),
                    Map.of(Demo02Config.maintenanceBindingId(WEARING_PUMP), BridgeSafetyMode.AUTONOMOUS),
                    h.config.severityMap(),
                    h.config.tickInterval());
            System.out.println("    !! loader accepted AUTONOMOUS — this should never happen");
        } catch (IllegalArgumentException expected) {
            System.out.println("    loader rejected AUTONOMOUS as expected: " + expected.getMessage());
        }

        // [5] Take one pump offline. The bridge must emit DEVICE_OFFLINE.
        System.out.println("[5] Send DDEATH for " + WEARING_PUMP
                + " — DEVICE_OFFLINE alarm must surface");
        h.transport.emitDdeath(WEARING_PUMP);
        h.run(1);

        System.out.println("\n== summary ==");
        System.out.printf("  ticks executed:      %d%n", h.subnet.currentTick());
        System.out.printf("  proposals for %s:   %d%n",
                WEARING_PUMP, h.subnet.proposalsFor(WEARING_PUMP));
        System.out.printf("  proposals for P10:   %d%n", h.subnet.proposalsFor("P10"));
        System.out.printf("  advisory publishes:  %d%n", h.transport.allPublishes().size());
        try {
            int auditLines = Files.readAllLines(Path.of(auditFile)).size();
            System.out.printf("  audit lines:         %d (%s)%n", auditLines, auditFile);
        } catch (Exception e) {
            System.out.println("  audit lines:         (read failed: " + e.getMessage() + ")");
        }
        h.close();
    }

    /** Wires the bridge + sub-net for one config "generation". */
    static final class Harness {

        final MqttBridgeConfig config;
        final SimulatedPumpFleetMqttTransport transport;
        final PumpFleetSimulator simulator;
        final MqttAuditOutput audit;
        final MqttClientService svc;
        final MqttSignalMapper mapper;
        final MqttAdvisoryOutputAggregator agg;
        final PumpHealthSubnet subnet;
        final List<MqttMetricInput> vibInputs = new ArrayList<>();
        final List<MqttMetricInput> tempInputs = new ArrayList<>();
        final MqttEventInput eventInput;
        final List<String> pumpIds;
        final long run;

        long ts = 1_740_000_000_000L;
        private static final long TICK_MS = 1_000L;

        Harness(String auditFile, long run, int fleetSize) {
            this.pumpIds = Demo02Config.pumpIds(fleetSize);
            this.run = run;
            this.config = Demo02Config.build(auditFile, pumpIds);
            this.simulator = new PumpFleetSimulator(pumpIds);
            this.transport = new SimulatedPumpFleetMqttTransport(simulator,
                    Demo02Config.GROUP_ID, Demo02Config.EDGE_NODE_ID);
            this.audit = new MqttAuditOutput(Path.of(auditFile));
            this.mapper = new MqttSignalMapper(config);
            this.svc = new MqttClientService(config, transport, mapper, audit);
            this.svc.start();
            this.agg = new MqttAdvisoryOutputAggregator(svc, audit);
            this.subnet = new PumpHealthSubnet(pumpIds);

            for (String pumpId : pumpIds) {
                vibInputs.add(new MqttMetricInput("vib-" + pumpId, svc,
                        List.of(Demo02Config.vibrationBindingId(pumpId))));
                tempInputs.add(new MqttMetricInput("temp-" + pumpId, svc,
                        List.of(Demo02Config.bearingTempBindingId(pumpId))));
            }
            this.eventInput = new MqttEventInput("pump-events", svc);
        }

        void run(int ticks) {
            for (int i = 0; i < ticks; i++) {
                transport.emitDdataTick(DT_SECONDS);
                ts += TICK_MS;
                List<IResult> results = subnet.tick(vibInputs, tempInputs, eventInput, ts);
                agg.save(results, ts, run, null);
            }
        }

        void close() {
            try { svc.close(); } catch (RuntimeException ignored) { /* idempotent */ }
            try { audit.close(); } catch (RuntimeException ignored) { /* idempotent */ }
        }
    }
}
