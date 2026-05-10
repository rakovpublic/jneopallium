/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.ros2;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeSafetyMode;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests for {@link Ros2BridgeConfigLoader} (04-ROS2-DDS.md §7, §10 S8). */
final class Ros2BridgeConfigLoaderTest {

    private static Throwable rootCause(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null && cur.getCause() != cur) cur = cur.getCause();
        return cur;
    }

    private static final String HAPPY = """
            mode: ROSBRIDGE
            rosbridgeUrl: "ws://localhost:9090"
            qosProfile: SENSOR_DATA
            simulatorOnly: false
            reads:
              - bindingId: ROBOT-ODOM
                topic: /robot1/odom
                msgType: nav_msgs/msg/Odometry
                signalTag: ROBOT.R1.ODOM
              - bindingId: PEER-ODOM-2
                topic: /robot2/odom
                msgType: nav_msgs/msg/Odometry
                signalTag: ROBOT.R2.PEER_ODOM
                asPeerObservation: true
                peerId: r2
            writes:
              - bindingId: MISSION-ADVICE
                topic: /jneopallium/advisory/mission_advice
                msgType: std_msgs/msg/String
                signalTag: ROBOT.R1.MISSION_ADV
            audit:
              localAuditFile: /tmp/jneopallium/ros2-audit.jsonl
            perTagSafetyMode:
              MISSION-ADVICE: ADVISORY
            """;

    @Test
    void loadsHappyPath() throws IOException {
        Ros2BridgeConfig cfg = Ros2BridgeConfigLoader.load(HAPPY);
        assertEquals(Ros2BridgeConfig.TransportMode.ROSBRIDGE, cfg.mode());
        assertEquals(2, cfg.reads().size());
        assertEquals(1, cfg.writes().size());
        assertFalse(cfg.simulatorOnly());
        assertEquals(BridgeSafetyMode.ADVISORY, cfg.perTagSafetyMode().get("MISSION-ADVICE"));
        assertNotNull(cfg.audit());
    }

    /** §10 S8: a write binding for /cmd_vel without simulatorOnly must fail loading. */
    @Test
    void rejectsCmdVelWithoutSimulatorOnly() {
        String yaml = """
                mode: ROSBRIDGE
                rosbridgeUrl: "ws://localhost:9090"
                simulatorOnly: false
                writes:
                  - bindingId: BAD-CMDVEL
                    topic: /cmd_vel
                    msgType: geometry_msgs/msg/Twist
                    signalTag: ROBOT.R1.CMDVEL
                audit:
                  localAuditFile: /tmp/jneopallium/ros2-audit.jsonl
                """;
        Exception ex = assertThrows(Exception.class, () -> Ros2BridgeConfigLoader.load(yaml));
        assertTrue(rootCause(ex).getMessage().contains("/cmd_vel"));
    }

    @Test
    void rejectsJointTrajectoryWithoutSimulatorOnly() {
        String yaml = """
                mode: ROSBRIDGE
                rosbridgeUrl: "ws://localhost:9090"
                writes:
                  - bindingId: JT
                    topic: /joint_trajectory
                    msgType: trajectory_msgs/msg/JointTrajectory
                    signalTag: T1
                audit:
                  localAuditFile: /tmp/x
                """;
        assertThrows(Exception.class, () -> Ros2BridgeConfigLoader.load(yaml));
    }

    @Test
    void allowsCmdVelInSimulatorOnlyMode() throws IOException {
        String yaml = """
                mode: ROSBRIDGE
                rosbridgeUrl: "ws://localhost:9090"
                simulatorOnly: true
                writes:
                  - bindingId: SIM-CMDVEL
                    topic: /cmd_vel
                    msgType: geometry_msgs/msg/Twist
                    signalTag: SIM.CMDVEL
                audit:
                  localAuditFile: /tmp/x
                """;
        Ros2BridgeConfig cfg = Ros2BridgeConfigLoader.load(yaml);
        assertTrue(cfg.simulatorOnly());
        assertEquals(1, cfg.writes().size());
    }

    @Test
    void rejectsAutonomousPromotionOutsideSimulatorOnly() {
        String yaml = """
                mode: ROSBRIDGE
                rosbridgeUrl: "ws://localhost:9090"
                simulatorOnly: false
                writes:
                  - bindingId: ADVICE
                    topic: /advisory/mission
                    msgType: std_msgs/msg/String
                    signalTag: ROBOT.R1.ADV
                audit:
                  localAuditFile: /tmp/x
                perTagSafetyMode:
                  ADVICE: AUTONOMOUS
                """;
        assertThrows(Exception.class, () -> Ros2BridgeConfigLoader.load(yaml));
    }

    @Test
    void unknownPropertyFailsLoading() {
        String yaml = """
                mode: ROSBRIDGE
                rosbridgeUrl: "ws://localhost:9090"
                bogusKey: 42
                audit:
                  localAuditFile: /tmp/x
                """;
        assertThrows(UnrecognizedPropertyException.class, () -> Ros2BridgeConfigLoader.load(yaml));
    }

    @Test
    void rejectsDuplicateBindingIds() {
        String yaml = """
                mode: ROSBRIDGE
                rosbridgeUrl: "ws://localhost:9090"
                reads:
                  - bindingId: SAME
                    topic: /a
                    msgType: nav_msgs/msg/Odometry
                    signalTag: T.A
                writes:
                  - bindingId: SAME
                    topic: /advisory/b
                    msgType: std_msgs/msg/String
                    signalTag: T.B
                audit:
                  localAuditFile: /tmp/x
                """;
        assertThrows(Exception.class, () -> Ros2BridgeConfigLoader.load(yaml));
    }

    @Test
    void rejectsPeerObservationOnNonOdometryMsgType() {
        String yaml = """
                mode: ROSBRIDGE
                rosbridgeUrl: "ws://localhost:9090"
                reads:
                  - bindingId: PEER
                    topic: /peer/x
                    msgType: sensor_msgs/msg/JointState
                    signalTag: T.X
                    asPeerObservation: true
                audit:
                  localAuditFile: /tmp/x
                """;
        assertThrows(Exception.class, () -> Ros2BridgeConfigLoader.load(yaml));
    }
}
