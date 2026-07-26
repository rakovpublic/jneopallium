/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.mavlink;

import com.rakovpublic.jneuropallium.ai.signals.fast.HarmVetoSignal;
import com.rakovpublic.jneuropallium.ai.signals.fast.TransparencyLogSignal;
import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeSafetyMode;
import com.rakovpublic.jneuropallium.worker.net.layers.IResult;
import com.rakovpublic.jneuropallium.worker.net.layers.impl.SimpleResultWrapper;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.AlarmPriority;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.swarm.FormationTemplate;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.embodiment.ProprioceptiveSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.AlarmSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.EfficiencySignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.AnomalyScoreSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm.FormationSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm.PeerObservationSignal;
import io.dronefleet.mavlink.common.Attitude;
import io.dronefleet.mavlink.common.BatteryStatus;
import io.dronefleet.mavlink.common.GlobalPositionInt;
import io.dronefleet.mavlink.common.MavSeverity;
import io.dronefleet.mavlink.common.RadioStatus;
import io.dronefleet.mavlink.common.Statustext;
import io.dronefleet.mavlink.common.SysStatus;
import io.dronefleet.mavlink.minimal.Heartbeat;
import io.dronefleet.mavlink.util.EnumValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for the MAVLink bridge (12-MAVLINK.md §10). Covers the
 * universal 00-FRAMEWORK §5 scenarios that apply (S1, S3, S4, S5) plus the
 * bridge-specific scenarios S7–S12. Runs against
 * {@link InMemoryMavlinkTransport} — no SITL required.
 */
class MavlinkBridgeIntegrationTest {

    private static final String CONN = "FLEET-A";

    @TempDir Path tempDir;

    private InMemoryMavlinkTransport transport;
    private MavlinkAuditOutput audit;
    private MavlinkClientService svc;

    @BeforeEach
    void setUp() {
        transport = new InMemoryMavlinkTransport();
    }

    @AfterEach
    void tearDown() {
        if (svc != null) svc.close();
        if (audit != null) audit.close();
    }

    /* ===== framework universal scenarios ===================================== */

    /**
     * S1: a GLOBAL_POSITION_INT delivers a typed Proprioceptive signal within
     * one drain.
     */
    @Test
    void s1_pureReadEmitsTypedSignal() {
        MavlinkBridgeConfig cfg = baseConfig(
                List.of(read("DRONE-1-POS", 1, "GLOBAL_POSITION_INT",
                        MavlinkBridgeConfig.ReadSignalKind.PROPRIOCEPTIVE,
                        "DRONE.1.POS", null, 1)),
                List.of(),
                List.of(),
                Map.of(),
                true);
        bring(cfg);

        // 47.123 deg lat, 8.456 deg lon, 100m alt, 1 m/s east velocity.
        GlobalPositionInt gp = GlobalPositionInt.builder()
                .timeBootMs(1000)
                .lat(471230000)
                .lon(84560000)
                .alt(100_000)
                .relativeAlt(50_000)
                .vx(100).vy(0).vz(0)
                .hdg(9000)
                .build();
        transport.deliver(1, 1, gp);

        List<IInputSignal> signals = svc.drain("DRONE-1-POS");
        assertEquals(1, signals.size());
        ProprioceptiveSignal sig = (ProprioceptiveSignal) signals.get(0);
        assertEquals(47.123, sig.getJointStates()[0], 1e-6);
        assertEquals(8.456, sig.getJointStates()[1], 1e-6);
        assertEquals(100.0, sig.getJointStates()[2], 1e-6);
        assertEquals(1.0, sig.getJointStates()[4], 1e-6);
    }

    /** S3: SHADOW mode rejects writes; nothing reaches the wire. */
    @Test
    void s3_shadowModeRejectsWrite() throws IOException {
        MavlinkBridgeConfig cfg = baseConfig(
                List.of(),
                List.of(),
                List.of(write("HARM-VETO", 0, "STATUSTEXT", "FLEET.HARM_VETO")),
                Map.of("HARM-VETO", BridgeSafetyMode.SHADOW),
                true);
        bring(cfg);

        MavlinkAdvisoryOutputAggregator agg = new MavlinkAdvisoryOutputAggregator(svc, audit);
        HarmVetoSignal hv = new HarmVetoSignal();
        hv.setName("FLEET.HARM_VETO");
        hv.setVetoReason("TEST");
        agg.save(List.of(result(hv)), System.currentTimeMillis(), 1L, null);

        assertEquals(0, transport.sendCount(), "SHADOW mode must not send");
        String log = Files.readString(tempDir.resolve("mavlink-audit.jsonl"));
        assertTrue(log.contains("SHADOW_MODE"), "expected SHADOW_MODE audit, got: " + log);
    }

    /** S4: reconnect drops cache and emits a {@code BRIDGE_RECONNECTED} alarm. */
    @Test
    void s4_reconnectClearsCacheAndEmitsAdvisoryEvent() {
        MavlinkBridgeConfig cfg = baseConfig(
                List.of(read("DRONE-1-POS", 1, "GLOBAL_POSITION_INT", null,
                        "DRONE.1.POS", null, 1)),
                List.of(), List.of(), Map.of(), true);
        bring(cfg);

        transport.deliver(1, 1, GlobalPositionInt.builder().build());
        svc.onReconnected(CONN);

        assertTrue(svc.drain("DRONE-1-POS").isEmpty());
        List<IInputSignal> events = svc.drainEvents();
        assertTrue(events.stream().anyMatch(e ->
                        e instanceof AlarmSignal a
                                && a.getConditionCode() != null
                                && a.getConditionCode().startsWith(MavlinkClientService.BRIDGE_RECONNECTED)),
                "expected BRIDGE_RECONNECTED advisory");
    }

    /* ===== bridge-specific scenarios (12-MAVLINK §10) ======================= */

    /** S7: full SITL-style telemetry produces a typed Proprioceptive signal. */
    @Test
    void s7_sitlTelemetryAttitude() {
        MavlinkBridgeConfig cfg = baseConfig(
                List.of(read("DRONE-1-ATT", 1, "ATTITUDE", null, "DRONE.1.ATT", null, 1)),
                List.of(), List.of(), Map.of(), true);
        bring(cfg);

        Attitude att = Attitude.builder()
                .timeBootMs(1000)
                .roll(0.1f).pitch(0.2f).yaw(0.3f)
                .rollspeed(0.01f).pitchspeed(0.02f).yawspeed(0.03f)
                .build();
        transport.deliver(1, 1, att);

        ProprioceptiveSignal sig = (ProprioceptiveSignal) svc.drain("DRONE-1-ATT").get(0);
        assertEquals(0.1, sig.getJointStates()[0], 1e-6);
        assertEquals(0.3, sig.getJointStates()[2], 1e-6);
    }

    /** S8: multi-system swarm — system 1 sees system 2 as a PeerObservationSignal. */
    @Test
    void s8_multiSystemPeerObservation() {
        MavlinkBridgeConfig cfg = baseConfig(
                List.of(
                        read("DRONE-1-POS", 1, "GLOBAL_POSITION_INT",
                                MavlinkBridgeConfig.ReadSignalKind.PROPRIOCEPTIVE,
                                "DRONE.1.POS", null, 1),
                        read("DRONE-1-SEES-2", 2, "GLOBAL_POSITION_INT",
                                MavlinkBridgeConfig.ReadSignalKind.PEER_OBSERVATION,
                                "DRONE.1.PEER.2.POS", "drone-2", 1)),
                List.of(), List.of(),
                Map.of(),
                true,
                List.of(1, 2, 3));
        bring(cfg);

        GlobalPositionInt gp2 = GlobalPositionInt.builder()
                .lat(471230000).lon(84560000).alt(50_000)
                .vx(0).vy(100).vz(0)
                .build();
        transport.deliver(2, 1, gp2);

        List<IInputSignal> peer = svc.drain("DRONE-1-SEES-2");
        assertEquals(1, peer.size());
        PeerObservationSignal sig = (PeerObservationSignal) peer.get(0);
        assertEquals("drone-2", sig.getPeerId());
        assertEquals(47.123, sig.getPositionLocal()[0], 1e-6);
        assertEquals(1.0, sig.getVelocityLocal()[1], 1e-6);

        // Cross-talk check: system 1's read binding is unaffected.
        assertTrue(svc.drain("DRONE-1-POS").isEmpty());
    }

    /** S9: heartbeat loss → PEER_OFFLINE alarm. */
    @Test
    void s9_heartbeatLossEmitsAlarm() {
        MavlinkBridgeConfig cfg = baseConfig(
                List.of(),
                List.of(), List.of(),
                Map.of(),
                true);
        bring(cfg);

        // Establish liveness for system 7.
        transport.deliver(7, 1, Heartbeat.builder().build());
        long now = System.currentTimeMillis();
        svc.checkHeartbeats(now);
        assertTrue(svc.drainEvents().stream().noneMatch(e ->
                e instanceof AlarmSignal a && "PEER_OFFLINE".equals(a.getConditionCode())));

        // 5 s later — past the 2 s timeout.
        svc.checkHeartbeats(now + 5_000L);
        List<IInputSignal> events = svc.drainEvents();
        assertTrue(events.stream().anyMatch(e ->
                e instanceof AlarmSignal a
                        && "PEER_OFFLINE".equals(a.getConditionCode())
                        && a.getPriority() == AlarmPriority.HIGH),
                "expected PEER_OFFLINE alarm after heartbeat timeout");
    }

    /** S10: forbidden write rejected at config load (COMMAND_LONG, simulatorOnly=false). */
    @Test
    void s10_forbiddenWriteRejectedAtLoad() {
        String yaml = """
                connections:
                  - id: FLEET-A
                    transport: UDP
                    bindPort: 14550
                simulatorOnly: false
                writes:
                  - bindingId: BAD
                    connectionId: FLEET-A
                    messageType: COMMAND_LONG
                    signalTag: BAD.CMDLONG
                audit:
                  localAuditFile: /tmp/x
                """;
        assertThrows(Exception.class, () -> MavlinkBridgeConfigLoader.load(yaml));
    }

    /** S11: battery low advisory → EfficiencySignal + downstream HarmVetoSignal → STATUSTEXT. */
    @Test
    void s11_batteryLowAndHarmVetoStatusText() {
        MavlinkBridgeConfig cfg = baseConfig(
                List.of(read("DRONE-1-BATT", 1, "BATTERY_STATUS", null, "DRONE.1.BATT", null, 1)),
                List.of(),
                List.of(write("HARM-VETO", 0, "STATUSTEXT", "DRONE.1.HARM_VETO")),
                Map.of("HARM-VETO", BridgeSafetyMode.ADVISORY),
                true);
        bring(cfg);

        BatteryStatus bs = BatteryStatus.builder()
                .id(0)
                .batteryRemaining(20)
                .currentBattery(500)
                .build();
        transport.deliver(1, 1, bs);

        EfficiencySignal eff = (EfficiencySignal) svc.drain("DRONE-1-BATT").get(0);
        assertEquals(0.20, eff.getEfficiency(), 1e-9);

        // Downstream emits a HarmVetoSignal.
        MavlinkAdvisoryOutputAggregator agg = new MavlinkAdvisoryOutputAggregator(svc, audit);
        HarmVetoSignal veto = new HarmVetoSignal();
        veto.setName("DRONE.1.HARM_VETO");
        veto.setVetoReason("BATTERY_CRITICAL");
        agg.save(List.of(result(veto)), System.currentTimeMillis(), 1L, null);

        List<InMemoryMavlinkTransport.Sent> sent = transport.sentMessages();
        assertEquals(1, sent.size());
        Statustext st = (Statustext) sent.get(0).payload();
        assertEquals(MavSeverity.MAV_SEVERITY_CRITICAL, st.severity().entry());
        assertTrue(st.text().contains("BATTERY_CRITICAL"), "veto text: " + st.text());
    }

    /**
     * S12: custom dialect roundtrip — the formation encoder produces a
     * {@code STATUSTEXT} payload carrying the template name and slot index.
     * The formation-encoder is exercised directly because
     * {@link FormationSignal} is an {@code ISignal}, not an
     * {@code IResultSignal}, and so cannot reach an aggregator through the
     * {@link IResult} flow today; the encoder here is the seam where the
     * generated {@code jneo.xml} dialect classes will swap in (12-MAVLINK
     * §7).
     */
    @Test
    void s12_customDialectFormationEncoding() {
        var encoder = new MavlinkAdvisoryOutputAggregator.MavlinkAdvisoryEncoder();
        FormationSignal fs = new FormationSignal(FormationTemplate.DIAMOND, 4, null);
        Statustext st = encoder.encodeFormation(fs);
        assertTrue(st.text().contains("DIAMOND"), "formation template: " + st.text());
        assertTrue(st.text().contains("slot=4"), "slot index: " + st.text());
    }

    /* ===== additional defensive scenarios ==================================== */

    /** §11 R1: messages from a system not in expectedSystems are dropped. */
    @Test
    void unexpectedSystemDropped() throws IOException {
        MavlinkBridgeConfig cfg = baseConfig(
                List.of(read("DRONE-1-POS", 1, "GLOBAL_POSITION_INT", null,
                        "DRONE.1.POS", null, 1)),
                List.of(), List.of(),
                Map.of(),
                true,
                List.of(1, 2));
        bring(cfg);

        // System 99 is not in expectedSystems → must be dropped.
        transport.deliver(99, 1, GlobalPositionInt.builder().build());
        assertTrue(svc.drain("DRONE-1-POS").isEmpty());

        String log = Files.readString(tempDir.resolve("mavlink-audit.jsonl"));
        assertTrue(log.contains("UNKNOWN_SYSTEM"), "expected UNKNOWN_SYSTEM audit, got: " + log);
    }

    /** STATUSTEXT events surface as AlarmSignal on the advisory event channel. */
    @Test
    void statusTextEventsSurfaceAsAlarm() {
        MavlinkBridgeConfig cfg = baseConfig(
                List.of(),
                List.of(eventBinding("STATUS-TEXTS", "STATUSTEXT", "DRONE.STATUS")),
                List.of(),
                Map.of(),
                true);
        bring(cfg);

        Statustext st = Statustext.builder()
                .severity(MavSeverity.MAV_SEVERITY_WARNING)
                .text("Pre-arm: not armed")
                .build();
        transport.deliver(1, 1, st);

        List<IInputSignal> events = svc.drainEvents();
        AlarmSignal alarm = (AlarmSignal) events.stream()
                .filter(e -> e instanceof AlarmSignal a && a.getTag() != null
                        && a.getTag().startsWith("DRONE.STATUS"))
                .findFirst().orElseThrow();
        assertEquals(AlarmPriority.HIGH, alarm.getPriority());
        assertTrue(alarm.getConditionCode().contains("Pre-arm"));
    }

    /** RADIO_STATUS below RSSI threshold → AnomalyScoreSignal. */
    @Test
    void radioStatusBelowThresholdEmitsAnomaly() {
        MavlinkBridgeConfig cfg = baseConfig(
                List.of(read("LINK-1", 1, "RADIO_STATUS",
                        MavlinkBridgeConfig.ReadSignalKind.ANOMALY,
                        "DRONE.1.LINK", null, 1)),
                List.of(), List.of(),
                Map.of(),
                true);
        bring(cfg);

        RadioStatus rs = RadioStatus.builder()
                .rssi(10).remrssi(10).noise(10).rxerrors(0).build();
        transport.deliver(1, 1, rs);

        AnomalyScoreSignal a = (AnomalyScoreSignal) svc.drain("LINK-1").get(0);
        assertTrue(a.getDeviationScore() > 0.5,
                "expected high anomaly for low RSSI, got " + a.getDeviationScore());
    }

    /** SYS_STATUS with unhealthy enabled sensor → AlarmSignal on the event channel. */
    @Test
    void sysStatusUnhealthyEmitsAlarm() {
        MavlinkBridgeConfig cfg = baseConfig(
                List.of(),
                List.of(eventBinding("SYS-HEALTH", "SYS_STATUS", "DRONE.HEALTH")),
                List.of(),
                Map.of(),
                true);
        bring(cfg);

        // present=0xff, enabled=0xff, health=0x0f — top 4 sensors enabled but unhealthy.
        SysStatus ss = SysStatus.builder()
                .onboardControlSensorsPresent(EnumValue.create(0xff))
                .onboardControlSensorsEnabled(EnumValue.create(0xff))
                .onboardControlSensorsHealth(EnumValue.create(0x0f))
                .dropRateComm(0)
                .build();
        transport.deliver(1, 1, ss);

        List<IInputSignal> events = svc.drainEvents();
        assertTrue(events.stream().anyMatch(e ->
                e instanceof AlarmSignal a
                        && a.getConditionCode() != null
                        && a.getConditionCode().startsWith("SENSOR_UNHEALTHY")),
                "expected SENSOR_UNHEALTHY alarm");
    }

    /**
     * S5 / framework: an unwritable audit file degrades the audit channel
     * but does not block traffic. We force the writer into degraded mode
     * by pointing the audit path at a child of an existing regular file —
     * {@code Files.createDirectories} cannot create a directory under a
     * file, so the open fails and the bridge keeps running.
     */
    @Test
    void s5_auditFailureDoesNotBlockTraffic() throws IOException {
        Path blockerFile = tempDir.resolve("blocker");
        Files.createFile(blockerFile);
        Path bogusFile = blockerFile.resolve("audit.jsonl");

        MavlinkBridgeConfig cfg = baseConfig(
                List.of(read("DRONE-1-POS", 1, "GLOBAL_POSITION_INT", null,
                        "DRONE.1.POS", null, 1)),
                List.of(), List.of(), Map.of(), true);
        audit = new MavlinkAuditOutput(bogusFile);
        svc = new MavlinkClientService(cfg, Map.of(CONN, transport), audit);
        svc.start();

        transport.deliver(1, 1, GlobalPositionInt.builder()
                .lat(0).lon(0).alt(0).build());
        assertEquals(1, svc.drain("DRONE-1-POS").size(),
                "bridge must keep emitting signals even with a degraded audit channel");
        assertTrue(audit.isDegraded(), "audit channel should be degraded");
    }

    /** TransparencyLogSignal must NEVER be forwarded onto MAVLink (§5). */
    @Test
    void transparencyLogIsNotForwardedToMavlink() {
        MavlinkBridgeConfig cfg = baseConfig(
                List.of(), List.of(),
                List.of(write("AUDIT", 0, "STATUSTEXT", "AUDIT.TAG")),
                Map.of("AUDIT", BridgeSafetyMode.ADVISORY),
                true);
        bring(cfg);

        MavlinkAdvisoryOutputAggregator agg = new MavlinkAdvisoryOutputAggregator(svc, audit);
        TransparencyLogSignal tl = new TransparencyLogSignal();
        tl.setActionPlanId("plan-1");
        tl.setName("AUDIT.TAG");
        agg.save(List.of(result(tl)), System.currentTimeMillis(), 1L, null);

        assertEquals(0, transport.sendCount(),
                "TransparencyLogSignal must not be sent on MAVLink (12-MAVLINK.md §5)");
    }

    /** Audit shape conforms to 00-FRAMEWORK §4. */
    @Test
    void auditShapeMatchesFramework() throws IOException {
        MavlinkBridgeConfig cfg = baseConfig(
                List.of(), List.of(),
                List.of(write("HARM-VETO", 0, "STATUSTEXT", "FLEET.HARM_VETO")),
                Map.of("HARM-VETO", BridgeSafetyMode.ADVISORY),
                true);
        bring(cfg);

        MavlinkAdvisoryOutputAggregator agg = new MavlinkAdvisoryOutputAggregator(svc, audit);
        HarmVetoSignal hv = new HarmVetoSignal();
        hv.setName("FLEET.HARM_VETO");
        hv.setVetoReason("UNIT_TEST");
        agg.save(List.of(result(hv)), 1700000000000L, 42L, null);

        audit.close();
        audit = null;
        String log = Files.readString(tempDir.resolve("mavlink-audit.jsonl"));
        assertTrue(log.contains("\"bridge\":\"mavlink\""), "missing bridge key: " + log);
        assertTrue(log.contains("\"verdict\":\"APPLIED\""), "missing APPLIED verdict: " + log);
        assertTrue(log.contains("\"tag\":\"FLEET.HARM_VETO\""), "missing tag: " + log);
    }

    /** §3 runtime backstop: rebuilding the config to flip simulatorOnly throws. */
    @Test
    void runtimeBackstopForForbiddenMessageType() {
        MavlinkBridgeConfig simCfg = baseConfig(
                List.of(), List.of(),
                List.of(write("SIM-CMDLONG", 0, "COMMAND_LONG", "SIM.CMDLONG")),
                Map.of(),
                true);
        assertThrows(IllegalArgumentException.class, () -> new MavlinkBridgeConfig(
                simCfg.connections(),
                false,
                simCfg.reads(), simCfg.events(), simCfg.writes(), simCfg.audit(),
                simCfg.perTagSafetyMode(), simCfg.tickInterval()));
    }

    /** mavlinkMessageTypeOf converts CamelCase payload class names to MAVLink names. */
    @Test
    void messageTypeNameConversion() {
        assertEquals("GLOBAL_POSITION_INT",
                MavlinkClientService.mavlinkMessageTypeOf(GlobalPositionInt.builder().build()));
        assertEquals("ATTITUDE",
                MavlinkClientService.mavlinkMessageTypeOf(Attitude.builder().build()));
        assertEquals("HEARTBEAT",
                MavlinkClientService.mavlinkMessageTypeOf(Heartbeat.builder().build()));
        assertNull(MavlinkClientService.mavlinkMessageTypeOf(null));
    }

    /* ===== helpers =========================================================== */

    private void bring(MavlinkBridgeConfig cfg) {
        Path file = tempDir.resolve("mavlink-audit.jsonl");
        audit = new MavlinkAuditOutput(file);
        svc = new MavlinkClientService(cfg, Map.of(CONN, transport), audit);
        svc.start();
    }

    private static MavlinkBridgeConfig baseConfig(
            List<MavlinkBridgeConfig.ReadBindingConfig> reads,
            List<MavlinkBridgeConfig.EventBindingConfig> events,
            List<MavlinkBridgeConfig.WriteBindingConfig> writes,
            Map<String, BridgeSafetyMode> safetyModes,
            boolean simulatorOnly) {
        return baseConfig(reads, events, writes, safetyModes, simulatorOnly, List.of());
    }

    private static MavlinkBridgeConfig baseConfig(
            List<MavlinkBridgeConfig.ReadBindingConfig> reads,
            List<MavlinkBridgeConfig.EventBindingConfig> events,
            List<MavlinkBridgeConfig.WriteBindingConfig> writes,
            Map<String, BridgeSafetyMode> safetyModes,
            boolean simulatorOnly,
            List<Integer> expectedSystems) {
        return new MavlinkBridgeConfig(
                List.of(new MavlinkBridgeConfig.ConnectionConfig(
                        CONN, MavlinkBridgeConfig.Transport.UDP,
                        "0.0.0.0", 14550, null, null, expectedSystems)),
                simulatorOnly,
                reads, events, writes,
                new MavlinkBridgeConfig.AuditConfig(
                        Path.of(System.getProperty("java.io.tmpdir"),
                                "mavlink-audit.jsonl").toString()),
                safetyModes,
                null);
    }

    private static MavlinkBridgeConfig.ReadBindingConfig read(
            String id, int systemId, String messageType,
            MavlinkBridgeConfig.ReadSignalKind kind,
            String tag, String peerId, int decimateBy) {
        return new MavlinkBridgeConfig.ReadBindingConfig(
                id, CONN, systemId, 1, messageType, kind, tag, peerId, decimateBy);
    }

    private static MavlinkBridgeConfig.EventBindingConfig eventBinding(
            String id, String messageType, String prefix) {
        return new MavlinkBridgeConfig.EventBindingConfig(
                id, CONN, messageType,
                MavlinkBridgeConfig.ReadSignalKind.ALARM, prefix);
    }

    private static MavlinkBridgeConfig.WriteBindingConfig write(
            String id, int targetSysId, String messageType, String tag) {
        return new MavlinkBridgeConfig.WriteBindingConfig(
                id, CONN, targetSysId, 0, messageType, tag,
                null, null, null, null);
    }

    private static <K extends com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal>
            IResult<K> result(K s) {
        return new SimpleResultWrapper<>(s, 1L);
    }
}
