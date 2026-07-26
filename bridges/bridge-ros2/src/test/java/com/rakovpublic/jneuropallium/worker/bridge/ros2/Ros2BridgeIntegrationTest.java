/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.ros2;

import com.rakovpublic.jneuropallium.ai.signals.fast.HarmVetoSignal;
import com.rakovpublic.jneuropallium.ai.signals.fast.MotorCommandSignal;
import com.rakovpublic.jneuropallium.ai.signals.fast.SensorySignal;
import com.rakovpublic.jneuropallium.ai.signals.fast.TransparencyLogSignal;
import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeSafetyMode;
import com.rakovpublic.jneuropallium.worker.net.layers.IResult;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.embodiment.ProprioceptiveSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.AlarmSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm.PeerObservationSignal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the ROS 2 / DDS bridge. Covers the universal
 * 00-FRAMEWORK §5 scenarios that apply (S1, S3, S4, S5) plus the
 * bridge-specific scenarios S7–S11 from 04-ROS2-DDS.md §10. Runs against
 * {@link InMemoryRos2Transport} — no rosbridge required.
 */
class Ros2BridgeIntegrationTest {

    @TempDir Path tempDir;

    private InMemoryRos2Transport transport;
    private Ros2AuditOutput audit;
    private Ros2ClientService svc;
    private Ros2MessageMapper mapper;

    @BeforeEach
    void setUp() {
        transport = new InMemoryRos2Transport();
        mapper = new Ros2MessageMapper();
    }

    @AfterEach
    void tearDown() {
        if (svc != null) svc.close();
        if (audit != null) audit.close();
    }

    /* ===== framework universal scenarios ====================================== */

    /** S1: a /turtle1/pose delivers a typed signal within one drain. */
    @Test
    void s1_pureReadEmitsTypedSignal() throws Exception {
        Ros2BridgeConfig cfg = baseConfig(
                List.of(readBinding("ROBOT-ODOM", "/robot1/odom",
                        "nav_msgs/msg/Odometry", "ROBOT.R1.ODOM")),
                List.of(),
                Map.of(),
                false);
        bring(cfg);

        transport.deliver("/robot1/odom", odomJson(1.0, 2.0, 3.0, 0.1, 0.2, 0.3));

        List<IInputSignal> signals = svc.drain("ROBOT-ODOM");
        assertEquals(1, signals.size());
        ProprioceptiveSignal sig = (ProprioceptiveSignal) signals.get(0);
        assertArrayEquals(new double[]{1.0, 2.0, 3.0, 0.1, 0.2, 0.3},
                sig.getJointStates(), 1e-9);
    }

    /** S3: SHADOW mode rejects writes; nothing reaches the wire. */
    @Test
    void s3_shadowModeRejectsWrite() throws Exception {
        Ros2BridgeConfig cfg = baseConfig(
                List.of(),
                List.of(writeBinding("MISSION-ADVICE",
                        "/jneopallium/advisory/mission_advice",
                        "std_msgs/msg/String", "ROBOT.R1.MISSION_ADV")),
                Map.of("MISSION-ADVICE", BridgeSafetyMode.SHADOW),
                false);
        bring(cfg);

        Ros2AdvisoryOutputAggregator agg = new Ros2AdvisoryOutputAggregator(svc, audit);
        MotorCommandSignal mc = new MotorCommandSignal(0, new double[]{1.0});
        mc.setName("ROBOT.R1.MISSION_ADV");
        mc.setExecute(true);
        agg.save(List.of(result(mc)), System.currentTimeMillis(), 1L, null);

        assertTrue(transport.published("/jneopallium/advisory/mission_advice").isEmpty(),
                "SHADOW mode must not publish");
        String log = Files.readString(tempDir.resolve("ros2-audit.jsonl"));
        assertTrue(log.contains("SHADOW_MODE"), "expected SHADOW_MODE audit, got: " + log);
    }

    /** S4: reconnect drops the cache and emits a {@code BRIDGE_RECONNECTED} alarm. */
    @Test
    void s4_reconnectClearsCacheAndEmitsAdvisoryEvent() throws Exception {
        Ros2BridgeConfig cfg = baseConfig(
                List.of(readBinding("ROBOT-ODOM", "/robot1/odom",
                        "nav_msgs/msg/Odometry", "ROBOT.R1.ODOM")),
                List.of(),
                Map.of(),
                false);
        bring(cfg);

        transport.deliver("/robot1/odom", odomJson(0.0, 0.0, 0.0, 0.0, 0.0, 0.0));
        // Drop and reconnect.
        svc.onReconnected();

        // Cache cleared: a fresh drain returns empty.
        assertTrue(svc.drain("ROBOT-ODOM").isEmpty());

        List<IInputSignal> events = svc.drainEvents();
        assertTrue(events.stream().anyMatch(e ->
                e instanceof AlarmSignal a
                        && Ros2ClientService.BRIDGE_RECONNECTED.equals(a.getConditionCode())));
    }

    /* ===== bridge-specific scenarios (§10) ================================== */

    /** S7: turtlesim subscribe — a turtle pose is mapped to a ProprioceptiveSignal. */
    @Test
    void s7_turtlesimSubscribePopulatesCache() throws Exception {
        Ros2BridgeConfig cfg = baseConfig(
                List.of(readBinding("TURTLE-POSE", "/turtle1/pose",
                        "turtlesim/msg/Pose", "TURTLE.T1.POSE")),
                List.of(),
                Map.of(),
                false);
        bring(cfg);

        String poseJson = """
                {"x":5.5,"y":4.2,"theta":1.57,"linear_velocity":0.3,"angular_velocity":0.0}""";
        transport.deliver("/turtle1/pose", poseJson);

        List<IInputSignal> signals = svc.drain("TURTLE-POSE");
        assertEquals(1, signals.size());
        ProprioceptiveSignal sig = (ProprioceptiveSignal) signals.get(0);
        assertEquals(5.5, sig.getJointStates()[0], 1e-9);
        assertEquals(1.57, sig.getJointStates()[2], 1e-9);
    }

    /** S8: a config with /cmd_vel without simulatorOnly cannot load. */
    @Test
    void s8_cmdVelRejectedAtLoad() {
        String yaml = """
                mode: ROSBRIDGE
                rosbridgeUrl: "ws://localhost:9090"
                simulatorOnly: false
                writes:
                  - bindingId: BAD
                    topic: /cmd_vel
                    msgType: geometry_msgs/msg/Twist
                    signalTag: BAD.CMDVEL
                audit:
                  localAuditFile: /tmp/x
                """;
        assertThrows(Exception.class, () -> Ros2BridgeConfigLoader.load(yaml));
    }

    /** S9: peer odometry binding emits {@link PeerObservationSignal}, not Proprioceptive. */
    @Test
    void s9_peerObservationSignal() throws Exception {
        Ros2BridgeConfig cfg = baseConfig(
                List.of(readBindingPeer("PEER-ODOM-2", "/robot2/odom",
                        "nav_msgs/msg/Odometry", "ROBOT.R2.PEER_ODOM", "r2")),
                List.of(),
                Map.of(),
                false);
        bring(cfg);

        transport.deliver("/robot2/odom", odomJson(7.0, 8.0, 0.0, 1.5, 0.0, 0.0));
        List<IInputSignal> signals = svc.drain("PEER-ODOM-2");
        assertEquals(1, signals.size());
        PeerObservationSignal sig = (PeerObservationSignal) signals.get(0);
        assertEquals("r2", sig.getPeerId());
        assertArrayEquals(new double[]{7.0, 8.0, 0.0}, sig.getPositionLocal(), 1e-9);
        assertArrayEquals(new double[]{1.5, 0.0, 0.0}, sig.getVelocityLocal(), 1e-9);
    }

    /** S10: decimateBy=10 → only every 10th message becomes a signal. */
    @Test
    void s10_decimationDropsIntermediateMessages() throws Exception {
        Ros2BridgeConfig cfg = baseConfig(
                List.of(readBindingDecimate("CAM", "/camera/image_raw",
                        "sensor_msgs/msg/Image", "ROBOT.R1.CAM", 10)),
                List.of(),
                Map.of(),
                false);
        bring(cfg);

        for (int i = 0; i < 30; i++) {
            transport.deliver("/camera/image_raw", "{\"data\":[" + i + "]}");
        }
        List<IInputSignal> signals = svc.drain("CAM");
        // 30 messages, decimateBy=10 → 3 signals (counters 10, 20, 30).
        assertEquals(3, signals.size());
        assertTrue(signals.get(0) instanceof SensorySignal);
    }

    /** S11: simulator-only mode allows /cmd_vel publish. */
    @Test
    void s11_simulatorOnlyAllowsCmdVel() throws Exception {
        Ros2BridgeConfig cfg = baseConfig(
                List.of(),
                List.of(writeBinding("SIM-CMDVEL", "/cmd_vel",
                        "geometry_msgs/msg/Twist", "SIM.CMDVEL")),
                Map.of(),
                true);
        bring(cfg);

        Ros2AdvisoryOutputAggregator agg = new Ros2AdvisoryOutputAggregator(svc, audit);
        MotorCommandSignal mc = new MotorCommandSignal(0, new double[]{0.5, 0.0, 0.0, 0.0, 0.0, 0.1});
        mc.setName("SIM.CMDVEL");
        mc.setExecute(true);
        agg.save(List.of(result(mc)), System.currentTimeMillis(), 1L, null);

        List<String> publishes = transport.published("/cmd_vel");
        assertEquals(1, publishes.size());
        assertTrue(publishes.get(0).contains("\"linear\""), "Twist payload expected: " + publishes.get(0));
    }

    /**
     * Forbidden-topic backstop at runtime: rebuilding a config to flip
     * {@code simulatorOnly} from {@code true} to {@code false} re-runs the
     * check and throws. This is the second line of defence behind the
     * load-time rejection — there is no path that constructs a non-simulator
     * config with a forbidden write binding.
     */
    @Test
    void runtimeBackstopForForbiddenTopic() {
        Ros2BridgeConfig simCfg = baseConfig(
                List.of(),
                List.of(writeBinding("SIM-CMDVEL", "/cmd_vel",
                        "geometry_msgs/msg/Twist", "SIM.CMDVEL")),
                Map.of(),
                true);
        assertThrows(IllegalArgumentException.class, () -> new Ros2BridgeConfig(
                simCfg.mode(), simCfg.rosbridgeUrl(), simCfg.domainId(),
                simCfg.qosProfile(),
                false,
                simCfg.reads(), simCfg.writes(), simCfg.audit(),
                simCfg.perTagSafetyMode(), simCfg.tickInterval()));
    }

    /** Audit shape conforms to 00-FRAMEWORK §4. */
    @Test
    void auditShapeMatchesFramework() throws Exception {
        Ros2BridgeConfig cfg = baseConfig(
                List.of(),
                List.of(writeBinding("MISSION-ADVICE",
                        "/jneopallium/advisory/mission_advice",
                        "std_msgs/msg/String", "ROBOT.R1.MISSION_ADV")),
                Map.of("MISSION-ADVICE", BridgeSafetyMode.ADVISORY),
                false);
        bring(cfg);

        Ros2AdvisoryOutputAggregator agg = new Ros2AdvisoryOutputAggregator(svc, audit);
        HarmVetoSignal veto = new HarmVetoSignal();
        veto.setName("ROBOT.R1.MISSION_ADV");
        veto.setVetoReason("HARMFUL_ACTION");
        agg.save(List.of(result(veto)), 1700000000000L, 42L, null);

        // Force a flush, then read.
        audit.close();
        audit = null;
        String log = Files.readString(tempDir.resolve("ros2-audit.jsonl"));
        assertTrue(log.contains("\"bridge\":\"ros2\""), "missing bridge key: " + log);
        assertTrue(log.contains("\"verdict\":\"APPLIED\""), "missing APPLIED verdict: " + log);
        assertTrue(log.contains("\"tag\":\"ROBOT.R1.MISSION_ADV\""), "missing tag: " + log);
    }

    /** TransparencyLogSignal flows to the optional audit-mirror topic. */
    @Test
    void transparencyLogMirrorsToAuditTopic() throws Exception {
        Ros2BridgeConfig cfg = new Ros2BridgeConfig(
                Ros2BridgeConfig.TransportMode.ROSBRIDGE,
                "ws://localhost:9090", null,
                Ros2BridgeConfig.QosProfile.SENSOR_DATA,
                false,
                List.of(),
                List.of(),
                new Ros2BridgeConfig.AuditConfig(
                        tempDir.resolve("ros2-audit.jsonl").toString(),
                        "/jneopallium/advisory/audit"),
                Map.of(),
                null);
        bring(cfg);

        Ros2AdvisoryOutputAggregator agg = new Ros2AdvisoryOutputAggregator(svc, audit);
        TransparencyLogSignal tl = new TransparencyLogSignal();
        tl.setActionPlanId("plan-1");
        agg.save(List.of(result(tl)), System.currentTimeMillis(), 1L, null);

        assertFalse(transport.published("/jneopallium/advisory/audit").isEmpty(),
                "audit-mirror topic should have a publish");
    }

    /* ===== helpers ============================================================ */

    private void bring(Ros2BridgeConfig cfg) {
        Path file = tempDir.resolve("ros2-audit.jsonl");
        audit = new Ros2AuditOutput(file);
        svc = new Ros2ClientService(cfg, transport, mapper, audit);
        svc.start();
    }

    private static Ros2BridgeConfig baseConfig(
            List<Ros2BridgeConfig.ReadBindingConfig> reads,
            List<Ros2BridgeConfig.WriteBindingConfig> writes,
            Map<String, BridgeSafetyMode> safetyModes,
            boolean simulatorOnly) {
        return new Ros2BridgeConfig(
                Ros2BridgeConfig.TransportMode.ROSBRIDGE,
                "ws://localhost:9090", null,
                Ros2BridgeConfig.QosProfile.SENSOR_DATA,
                simulatorOnly,
                reads, writes,
                new Ros2BridgeConfig.AuditConfig(
                        java.nio.file.Path.of(System.getProperty("java.io.tmpdir"),
                                "ros2-audit.jsonl").toString(), null),
                safetyModes,
                null);
    }

    private static Ros2BridgeConfig.ReadBindingConfig readBinding(
            String id, String topic, String msgType, String signalTag) {
        return new Ros2BridgeConfig.ReadBindingConfig(
                id, topic, msgType, signalTag, null, false, null, 1, 720, 1_048_576);
    }

    private static Ros2BridgeConfig.ReadBindingConfig readBindingPeer(
            String id, String topic, String msgType, String signalTag, String peerId) {
        return new Ros2BridgeConfig.ReadBindingConfig(
                id, topic, msgType, signalTag, null, true, peerId, 1, 720, 1_048_576);
    }

    private static Ros2BridgeConfig.ReadBindingConfig readBindingDecimate(
            String id, String topic, String msgType, String signalTag, int decimate) {
        return new Ros2BridgeConfig.ReadBindingConfig(
                id, topic, msgType, signalTag, null, false, null, decimate, 720, 1_048_576);
    }

    private static Ros2BridgeConfig.WriteBindingConfig writeBinding(
            String id, String topic, String msgType, String signalTag) {
        return new Ros2BridgeConfig.WriteBindingConfig(
                id, topic, msgType, signalTag, null, null, null, null);
    }

    private static String odomJson(double px, double py, double pz, double vx, double vy, double vz) {
        return ("{\"pose\":{\"pose\":{\"position\":{\"x\":%s,\"y\":%s,\"z\":%s}}},"
                + "\"twist\":{\"twist\":{\"linear\":{\"x\":%s,\"y\":%s,\"z\":%s}}}}")
                .formatted(px, py, pz, vx, vy, vz);
    }

    private static IResult<IResultSignal> result(IResultSignal<?> s) {
        return new IResult<>() {
            @Override public IResultSignal getResult() { return s; }
            @Override public Long getNeuronId() { return 1L; }
        };
    }
}
