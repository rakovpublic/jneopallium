/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.mavlink;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeSafetyMode;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests for {@link MavlinkBridgeConfigLoader} (12-MAVLINK.md §6, §10 S10). */
final class MavlinkBridgeConfigLoaderTest {

    private static Throwable rootCause(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null && cur.getCause() != cur) cur = cur.getCause();
        return cur;
    }

    private static final String HAPPY = """
            connections:
              - id: FLEET-A
                transport: UDP
                bindAddress: "0.0.0.0"
                bindPort: 14550
                expectedSystems: [1, 2, 3]
            simulatorOnly: true
            reads:
              - bindingId: DRONE-1-POS
                connectionId: FLEET-A
                systemId: 1
                componentId: 1
                messageType: GLOBAL_POSITION_INT
                targetSignal: PROPRIOCEPTIVE
                signalTag: DRONE.1.POS
              - bindingId: DRONE-1-SEES-2
                connectionId: FLEET-A
                systemId: 2
                messageType: GLOBAL_POSITION_INT
                targetSignal: PEER_OBSERVATION
                signalTag: DRONE.1.PEER.2.POS
                peerId: drone-2
              - bindingId: DRONE-1-BATT
                connectionId: FLEET-A
                systemId: 1
                messageType: BATTERY_STATUS
                targetSignal: EFFICIENCY
                signalTag: DRONE.1.BATT
            events:
              - bindingId: STATUS-TEXTS
                connectionId: FLEET-A
                messageType: STATUSTEXT
                targetSignal: ALARM
                signalTagPrefix: DRONE.STATUS
            writes:
              - bindingId: FORMATION-ADV
                connectionId: FLEET-A
                targetSystemId: 0
                messageType: JNEO_FORMATION
                signalTag: FLEET.FORMATION.ADV
            audit:
              localAuditFile: /tmp/jneopallium/mavlink-audit.jsonl
            perTagSafetyMode:
              FORMATION-ADV: ADVISORY
            """;

    @Test
    void loadsHappyPath() throws IOException {
        MavlinkBridgeConfig cfg = MavlinkBridgeConfigLoader.load(HAPPY);
        assertEquals(1, cfg.connections().size());
        assertEquals(MavlinkBridgeConfig.Transport.UDP, cfg.connections().get(0).transport());
        assertEquals(3, cfg.reads().size());
        assertEquals(1, cfg.events().size());
        assertEquals(1, cfg.writes().size());
        assertTrue(cfg.simulatorOnly());
        assertEquals(BridgeSafetyMode.ADVISORY, cfg.perTagSafetyMode().get("FORMATION-ADV"));
        assertNotNull(cfg.audit());
    }

    @Test
    void simulatorOnlyDefaultsTrue() throws IOException {
        String yaml = """
                connections:
                  - id: C1
                    transport: UDP
                    bindPort: 14550
                audit:
                  localAuditFile: /tmp/x
                """;
        MavlinkBridgeConfig cfg = MavlinkBridgeConfigLoader.load(yaml);
        assertTrue(cfg.simulatorOnly(),
                "simulatorOnly defaults to true (12-MAVLINK.md §3, §6: safe-by-default).");
    }

    /** §10 S10: a write binding for COMMAND_LONG without simulatorOnly must fail loading. */
    @Test
    void rejectsCommandLongWithoutSimulatorOnly() {
        String yaml = """
                connections:
                  - id: C1
                    transport: UDP
                    bindPort: 14550
                simulatorOnly: false
                writes:
                  - bindingId: BAD-CMDLONG
                    connectionId: C1
                    messageType: COMMAND_LONG
                    signalTag: T.X
                audit:
                  localAuditFile: /tmp/x
                """;
        Exception ex = assertThrows(Exception.class, () -> MavlinkBridgeConfigLoader.load(yaml));
        assertTrue(rootCause(ex).getMessage().contains("COMMAND_LONG"));
    }

    @Test
    void rejectsSetModeWithoutSimulatorOnly() {
        String yaml = """
                connections:
                  - id: C1
                    transport: UDP
                    bindPort: 14550
                simulatorOnly: false
                writes:
                  - bindingId: BAD-SETMODE
                    connectionId: C1
                    messageType: SET_MODE
                    signalTag: T.X
                audit:
                  localAuditFile: /tmp/x
                """;
        assertThrows(Exception.class, () -> MavlinkBridgeConfigLoader.load(yaml));
    }

    @Test
    void allowsCommandLongInSimulatorOnlyMode() throws IOException {
        String yaml = """
                connections:
                  - id: C1
                    transport: UDP
                    bindPort: 14550
                simulatorOnly: true
                writes:
                  - bindingId: SIM-CMDLONG
                    connectionId: C1
                    messageType: COMMAND_LONG
                    signalTag: SIM.CMDLONG
                audit:
                  localAuditFile: /tmp/x
                """;
        MavlinkBridgeConfig cfg = MavlinkBridgeConfigLoader.load(yaml);
        assertTrue(cfg.simulatorOnly());
        assertEquals(1, cfg.writes().size());
    }

    @Test
    void rejectsAutonomousPromotionOutsideSimulatorOnly() {
        String yaml = """
                connections:
                  - id: C1
                    transport: UDP
                    bindPort: 14550
                simulatorOnly: false
                writes:
                  - bindingId: ADV
                    connectionId: C1
                    messageType: STATUSTEXT
                    signalTag: T.A
                audit:
                  localAuditFile: /tmp/x
                perTagSafetyMode:
                  ADV: AUTONOMOUS
                """;
        assertThrows(Exception.class, () -> MavlinkBridgeConfigLoader.load(yaml));
    }

    @Test
    void unknownPropertyFailsLoading() {
        String yaml = """
                connections:
                  - id: C1
                    transport: UDP
                    bindPort: 14550
                bogusKey: 42
                audit:
                  localAuditFile: /tmp/x
                """;
        assertThrows(UnrecognizedPropertyException.class, () -> MavlinkBridgeConfigLoader.load(yaml));
    }

    @Test
    void rejectsDuplicateBindingIds() {
        String yaml = """
                connections:
                  - id: C1
                    transport: UDP
                    bindPort: 14550
                reads:
                  - bindingId: SAME
                    connectionId: C1
                    systemId: 1
                    messageType: ATTITUDE
                    signalTag: T.A
                writes:
                  - bindingId: SAME
                    connectionId: C1
                    messageType: STATUSTEXT
                    signalTag: T.B
                audit:
                  localAuditFile: /tmp/x
                """;
        assertThrows(Exception.class, () -> MavlinkBridgeConfigLoader.load(yaml));
    }

    /** §6, §11 R1: systemId not in expectedSystems must fail loading. */
    @Test
    void rejectsSystemIdNotInExpectedSystems() {
        String yaml = """
                connections:
                  - id: C1
                    transport: UDP
                    bindPort: 14550
                    expectedSystems: [1, 2]
                reads:
                  - bindingId: ROGUE
                    connectionId: C1
                    systemId: 99
                    messageType: ATTITUDE
                    signalTag: T.X
                audit:
                  localAuditFile: /tmp/x
                """;
        Exception ex = assertThrows(Exception.class, () -> MavlinkBridgeConfigLoader.load(yaml));
        assertTrue(rootCause(ex).getMessage().contains("expectedSystems"));
    }

    @Test
    void rejectsPeerObservationOnNonGlobalPositionMessageType() {
        String yaml = """
                connections:
                  - id: C1
                    transport: UDP
                    bindPort: 14550
                reads:
                  - bindingId: PEER
                    connectionId: C1
                    systemId: 2
                    messageType: ATTITUDE
                    targetSignal: PEER_OBSERVATION
                    signalTag: T.X
                audit:
                  localAuditFile: /tmp/x
                """;
        assertThrows(Exception.class, () -> MavlinkBridgeConfigLoader.load(yaml));
    }

    @Test
    void rejectsBindingForUnknownConnection() {
        String yaml = """
                connections:
                  - id: C1
                    transport: UDP
                    bindPort: 14550
                reads:
                  - bindingId: ORPHAN
                    connectionId: NOPE
                    systemId: 1
                    messageType: ATTITUDE
                    signalTag: T.X
                audit:
                  localAuditFile: /tmp/x
                """;
        Exception ex = assertThrows(Exception.class, () -> MavlinkBridgeConfigLoader.load(yaml));
        assertTrue(rootCause(ex).getMessage().contains("connectionId"));
    }

    @Test
    void rejectsTcpWithoutHostPort() {
        String yaml = """
                connections:
                  - id: C1
                    transport: TCP
                audit:
                  localAuditFile: /tmp/x
                """;
        assertThrows(Exception.class, () -> MavlinkBridgeConfigLoader.load(yaml));
    }
}
