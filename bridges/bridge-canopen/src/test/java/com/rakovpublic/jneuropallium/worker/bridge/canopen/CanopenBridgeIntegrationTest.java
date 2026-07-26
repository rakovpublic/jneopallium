/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.canopen;

import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeSafetyMode;
import com.rakovpublic.jneuropallium.worker.net.layers.IResult;
import com.rakovpublic.jneuropallium.worker.net.layers.impl.SimpleResultWrapper;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.AlarmPriority;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.BatchPhase;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.Quality;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.embodiment.ProprioceptiveSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.ActuatorCommandSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.AlarmSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.BatchStateSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.EfficiencySignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MeasurementSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.SetpointSignal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for the CANopen bridge (13-CANOPEN.md §10). Covers the
 * universal 00-FRAMEWORK §5 scenarios that apply (S1, S2, S3, S4, S5, S6)
 * plus the bridge-specific scenarios S7–S12. Runs against
 * {@link InMemoryCanopenClientService} — no SocketCAN / USB-CAN dongle
 * required.
 */
class CanopenBridgeIntegrationTest {

    private static final int NODE_DRIVE = 0x01;
    private static final int NODE_BMS   = 0x02;

    private static final int OD_POSITION_ACTUAL = 0x6064;
    private static final int OD_STATUSWORD      = 0x6041;
    private static final int OD_PROFILE_VEL     = 0x6081;
    private static final int OD_BMS_SOC         = 0x2001;

    @TempDir Path tempDir;

    private CanopenAuditOutput audit;
    private InMemoryCanopenClientService svc;

    @BeforeEach
    void setUp() {
        audit = new CanopenAuditOutput(tempDir.resolve("canopen-audit.jsonl"));
    }

    @AfterEach
    void tearDown() {
        if (svc != null) svc.close();
        if (audit != null) audit.close();
    }

    /* ===== §5 universal framework scenarios ============================= */

    /**
     * S1 — pure read: a TPDO1 frame from a CiA-402 drive lands as a
     * {@link ProprioceptiveSignal}.
     */
    @Test
    void s1_pureReadEmitsProprioceptiveSignal() {
        bring(baseConfig(
                List.of(readBinding("DRIVE-1-POS", NODE_DRIVE,
                        OD_POSITION_ACTUAL, CanopenBridgeConfig.PdoSource.TPDO1,
                        CanopenBridgeConfig.OdType.INT32,
                        CanopenBridgeConfig.ReadSignalKind.PROPRIOCEPTIVE,
                        "DRIVE.1.POS_ACTUAL", 1.0, 0.0, 1)),
                List.of(),
                List.of(),
                Map.of(),
                Map.of()));
        // 12345 ticks of position, INT32 little-endian over TPDO1 (cob 0x181).
        svc.onCanFrame(tpdoFrame(NODE_DRIVE, 1,
                int32ToBytes(12345)));

        List<IInputSignal> sigs = svc.drain("DRIVE-1-POS");
        assertEquals(1, sigs.size());
        ProprioceptiveSignal sig = (ProprioceptiveSignal) sigs.get(0);
        assertEquals(12345.0, sig.getJointStates()[0], 1e-9);
        assertEquals(NODE_DRIVE, sig.getEffectorId());
    }

    /**
     * S2 — quality propagates: when a node has gone past the heartbeat
     * timeout, subsequent {@link MeasurementSignal} reads from it carry
     * {@code Quality.UNCERTAIN}.
     */
    @Test
    void s2_qualityPropagatesAfterHeartbeatLoss() {
        bring(baseConfig(
                List.of(readBinding("BMS-SOC", NODE_BMS,
                        OD_BMS_SOC, CanopenBridgeConfig.PdoSource.TPDO1,
                        CanopenBridgeConfig.OdType.UINT16,
                        CanopenBridgeConfig.ReadSignalKind.MEASUREMENT,
                        "BMS.PACK_A.SOC", 1.0, 0.0, 1)),
                List.of(), List.of(), Map.of(), Map.of()));

        // Establish liveness for node 2.
        svc.onCanFrame(heartbeatFrame(NODE_BMS, 0x05 /* operational */));
        long now = System.currentTimeMillis();
        svc.checkHeartbeats(now);
        assertFalse(svc.isOffline(NODE_BMS));

        // 5 s later — past the 2 s timeout.
        svc.checkHeartbeats(now + 5_000L);
        assertTrue(svc.isOffline(NODE_BMS));

        svc.onCanFrame(tpdoFrame(NODE_BMS, 1, uint16ToBytes(42)));
        MeasurementSignal m = (MeasurementSignal) svc.drain("BMS-SOC").get(0);
        assertEquals(Quality.UNCERTAIN, m.getQuality(),
                "reads from an offline node must be marked UNCERTAIN (00-FRAMEWORK §0.5, §5 S2)");
    }

    /** S3 — SHADOW-mode rejects writes; nothing reaches the wire. */
    @Test
    void s3_shadowModeRejectsWrite() throws IOException {
        bring(baseConfig(
                List.of(),
                List.of(),
                List.of(writeBinding("DRIVE-1-PROFILE-VEL", NODE_DRIVE,
                        OD_PROFILE_VEL, CanopenBridgeConfig.OdType.UINT32,
                        CanopenBridgeConfig.WriteVia.SDO,
                        "DRIVE.1.PROFILE_VEL", 0.0, 5000.0, null)),
                Map.of("DRIVE-1-PROFILE-VEL", BridgeSafetyMode.SHADOW),
                Map.of(NODE_DRIVE, List.of(OD_PROFILE_VEL))));

        var agg = new CanopenAdvisoryOutputAggregator(svc, audit);
        agg.save(List.of(result(setpoint("DRIVE.1.PROFILE_VEL", 1500.0))),
                System.currentTimeMillis(), 1L, null);

        assertEquals(0, svc.sendCount(), "SHADOW must not emit a frame");
        String log = Files.readString(tempDir.resolve("canopen-audit.jsonl"));
        assertTrue(log.contains("SHADOW_MODE"), "expected SHADOW_MODE audit, got: " + log);
    }

    /** S4 — reconnect drops cache and emits {@code BRIDGE_RECONNECTED}. */
    @Test
    void s4_reconnectClearsCacheAndEmitsAdvisoryEvent() {
        bring(baseConfig(
                List.of(readBinding("DRIVE-1-POS", NODE_DRIVE,
                        OD_POSITION_ACTUAL, CanopenBridgeConfig.PdoSource.TPDO1,
                        CanopenBridgeConfig.OdType.INT32,
                        CanopenBridgeConfig.ReadSignalKind.PROPRIOCEPTIVE,
                        "DRIVE.1.POS_ACTUAL", 1.0, 0.0, 1)),
                List.of(), List.of(), Map.of(), Map.of()));

        svc.onCanFrame(tpdoFrame(NODE_DRIVE, 1, int32ToBytes(7)));
        svc.onReconnected();
        assertTrue(svc.drain("DRIVE-1-POS").isEmpty(),
                "cache must be dropped on reconnect (00-FRAMEWORK §2.3)");
        List<IInputSignal> events = svc.drainEvents();
        assertTrue(events.stream().anyMatch(e ->
                        e instanceof AlarmSignal a && CanopenClientService.BRIDGE_RECONNECTED.equals(a.getConditionCode())),
                "expected BRIDGE_RECONNECTED advisory");
    }

    /** S5 — audit failure isolation: an unwritable file degrades the audit channel but doesn't block traffic. */
    @Test
    void s5_auditFailureIsolation() throws IOException {
        Path blocker = tempDir.resolve("blocker");
        Files.createFile(blocker);
        Path bogus = blocker.resolve("audit.jsonl");
        audit.close();
        audit = new CanopenAuditOutput(bogus);
        svc = new InMemoryCanopenClientService(baseConfig(
                List.of(readBinding("DRIVE-1-POS", NODE_DRIVE,
                        OD_POSITION_ACTUAL, CanopenBridgeConfig.PdoSource.TPDO1,
                        CanopenBridgeConfig.OdType.INT32,
                        CanopenBridgeConfig.ReadSignalKind.PROPRIOCEPTIVE,
                        "DRIVE.1.POS_ACTUAL", 1.0, 0.0, 1)),
                List.of(), List.of(), Map.of(), Map.of()), audit);
        svc.start();

        svc.onCanFrame(tpdoFrame(NODE_DRIVE, 1, int32ToBytes(1)));
        assertEquals(1, svc.drain("DRIVE-1-POS").size(),
                "bridge must keep emitting signals even with a degraded audit channel");
        assertTrue(audit.isDegraded(), "audit channel should be degraded");
    }

    /** S6 — unknown tag rejected. */
    @Test
    void s6_unknownTagRejected() throws IOException {
        bring(baseConfig(
                List.of(), List.of(),
                List.of(writeBinding("DRIVE-1-PROFILE-VEL", NODE_DRIVE,
                        OD_PROFILE_VEL, CanopenBridgeConfig.OdType.UINT32,
                        CanopenBridgeConfig.WriteVia.SDO,
                        "DRIVE.1.PROFILE_VEL", null, null, null)),
                Map.of("DRIVE-1-PROFILE-VEL", BridgeSafetyMode.ADVISORY),
                Map.of(NODE_DRIVE, List.of(OD_PROFILE_VEL))));

        var agg = new CanopenAdvisoryOutputAggregator(svc, audit);
        agg.save(List.of(result(setpoint("UNKNOWN.TAG", 1.0))),
                System.currentTimeMillis(), 1L, null);

        assertEquals(0, svc.sendCount());
        String log = Files.readString(tempDir.resolve("canopen-audit.jsonl"));
        assertTrue(log.contains("UNKNOWN_TAG"));
    }

    /* ===== §10 bridge-specific scenarios ================================= */

    /** S7 — vcan0-style PDO read produces the configured signal. */
    @Test
    void s7_vcanRead() {
        bring(baseConfig(
                List.of(readBinding("DRIVE-1-POS", NODE_DRIVE,
                        OD_POSITION_ACTUAL, CanopenBridgeConfig.PdoSource.TPDO1,
                        CanopenBridgeConfig.OdType.INT32,
                        CanopenBridgeConfig.ReadSignalKind.PROPRIOCEPTIVE,
                        "DRIVE.1.POS_ACTUAL", 1.0, 0.0, 1)),
                List.of(), List.of(), Map.of(), Map.of()));

        for (int v : new int[]{1, 2, 3, 4, 5}) {
            svc.onCanFrame(tpdoFrame(NODE_DRIVE, 1, int32ToBytes(v * 100)));
        }
        List<IInputSignal> sigs = svc.drain("DRIVE-1-POS");
        assertEquals(5, sigs.size());
        for (int i = 0; i < 5; i++) {
            assertEquals((double) ((i + 1) * 100),
                    ((ProprioceptiveSignal) sigs.get(i)).getJointStates()[0], 1e-9);
        }
    }

    /** S8 — EDS parsing wired: a parsed EDS exposes the OD entry. */
    @Test
    void s8_edsParse() throws Exception {
        String eds = """
                [6064]
                ParameterName=Position actual value
                DataType=0x0004
                AccessType=ro
                """;
        Map<Integer, ObjectDictionaryEntry> od = EdsParser.parse(eds);
        ObjectDictionaryEntry e = od.get(0x6064 << 8);
        assertNotNull(e);
        assertEquals(CanopenBridgeConfig.OdType.INT32, e.odType());
    }

    /** S9 — heartbeat loss → NODE_OFFLINE alarm + sticky offline flag. */
    @Test
    void s9_heartbeatLossEmitsAlarm() {
        bring(baseConfig(
                List.of(),
                List.of(eventBinding("DRIVE-1-OFFLINE", NODE_DRIVE,
                        CanopenBridgeConfig.EventSource.HEARTBEAT_LOSS, "DRIVE.OFFLINE")),
                List.of(), Map.of(), Map.of()));

        svc.onCanFrame(heartbeatFrame(NODE_DRIVE, 0x05));
        long now = System.currentTimeMillis();
        svc.checkHeartbeats(now);
        assertTrue(svc.drainEvents().stream().noneMatch(
                e -> e instanceof AlarmSignal a && "NODE_OFFLINE".equals(a.getConditionCode())));

        svc.checkHeartbeats(now + 5_000L);
        List<IInputSignal> events = svc.drainEvents();
        assertTrue(events.stream().anyMatch(e ->
                        e instanceof AlarmSignal a
                                && "NODE_OFFLINE".equals(a.getConditionCode())
                                && a.getPriority() == AlarmPriority.HIGH),
                "expected NODE_OFFLINE alarm after heartbeat timeout");
        // The HEARTBEAT_LOSS event-binding's queue also gets the alarm.
        List<IInputSignal> q = svc.drain("DRIVE-1-OFFLINE");
        assertTrue(q.stream().anyMatch(e -> e instanceof AlarmSignal a
                && a.getTag() != null && a.getTag().startsWith("DRIVE.OFFLINE")));
    }

    /** S10 — EMCY frame decoded into AlarmSignal with the standard description. */
    @Test
    void s10_emcyDecode() {
        bring(baseConfig(
                List.of(),
                List.of(eventBinding("DRIVE-1-FAULT", NODE_DRIVE,
                        CanopenBridgeConfig.EventSource.EMCY, "DRIVE.FAULT")),
                List.of(), Map.of(), Map.of()));

        // EMCY frame: code=0x2310 (CONTINUOUS_OVER_CURRENT), errorReg=0x02, vendor=0.
        byte[] payload = new byte[8];
        ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN)
                .putShort((short) 0x2310).put((byte) 0x02);
        svc.onCanFrame(new CanFrame(0x080 + NODE_DRIVE, payload));

        List<IInputSignal> q = svc.drain("DRIVE-1-FAULT");
        assertEquals(1, q.size());
        AlarmSignal a = (AlarmSignal) q.get(0);
        assertEquals(AlarmPriority.URGENT, a.getPriority(),
                "0x23xx (current) maps to URGENT in CanopenSignalMapper");
        assertTrue(a.getConditionCode().contains("CONTINUOUS_OVER_CURRENT"),
                "expected the standard description, got: " + a.getConditionCode());
        // The EMCY also fans out to the global event channel.
        assertTrue(svc.drainEvents().stream().anyMatch(
                e -> e instanceof AlarmSignal x && x.getConditionCode().contains("CONTINUOUS_OVER_CURRENT")));
    }

    /**
     * S11 — disallowed index rejected at config load. Constructed
     * directly because 13-CANOPEN.md §6 rejects them at load, not
     * runtime.
     */
    @Test
    void s11_disallowedIndexRejectedAtLoad() {
        IllegalArgumentException ex = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class, () -> baseConfig(
                        List.of(), List.of(),
                        List.of(writeBinding("BAD-CTRLWORD", NODE_DRIVE,
                                0x6040,  // controlword
                                CanopenBridgeConfig.OdType.UINT16,
                                CanopenBridgeConfig.WriteVia.SDO,
                                "BAD.CTRLWORD", null, null, null)),
                        Map.of(),
                        Map.of(NODE_DRIVE, List.of(0x6040))));
        assertTrue(ex.getMessage().contains("forbidden"),
                "expected forbidden-OD-index message, got: " + ex.getMessage());
    }

    /** S12 — SDO write to an allowed index emits an SDO download frame. */
    @Test
    void s12_sdoWriteToAllowedIndex() throws IOException {
        bring(baseConfig(
                List.of(), List.of(),
                List.of(writeBinding("DRIVE-1-PROFILE-VEL", NODE_DRIVE,
                        OD_PROFILE_VEL, CanopenBridgeConfig.OdType.UINT32,
                        CanopenBridgeConfig.WriteVia.SDO,
                        "DRIVE.1.PROFILE_VEL", 0.0, 5000.0, null)),
                Map.of("DRIVE-1-PROFILE-VEL", BridgeSafetyMode.ADVISORY),
                Map.of(NODE_DRIVE, List.of(OD_PROFILE_VEL))));

        var agg = new CanopenAdvisoryOutputAggregator(svc, audit);
        agg.save(List.of(result(actuator("DRIVE.1.PROFILE_VEL", 1500.0, true))),
                System.currentTimeMillis(), 1L, null);

        assertEquals(1, svc.sendCount());
        CanFrame f = svc.sentFrames().get(0);
        assertEquals(0x600 + NODE_DRIVE, f.cobId(), "SDO request COB-ID = 0x600 + nodeId");
        byte[] data = f.data();
        // Expedited download command-specifier with size indicated, n=0 (4 data bytes).
        assertEquals((byte) 0x23, data[0]);
        assertEquals((byte) (OD_PROFILE_VEL & 0xff), data[1]);
        assertEquals((byte) ((OD_PROFILE_VEL >>> 8) & 0xff), data[2]);
        assertEquals((byte) 0x00, data[3]);
        // 1500 LE u32 = 0xDC, 0x05, 0x00, 0x00.
        assertEquals((byte) 0xDC, data[4]);
        assertEquals((byte) 0x05, data[5]);

        String log = Files.readString(tempDir.resolve("canopen-audit.jsonl"));
        assertTrue(log.contains("APPLIED"));
    }

    /* ===== additional defensive scenarios ================================ */

    /** Statusword PDO emits a fault alarm when bit 3 is set, plus the carrier MeasurementSignal. */
    @Test
    void statuswordPdoFiresMeasurementAndFaultAlarm() {
        bring(baseConfig(
                List.of(readBinding("DRIVE-1-STATE", NODE_DRIVE,
                        OD_STATUSWORD, CanopenBridgeConfig.PdoSource.TPDO2,
                        CanopenBridgeConfig.OdType.UINT16,
                        CanopenBridgeConfig.ReadSignalKind.BATCH_STATE,
                        "DRIVE.1.STATE", 1.0, 0.0, 1)),
                List.of(), List.of(), Map.of(), Map.of()));

        // Bit 3 = FAULT, bits 0..2 = "ready to switch on disabled" combination.
        int statusword = 0x000F;
        svc.onCanFrame(tpdoFrame(NODE_DRIVE, 2, uint16ToBytes(statusword)));

        List<IInputSignal> q = svc.drain("DRIVE-1-STATE");
        assertEquals(1, q.size());
        MeasurementSignal carrier = (MeasurementSignal) q.get(0);
        assertEquals(statusword, (int) carrier.getMeasurement());

        BatchStateSignal state = svc.lastDriveState("DRIVE-1-STATE");
        assertNotNull(state);
        assertEquals(BatchPhase.ABORTED, state.getPhase());

        AlarmSignal fault = (AlarmSignal) svc.drainEvents().stream()
                .filter(e -> e instanceof AlarmSignal)
                .findFirst().orElseThrow();
        assertEquals(AlarmPriority.HIGH, fault.getPriority());
        assertTrue(fault.getConditionCode().startsWith("DRIVE_FAULT"));
    }

    /** BMS scalar PDO → EfficiencySignal with the SoC normalised to 0..1. */
    @Test
    void bmsPdoEmitsEfficiencySignal() {
        bring(baseConfig(
                List.of(readBinding("BMS-SOC", NODE_BMS,
                        OD_BMS_SOC, CanopenBridgeConfig.PdoSource.TPDO1,
                        CanopenBridgeConfig.OdType.UINT16,
                        CanopenBridgeConfig.ReadSignalKind.EFFICIENCY,
                        "BMS.PACK_A.SOC", 1.0, 0.0, 1)),
                List.of(), List.of(), Map.of(), Map.of()));

        svc.onCanFrame(tpdoFrame(NODE_BMS, 1, uint16ToBytes(85)));
        EfficiencySignal eff = (EfficiencySignal) svc.drain("BMS-SOC").get(0);
        assertEquals(0.85, eff.getEfficiency(), 1e-9);
    }

    /** Runtime backstop: a forbidden index reaching {@code send()} is rejected with a NOT_ON_ALLOW_LIST audit. */
    @Test
    void runtimeBackstopRejectsDisallowedSendDirectly() throws IOException {
        // Build a config that has an allow-listed write binding, then attempt
        // a direct send to a binding id that doesn't exist (proxying the
        // "loader bypassed" path the runtime backstop is the last line of
        // defence against).
        bring(baseConfig(
                List.of(), List.of(),
                List.of(writeBinding("DRIVE-1-PROFILE-VEL", NODE_DRIVE,
                        OD_PROFILE_VEL, CanopenBridgeConfig.OdType.UINT32,
                        CanopenBridgeConfig.WriteVia.SDO,
                        "DRIVE.1.PROFILE_VEL", null, null, null)),
                Map.of("DRIVE-1-PROFILE-VEL", BridgeSafetyMode.ADVISORY),
                Map.of(NODE_DRIVE, List.of(OD_PROFILE_VEL))));

        boolean ok = svc.send("UNKNOWN-BINDING", 1.0,
                System.currentTimeMillis(), 1L);
        assertFalse(ok);
        String log = Files.readString(tempDir.resolve("canopen-audit.jsonl"));
        assertTrue(log.contains("UNKNOWN_BINDING"),
                "expected UNKNOWN_BINDING audit, got: " + log);
    }

    /** Decimation: every Nth frame is delivered, the rest dropped. */
    @Test
    void decimationDropsAllButEveryNth() {
        bring(baseConfig(
                List.of(readBinding("DRIVE-1-POS", NODE_DRIVE,
                        OD_POSITION_ACTUAL, CanopenBridgeConfig.PdoSource.TPDO1,
                        CanopenBridgeConfig.OdType.INT32,
                        CanopenBridgeConfig.ReadSignalKind.PROPRIOCEPTIVE,
                        "DRIVE.1.POS_ACTUAL", 1.0, 0.0, 3)),
                List.of(), List.of(), Map.of(), Map.of()));

        for (int i = 0; i < 9; i++) {
            svc.onCanFrame(tpdoFrame(NODE_DRIVE, 1, int32ToBytes(i)));
        }
        // decimateBy=3 with the counter incremented before the modulo means
        // i=2,5,8 (the 3rd, 6th, 9th frames) pass.
        assertEquals(3, svc.drain("DRIVE-1-POS").size());
    }

    /** The audit shape conforms to 00-FRAMEWORK §4. */
    @Test
    void auditShapeMatchesFramework() throws IOException {
        bring(baseConfig(
                List.of(), List.of(),
                List.of(writeBinding("DRIVE-1-PROFILE-VEL", NODE_DRIVE,
                        OD_PROFILE_VEL, CanopenBridgeConfig.OdType.UINT32,
                        CanopenBridgeConfig.WriteVia.SDO,
                        "DRIVE.1.PROFILE_VEL", 0.0, 5000.0, null)),
                Map.of("DRIVE-1-PROFILE-VEL", BridgeSafetyMode.ADVISORY),
                Map.of(NODE_DRIVE, List.of(OD_PROFILE_VEL))));

        var agg = new CanopenAdvisoryOutputAggregator(svc, audit);
        agg.save(List.of(result(actuator("DRIVE.1.PROFILE_VEL", 2500.0, true))),
                1700_000_000_000L, 42L, null);

        audit.close();
        audit = null;
        String log = Files.readString(tempDir.resolve("canopen-audit.jsonl"));
        assertTrue(log.contains("\"bridge\":\"canopen\""), "missing bridge key: " + log);
        assertTrue(log.contains("\"verdict\":\"APPLIED\""), "missing APPLIED verdict: " + log);
        assertTrue(log.contains("\"tag\":\"DRIVE.1.PROFILE_VEL\""), "missing tag: " + log);
    }

    /**
     * Clamping and rate-limiting are applied per the universal §2.2
     * algorithm; the audit's {@code reason} captures the modification.
     */
    @Test
    void clampApplied() throws IOException {
        bring(baseConfig(
                List.of(), List.of(),
                List.of(writeBinding("DRIVE-1-PROFILE-VEL", NODE_DRIVE,
                        OD_PROFILE_VEL, CanopenBridgeConfig.OdType.UINT32,
                        CanopenBridgeConfig.WriteVia.SDO,
                        "DRIVE.1.PROFILE_VEL", 0.0, 5000.0, null)),
                Map.of("DRIVE-1-PROFILE-VEL", BridgeSafetyMode.ADVISORY),
                Map.of(NODE_DRIVE, List.of(OD_PROFILE_VEL))));

        var agg = new CanopenAdvisoryOutputAggregator(svc, audit);
        agg.save(List.of(result(actuator("DRIVE.1.PROFILE_VEL", 99_999.0, true))),
                System.currentTimeMillis(), 1L, null);

        assertEquals(1, svc.sendCount());
        CanFrame f = svc.sentFrames().get(0);
        // 5000 LE = 0x88, 0x13, 0x00, 0x00.
        assertEquals((byte) 0x88, f.data()[4]);
        assertEquals((byte) 0x13, f.data()[5]);
        String log = Files.readString(tempDir.resolve("canopen-audit.jsonl"));
        assertTrue(log.contains("CLAMPED_HIGH"), "expected CLAMPED_HIGH, got: " + log);
    }

    /* ===== helpers ====================================================== */

    private void bring(CanopenBridgeConfig cfg) {
        svc = new InMemoryCanopenClientService(cfg, audit);
        svc.start();
    }

    private static CanopenBridgeConfig baseConfig(
            List<CanopenBridgeConfig.ReadBindingConfig> reads,
            List<CanopenBridgeConfig.EventBindingConfig> events,
            List<CanopenBridgeConfig.WriteBindingConfig> writes,
            Map<String, BridgeSafetyMode> safetyModes,
            Map<Integer, List<Integer>> allowList) {
        return new CanopenBridgeConfig(
                new CanopenBridgeConfig.CanBusConfig(
                        CanopenBridgeConfig.BusType.SOCKETCAN, "vcan0", 500_000, 0.875),
                List.of(
                        new CanopenBridgeConfig.NodeConfig(NODE_DRIVE, "CiA-402", null),
                        new CanopenBridgeConfig.NodeConfig(NODE_BMS, "CiA-418", null)),
                reads, events, writes, allowList,
                new CanopenBridgeConfig.AuditConfig(
                        Path.of(System.getProperty("java.io.tmpdir"), "canopen-audit.jsonl").toString()),
                safetyModes, null);
    }

    private static CanopenBridgeConfig.ReadBindingConfig readBinding(
            String id, int nodeId, int odIndex,
            CanopenBridgeConfig.PdoSource src,
            CanopenBridgeConfig.OdType type,
            CanopenBridgeConfig.ReadSignalKind kind,
            String tag, double scale, double offset, int decimateBy) {
        return new CanopenBridgeConfig.ReadBindingConfig(
                id, nodeId, odIndex, 0, src, type, kind, tag, scale, offset, decimateBy);
    }

    private static CanopenBridgeConfig.EventBindingConfig eventBinding(
            String id, int nodeId,
            CanopenBridgeConfig.EventSource source, String prefix) {
        return new CanopenBridgeConfig.EventBindingConfig(
                id, nodeId, source, CanopenBridgeConfig.ReadSignalKind.ALARM, prefix);
    }

    private static CanopenBridgeConfig.WriteBindingConfig writeBinding(
            String id, int nodeId, int odIndex,
            CanopenBridgeConfig.OdType type,
            CanopenBridgeConfig.WriteVia via, String tag,
            Double minClamp, Double maxClamp, Double rampRate) {
        return new CanopenBridgeConfig.WriteBindingConfig(
                id, nodeId, odIndex, 0, type, via, tag,
                minClamp, maxClamp, rampRate, null);
    }

    private static CanFrame tpdoFrame(int nodeId, int pdoIndex /*1..4*/, byte[] payload) {
        int cob = switch (pdoIndex) {
            case 1 -> 0x180 + nodeId;
            case 2 -> 0x280 + nodeId;
            case 3 -> 0x380 + nodeId;
            case 4 -> 0x480 + nodeId;
            default -> throw new IllegalArgumentException("pdoIndex 1..4");
        };
        return new CanFrame(cob, payload);
    }

    private static CanFrame heartbeatFrame(int nodeId, int nmtState) {
        return new CanFrame(0x700 + nodeId, new byte[]{(byte) (nmtState & 0xff)});
    }

    private static byte[] int32ToBytes(int v) {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(v).array();
    }

    private static byte[] uint16ToBytes(int v) {
        return ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN)
                .putShort((short) (v & 0xffff)).array();
    }

    private static SetpointSignal setpoint(String tag, double v) {
        return new SetpointSignal(tag, v, 0.0, "test");
    }

    private static ActuatorCommandSignal actuator(String tag, double v, boolean execute) {
        return new ActuatorCommandSignal(tag, v, 0.0, execute);
    }

    private static <K extends com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal>
            IResult<K> result(K s) {
        return new SimpleResultWrapper<>(s, 1L);
    }
}
