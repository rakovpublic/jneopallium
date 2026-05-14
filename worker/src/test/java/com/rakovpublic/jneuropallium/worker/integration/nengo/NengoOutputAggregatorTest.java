/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.integration.nengo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rakovpublic.jneuropallium.ai.enums.HarmVerdict;
import com.rakovpublic.jneuropallium.ai.signals.fast.HarmVetoSignal;
import com.rakovpublic.jneuropallium.ai.signals.fast.MotorCommandSignal;
import com.rakovpublic.jneuropallium.ai.signals.fast.TransparencyLogSignal;
import com.rakovpublic.jneuropallium.worker.bridge.common.AbstractBridgeAuditOutput;
import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeAuditRecord;
import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeSafetyMode;
import com.rakovpublic.jneuropallium.worker.net.layers.IResult;
import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests for {@link NengoBridgeOutputAggregator} — S11, S12. */
final class NengoOutputAggregatorTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @TempDir Path tmp;

    private Path outboundJsonl;
    private Path auditJsonl;

    @BeforeEach
    void setUp() {
        outboundJsonl = tmp.resolve("nengo-out.jsonl");
        auditJsonl = tmp.resolve("nengo-audit.jsonl");
    }

    @AfterEach
    void cleanup() throws IOException {
        Files.deleteIfExists(outboundJsonl);
        Files.deleteIfExists(auditJsonl);
    }

    private NengoBridgeConfig autonomousConfig() {
        return new NengoBridgeConfig(
                new NengoBridgeConfig.TransportSection(
                        tmp.resolve("in.jsonl").toString(),
                        outboundJsonl.toString(),
                        NengoBridgeConfig.TransportMode.FILE,
                        250L, 5_000L, 65_536),
                true,
                List.of(),
                List.of(new NengoBridgeConfig.OutputMapping(
                        "MotorCommandSignal", List.of("vx", "vy"), 250L,
                        new NengoBridgeConfig.FailSafeFrame(
                                "STOP", Map.of("vx", 0.0, "vy", 0.0)))),
                new NengoBridgeConfig.WatchdogSection(250L, 250L),
                new NengoBridgeConfig.AuditSection(auditJsonl.toString()),
                Map.of(NengoBridgeOutputAggregator.MOTOR_TAG, BridgeSafetyMode.AUTONOMOUS));
    }

    private NengoBridgeConfig shadowConfig() {
        NengoBridgeConfig cfg = autonomousConfig();
        return new NengoBridgeConfig(
                cfg.transport(),
                cfg.simulatorOnly(),
                cfg.inputMappings(),
                cfg.outputMappings(),
                cfg.watchdog(),
                cfg.audit(),
                Map.of() /* default SHADOW */);
    }

    @Test
    void motorCommandWithExecuteTrueProducesFrame() throws IOException {
        NengoBridgeConfig cfg = autonomousConfig();
        try (NengoChannelService ch = NengoChannelService.outputFromConfig(cfg);
             AbstractBridgeAuditOutput audit = new NengoBridgeAuditOutput(auditJsonl)) {

            JneopalliumToNengoMapper mapper = new JneopalliumToNengoMapper(cfg);
            NengoBridgeOutputAggregator agg =
                    new NengoBridgeOutputAggregator(cfg, ch, mapper, audit);

            MotorCommandSignal mc = new MotorCommandSignal(0, new double[]{0.42, -0.15});
            mc.setExecute(true);
            agg.save(List.of(result(mc, 1L)), 10_000L, 1L, null);

            String[] outLines = Files.readString(outboundJsonl, StandardCharsets.UTF_8).split("\n");
            assertEquals(1, countNonEmpty(outLines));
            NengoOutputFrame frame = JSON.readValue(outLines[0], NengoOutputFrame.class);
            assertEquals("OK", frame.safetyStatus());
            assertEquals(0.42, frame.values().get("vx"), 1e-9);
            assertEquals(-0.15, frame.values().get("vy"), 1e-9);
            assertEquals(10_000L + 250L, frame.validUntilMs());
            assertEquals(1, countAudit(BridgeAuditRecord.Verdict.APPLIED, "MOTOR_FRAME"));
        }
    }

    @Test
    void motorCommandWithoutExecuteIsRejected() throws IOException {
        NengoBridgeConfig cfg = autonomousConfig();
        try (NengoChannelService ch = NengoChannelService.outputFromConfig(cfg);
             AbstractBridgeAuditOutput audit = new NengoBridgeAuditOutput(auditJsonl)) {

            JneopalliumToNengoMapper mapper = new JneopalliumToNengoMapper(cfg);
            NengoBridgeOutputAggregator agg =
                    new NengoBridgeOutputAggregator(cfg, ch, mapper, audit);

            MotorCommandSignal mc = new MotorCommandSignal(0, new double[]{0.5, 0.5});
            // execute=false by default — must be refused
            agg.save(List.of(result(mc, 1L)), 10_000L, 1L, null);

            assertFalse(Files.exists(outboundJsonl) && Files.size(outboundJsonl) > 0);
            assertEquals(1, countAudit(BridgeAuditRecord.Verdict.REJECTED, "EXECUTE_FALSE"));
        }
    }

    @Test
    void shadowModeRejectsMotor() throws IOException {
        NengoBridgeConfig cfg = shadowConfig();
        try (NengoChannelService ch = NengoChannelService.outputFromConfig(cfg);
             AbstractBridgeAuditOutput audit = new NengoBridgeAuditOutput(auditJsonl)) {

            JneopalliumToNengoMapper mapper = new JneopalliumToNengoMapper(cfg);
            NengoBridgeOutputAggregator agg =
                    new NengoBridgeOutputAggregator(cfg, ch, mapper, audit);

            MotorCommandSignal mc = new MotorCommandSignal(0, new double[]{0.42, -0.15});
            mc.setExecute(true);
            agg.save(List.of(result(mc, 1L)), 10_000L, 1L, null);

            assertFalse(Files.exists(outboundJsonl) && Files.size(outboundJsonl) > 0);
            assertEquals(1, countAudit(BridgeAuditRecord.Verdict.REJECTED, "SHADOW_MODE"));
        }
    }

    /** S12 — harm veto produces STOP frame audited as INTERLOCK_TRIP. */
    @Test
    void s12_harmVetoProducesStopFrame() throws IOException {
        NengoBridgeConfig cfg = autonomousConfig();
        try (NengoChannelService ch = NengoChannelService.outputFromConfig(cfg);
             AbstractBridgeAuditOutput audit = new NengoBridgeAuditOutput(auditJsonl)) {

            JneopalliumToNengoMapper mapper = new JneopalliumToNengoMapper(cfg);
            NengoBridgeOutputAggregator agg =
                    new NengoBridgeOutputAggregator(cfg, ch, mapper, audit);

            HarmVetoSignal hv = new HarmVetoSignal("plan-1", "OBSTACLE_TOO_CLOSE",
                    HarmVerdict.HARMFUL, null);
            TransparencyLogSignal tx = new TransparencyLogSignal(
                    "tx-abc", "harm", new String[]{"n42"}, HarmVerdict.HARMFUL, 1L);

            agg.save(List.of(result(tx, 42L), result(hv, 1L)),
                    20_000L, 2L, null);

            String[] outLines = Files.readString(outboundJsonl, StandardCharsets.UTF_8).split("\n");
            NengoOutputFrame frame = JSON.readValue(outLines[0], NengoOutputFrame.class);
            assertEquals("STOP", frame.safetyStatus());
            assertEquals(0.0, frame.values().get("vx"), 1e-9);
            assertEquals(0.0, frame.values().get("vy"), 1e-9);
            assertEquals("tx-abc", frame.transparencyLogId());
            assertEquals(1, countAudit(BridgeAuditRecord.Verdict.INTERLOCK_TRIP, "HARM_VETO"));
        }
    }

    /** S11 — watchdog STOP frame emitted after outputDecayMs of silence. */
    @Test
    void s11_watchdogDecay() throws IOException {
        NengoBridgeConfig cfg = autonomousConfig();
        try (NengoChannelService ch = NengoChannelService.outputFromConfig(cfg);
             AbstractBridgeAuditOutput audit = new NengoBridgeAuditOutput(auditJsonl)) {

            JneopalliumToNengoMapper mapper = new JneopalliumToNengoMapper(cfg);
            NengoBridgeOutputAggregator agg =
                    new NengoBridgeOutputAggregator(cfg, ch, mapper, audit);

            // First, write one approved frame so lastAppliedTs is set.
            MotorCommandSignal mc = new MotorCommandSignal(0, new double[]{0.1, 0.2});
            mc.setExecute(true);
            agg.save(List.of(result(mc, 1L)), 10_000L, 1L, null);
            assertEquals(10_000L, agg.lastAppliedTs());

            // Next tick: no results, but enough wall-clock has passed.
            agg.save(List.of(), 10_500L, 2L, null);

            String content = Files.readString(outboundJsonl, StandardCharsets.UTF_8);
            assertNotNull(content);
            // Two frames: the first MOTOR_FRAME, then the WATCHDOG STOP.
            String[] outLines = content.split("\n");
            assertEquals(2, countNonEmpty(outLines));
            NengoOutputFrame stop = JSON.readValue(outLines[1], NengoOutputFrame.class);
            assertEquals("STOP", stop.safetyStatus());
            assertTrue("WATCHDOG_DECAY".equals(stop.transparencyLogId()));
            assertEquals(1, countAudit(BridgeAuditRecord.Verdict.INTERLOCK_TRIP, "WATCHDOG_DECAY"));
        }
    }

    /* ===== helpers ========================================================= */

    private static IResult<IResultSignal> result(IResultSignal<?> sig, Long neuronId) {
        @SuppressWarnings({"rawtypes", "unchecked"})
        IResult<IResultSignal> r = new IResult<>() {
            @Override public IResultSignal getResult() { return sig; }
            @Override public Long getNeuronId() { return neuronId; }
        };
        return r;
    }

    private static int countNonEmpty(String[] lines) {
        int n = 0;
        for (String l : lines) if (l != null && !l.isEmpty()) n++;
        return n;
    }

    private int countAudit(BridgeAuditRecord.Verdict verdict,
                           String reasonContains) throws IOException {
        if (!Files.exists(auditJsonl)) return 0;
        int n = 0;
        for (String line : Files.readAllLines(auditJsonl, StandardCharsets.UTF_8)) {
            BridgeAuditRecord r = JSON.readValue(line, BridgeAuditRecord.class);
            if (r.verdict() == verdict
                    && r.reason() != null && r.reason().contains(reasonContains)) n++;
        }
        return n;
    }
}
