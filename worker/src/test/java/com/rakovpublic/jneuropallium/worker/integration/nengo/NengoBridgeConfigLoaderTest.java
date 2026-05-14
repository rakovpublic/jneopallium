/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.integration.nengo;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeSafetyMode;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests for {@link NengoBridgeConfigLoader} (15-NENGO.md §7, §11 S15). */
final class NengoBridgeConfigLoaderTest {

    private static Throwable rootCause(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null && cur.getCause() != cur) cur = cur.getCause();
        return cur;
    }

    private static final String HAPPY = """
            transport:
              channelInPath:  "/tmp/jneo-nengo-input.sock"
              channelOutPath: "/tmp/jneo-nengo-output.sock"
              mode: UDS
              reconnectBackoffMs: 250
              reconnectMaxMs:     5000
              frameMaxBytes:      65536
            simulatorOnly: true
            inputMappings:
              - frameLabel: "dx_target"
                signal: SENSORY
                modality: GOAL_RELATIVE
                signalTag: "ROBOT.GOAL.DX"
              - frameLabel: "human_risk"
                signal: HARM_ASSESSMENT
                signalTag: "ROBOT.HARM.RISK"
              - frameLabel: "battery"
                signal: EFFICIENCY
                signalTag: "ROBOT.BATTERY"
            outputMappings:
              - approvedSignalType: "MotorCommandSignal"
                frameLabels: ["vx", "vy"]
                validForMs: 250
                failSafeFrame:
                  safety_status: "STOP"
                  values: { vx: 0.0, vy: 0.0 }
            watchdog:
              staleFrameMs: 250
              outputDecayMs: 250
            audit:
              localAuditFile: "/tmp/jneopallium/nengo-audit.jsonl"
            perTagSafetyMode:
              ROBOT.MOTOR: AUTONOMOUS
            """;

    @Test
    void loadsHappyPath() throws IOException {
        NengoBridgeConfig cfg = NengoBridgeConfigLoader.load(HAPPY);
        assertNotNull(cfg);
        assertEquals(NengoBridgeConfig.TransportMode.UDS, cfg.transport().mode());
        assertEquals("/tmp/jneo-nengo-input.sock",  cfg.transport().channelInPath());
        assertEquals("/tmp/jneo-nengo-output.sock", cfg.transport().channelOutPath());
        assertEquals(3, cfg.inputMappings().size());
        assertEquals(1, cfg.outputMappings().size());
        assertTrue(cfg.simulatorOnly());
        assertEquals(BridgeSafetyMode.AUTONOMOUS,
                cfg.perTagSafetyMode().get("ROBOT.MOTOR"));
    }

    /** §11 S15: AUTONOMOUS must be refused outside simulatorOnly mode. */
    @Test
    void rejectsAutonomousWithoutSimulatorOnly() {
        String yaml = HAPPY.replace("simulatorOnly: true", "simulatorOnly: false");
        Exception ex = assertThrows(Exception.class,
                () -> NengoBridgeConfigLoader.load(yaml));
        assertTrue(rootCause(ex).getMessage().contains("AUTONOMOUS"));
    }

    @Test
    void unknownPropertyFailsLoading() {
        String yaml = """
                transport:
                  channelInPath:  "/tmp/a"
                  channelOutPath: "/tmp/b"
                  mode: FILE
                audit:
                  localAuditFile: "/tmp/c.jsonl"
                bogusKey: 42
                """;
        assertThrows(UnrecognizedPropertyException.class,
                () -> NengoBridgeConfigLoader.load(yaml));
    }

    @Test
    void rejectsDuplicateInputFrameLabels() {
        String yaml = """
                transport:
                  channelInPath:  "/tmp/a"
                  channelOutPath: "/tmp/b"
                  mode: FILE
                inputMappings:
                  - frameLabel: "same"
                    signal: MEASUREMENT
                    signalTag: "A"
                  - frameLabel: "same"
                    signal: MEASUREMENT
                    signalTag: "B"
                audit:
                  localAuditFile: "/tmp/c.jsonl"
                """;
        Exception ex = assertThrows(Exception.class,
                () -> NengoBridgeConfigLoader.load(yaml));
        assertTrue(rootCause(ex).getMessage().contains("duplicate input frameLabel"));
    }

    @Test
    void fileModeAllowsAutonomousWhenSimulatorOnly() throws IOException {
        String yaml = """
                transport:
                  channelInPath:  "/tmp/a"
                  channelOutPath: "/tmp/b"
                  mode: FILE
                simulatorOnly: true
                outputMappings:
                  - approvedSignalType: "MotorCommandSignal"
                    frameLabels: ["vx", "vy"]
                    validForMs: 250
                audit:
                  localAuditFile: "/tmp/c.jsonl"
                perTagSafetyMode:
                  ROBOT.MOTOR: AUTONOMOUS
                """;
        NengoBridgeConfig cfg = NengoBridgeConfigLoader.load(yaml);
        assertEquals(NengoBridgeConfig.TransportMode.FILE, cfg.transport().mode());
        assertTrue(cfg.simulatorOnly());
    }

    @Test
    void defaultsApplyWhenMissing() throws IOException {
        String yaml = """
                transport:
                  channelInPath:  "/tmp/a"
                  channelOutPath: "/tmp/b"
                audit:
                  localAuditFile: "/tmp/c.jsonl"
                """;
        NengoBridgeConfig cfg = NengoBridgeConfigLoader.load(yaml);
        assertEquals(NengoBridgeConfig.TransportMode.UDS, cfg.transport().mode());
        assertEquals(250L, cfg.watchdog().staleFrameMs());
        assertEquals(250L, cfg.watchdog().outputDecayMs());
        assertFalse(cfg.simulatorOnly());
        assertTrue(cfg.inputMappings().isEmpty());
        assertTrue(cfg.outputMappings().isEmpty());
    }
}
