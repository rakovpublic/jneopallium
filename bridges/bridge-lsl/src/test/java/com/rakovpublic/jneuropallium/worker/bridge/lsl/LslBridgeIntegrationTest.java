/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.lsl;

import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeSafetyMode;
import com.rakovpublic.jneuropallium.worker.net.layers.IResult;
import com.rakovpublic.jneuropallium.worker.net.layers.impl.SimpleResultWrapper;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci.IntentKind;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci.PolarityPattern;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci.SeizureMarker;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.OverrideKind;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.affect.AppraisalSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.affect.InteroceptiveSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.bci.CalibrationSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.bci.IntentSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.bci.LFPSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.bci.SeizureRiskSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.bci.StimulationCommandSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.AlarmSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.InterlockSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.OperatorOverrideSignal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for the LSL bridge (05-LSL.md §9). Covers the universal
 * 00-FRAMEWORK §5 scenarios that apply (S1, S3, S4, S5, S6) plus the
 * bridge-specific scenarios S7–S11. Runs entirely on
 * {@link InMemoryLslTransport} — no liblsl native binary required.
 */
class LslBridgeIntegrationTest {

    @TempDir Path tempDir;

    private LslAuditOutput audit;
    private InMemoryLslTransport transport;
    private LslClientService svc;

    @BeforeEach
    void setUp() {
        audit = new LslAuditOutput(tempDir.resolve("lsl-audit.jsonl"));
        transport = new InMemoryLslTransport();
    }

    @AfterEach
    void tearDown() {
        if (svc != null) svc.close();
        if (audit != null) audit.close();
    }

    /* ===== §5 universal framework scenarios + §9 bridge specifics ====== */

    /**
     * S1 + S7 — discover & bind: an EEG stream resolves and emits per-channel
     * {@link LFPSignal}s after one chunk.
     */
    @Test
    void s7_discoverAndBind_emitsLfpSignals() {
        transport.publishStream("OpenViBE-EEG-256Hz", "EEG",
                List.of("Cz", "Fz", "Pz", "Oz"), 256.0);
        // Three samples, four channels each.
        transport.publishSample("OpenViBE-EEG-256Hz", new double[]{1.0, 2.0, 3.0, 4.0}, 0.001);
        transport.publishSample("OpenViBE-EEG-256Hz", new double[]{1.5, 2.5, 3.5, 4.5}, 0.002);
        transport.publishSample("OpenViBE-EEG-256Hz", new double[]{2.0, 3.0, 4.0, 5.0}, 0.003);

        svc = new LslClientService(eegOnlyConfig(), transport, audit);
        svc.start();
        svc.poll();

        List<IInputSignal> drained = svc.drain("EEG-MAIN");
        // 3 samples × 3 configured channels (Cz, Fz, Pz)
        assertEquals(9, drained.size());
        assertTrue(drained.get(0) instanceof LFPSignal);
    }

    /**
     * S8 — time sync: emitted LFP timestamps include the LSL
     * {@code time_correction} offset within the spec's 2 ms tolerance.
     */
    @Test
    void s8_timeSyncAppliesTimeCorrection() {
        transport.publishStream("EEG-A", "EEG", List.of("Cz"), 256.0);
        transport.publishSample("EEG-A", new double[]{1.0}, 1.000);
        transport.setTimeCorrection("EEG-A", 0.005); // +5 ms

        svc = new LslClientService(singleEegConfig("EEG-A", List.of("Cz")), transport, audit);
        svc.start();
        svc.poll();
        List<IInputSignal> sigs = svc.drain("EEG-A-BIND");
        assertEquals(1, sigs.size());
        LFPSignal s = (LFPSignal) sigs.get(0);
        // Expect 1.005 s = 1_005_000_000 ns, ±2 ms tolerance per §9 S8.
        long expectedNs = 1_005_000_000L;
        long diffMs = Math.abs(s.getTimestampNs() - expectedNs) / 1_000_000L;
        assertTrue(diffMs <= 2, "time correction not applied within 2 ms; diff=" + diffMs + "ms");
    }

    /**
     * S9 — stream disappears: killing the publisher emits exactly one
     * {@code LSL_STREAM_LOST} alarm on the next poll and the cache stops
     * updating.
     */
    @Test
    void s9_streamLostEmitsAlarm() {
        transport.publishStream("EEG-A", "EEG", List.of("Cz"), 256.0);
        transport.publishSample("EEG-A", new double[]{1.0}, 0.001);
        svc = new LslClientService(singleEegConfig("EEG-A", List.of("Cz")), transport, audit);
        svc.start();
        svc.poll(); // one good sample
        // Drain so subsequent inspection of the queue is unambiguous.
        assertEquals(1, svc.drain("EEG-A-BIND").size());

        transport.killPublisher("EEG-A");
        svc.poll();

        List<IInputSignal> events = svc.drainEvents();
        assertEquals(1, events.size());
        AlarmSignal alarm = (AlarmSignal) events.get(0);
        assertEquals(LslClientService.LSL_STREAM_LOST, alarm.getConditionCode());

        // No further data flows even if the publisher pushes more samples.
        transport.publishSample("EEG-A", new double[]{2.0}, 0.002);
        svc.poll();
        assertTrue(svc.drain("EEG-A-BIND").isEmpty());
    }

    /**
     * S10 — channel mismatch: configured channel name {@code X9} does not
     * exist on the resolved stream → start fails fast.
     */
    @Test
    void s10_channelMismatchFailsFast() {
        transport.publishStream("EEG-A", "EEG", List.of("Cz", "Fz"), 256.0);
        svc = new LslClientService(singleEegConfig("EEG-A", List.of("Cz", "Fz", "X9")),
                transport, audit);
        Throwable t = assertThrows(LslTransport.LslTransportException.class, () -> svc.start());
        assertTrue(t.getMessage().contains("X9"));
    }

    /**
     * S11 — outlet write: a permitted {@link StimulationCommandSignal}
     * (gate returns {@code null}) results in a marker on the
     * {@code Jneopallium-Stim-Advisory} outlet and an {@code APPLIED}
     * audit record.
     */
    @Test
    void s11_outletWriteAfterSafetyGatePass() throws IOException {
        bringFullConfig(BridgeSafetyMode.ADVISORY);
        StimulationCommandSignal stim = new StimulationCommandSignal(
                3, 50.0, 200.0, 100.0, 5, PolarityPattern.CATHODIC_FIRST_BIPHASIC);

        LslAdvisoryOutputAggregator agg = new LslAdvisoryOutputAggregator(svc, audit, allowingGate());
        agg.save(List.of(result(stim)), 1_000L, 1L, null);

        List<String> markers = transport.markersOn("Jneopallium-Stim-Advisory");
        assertEquals(1, markers.size());
        assertTrue(markers.get(0).startsWith("STIM "));

        String auditLine = readAudit();
        assertTrue(auditLine.contains("\"verdict\":\"APPLIED\""));
        assertTrue(auditLine.contains("\"tag\":\"BCI.STIM.ADVISORY\""));
    }

    /**
     * S3 — SHADOW mode rejects writes (the marker is not pushed onto the
     * outlet; an audit is written with reason {@code SHADOW_MODE}).
     */
    @Test
    void s3_shadowModeRejectsStimulation() throws IOException {
        bringFullConfig(BridgeSafetyMode.SHADOW);
        StimulationCommandSignal stim = new StimulationCommandSignal(
                3, 50.0, 200.0, 100.0, 5, PolarityPattern.CATHODIC_FIRST_BIPHASIC);
        LslAdvisoryOutputAggregator agg = new LslAdvisoryOutputAggregator(svc, audit, allowingGate());
        agg.save(List.of(result(stim)), 1_000L, 1L, null);

        assertTrue(transport.markersOn("Jneopallium-Stim-Advisory").isEmpty());
        assertTrue(readAudit().contains("\"reason\":\"SHADOW_MODE\""));
    }

    /**
     * S6 — unknown tag rejected: an Intent signal arriving with no Intent
     * outlet configured produces no write.
     */
    @Test
    void s6_unknownTagSkippedSilently() {
        bringFullConfig(BridgeSafetyMode.ADVISORY);
        IntentSignal intent = new IntentSignal(IntentKind.NONE, new double[]{0.1}, 0.5);
        LslAdvisoryOutputAggregator agg = new LslAdvisoryOutputAggregator(svc, audit, allowingGate());
        agg.save(List.of(result(intent)), 1_000L, 1L, null);

        // No Intent outlet was configured by bringFullConfig → silently skipped.
        assertTrue(transport.markersOn("Jneopallium-Intent").isEmpty());
    }

    /**
     * S5 — audit failure isolation: open audit at an unwritable path; the
     * bridge degrades gracefully and the outlet still gets the marker.
     */
    @Test
    void s5_auditFailureIsolated() throws IOException {
        Path bad = tempDir.resolve("non-existent/dir/that/cannot/be/created");
        // Pre-create the path as a file so creating the parent dir fails.
        Files.createFile(tempDir.resolve("non-existent"));
        LslAuditOutput badAudit = new LslAuditOutput(bad);
        try {
            bringFullConfig(BridgeSafetyMode.ADVISORY, badAudit);
            StimulationCommandSignal stim = new StimulationCommandSignal(
                    3, 50.0, 200.0, 100.0, 5, PolarityPattern.CATHODIC_FIRST_BIPHASIC);
            LslAdvisoryOutputAggregator agg = new LslAdvisoryOutputAggregator(svc, badAudit, allowingGate());
            agg.save(List.of(result(stim)), 1_000L, 1L, null);
            // The marker must still be published despite audit-write failure.
            assertEquals(1, transport.markersOn("Jneopallium-Stim-Advisory").size());
            assertTrue(badAudit.isDegraded());
        } finally {
            badAudit.close();
        }
    }

    /**
     * Stimulation gate veto: gate returns a non-null reason → REJECTED.
     */
    @Test
    void stimulationGateVetoRejectsCommand() throws IOException {
        bringFullConfig(BridgeSafetyMode.ADVISORY);
        LslAdvisoryOutputAggregator.StimulationGate veto =
                (cmd, tick) -> "charge_density_exceeds_shannon";
        LslAdvisoryOutputAggregator agg = new LslAdvisoryOutputAggregator(svc, audit, veto);
        StimulationCommandSignal stim = new StimulationCommandSignal(
                3, 5_000.0, 600.0, 100.0, 5, PolarityPattern.CATHODIC_FIRST_BIPHASIC);
        agg.save(List.of(result(stim)), 1_000L, 1L, null);

        assertTrue(transport.markersOn("Jneopallium-Stim-Advisory").isEmpty());
        assertTrue(readAudit().contains("GATE_VETO:charge_density_exceeds_shannon"));
    }

    /**
     * Interlock has direct authority — fail-safe value pushed onto the
     * Risk outlet regardless of mode.
     */
    @Test
    void interlockDrivesFailsafeOnRiskOutlet() throws IOException {
        bringFullConfig(BridgeSafetyMode.SHADOW); // even SHADOW does not block
        InterlockSignal il = new InterlockSignal();
        il.setInterlockId("RISK-ADVISORY");
        il.setTripped(true);
        LslAdvisoryOutputAggregator agg = new LslAdvisoryOutputAggregator(svc, audit, allowingGate());
        agg.save(List.of(result(il)), 1_000L, 1L, null);
        // Risk outlet receives the fail-safe (1.0).
        List<double[]> samples = transport.numericOn("Jneopallium-Risk");
        assertEquals(1, samples.size());
        assertEquals(1.0, samples.get(0)[0], 1e-9);
        assertTrue(readAudit().contains("\"verdict\":\"INTERLOCK_TRIP\""));
    }

    /**
     * Operator override blocks subsequent SeizureRisk writes for the
     * tag's TTL.
     */
    @Test
    void operatorOverrideHoldsRiskWrites() throws IOException {
        bringFullConfig(BridgeSafetyMode.ADVISORY);
        OperatorOverrideSignal ov = new OperatorOverrideSignal(
                "BCI.RISK", OverrideKind.MANUAL, "operator-1", "drill", 0.5);
        SeizureRiskSignal risk = new SeizureRiskSignal(0.7, SeizureMarker.NONE, 0);
        LslAdvisoryOutputAggregator agg = new LslAdvisoryOutputAggregator(svc, audit, allowingGate());
        agg.save(List.of(result(ov), result(risk)), 1_000L, 1L, null);
        assertTrue(transport.numericOn("Jneopallium-Risk").isEmpty());
        assertTrue(readAudit().contains("\"verdict\":\"OVERRIDE_HOLD\""));
    }

    /**
     * Marker stream → Calibration when the cue regex matches; raw marker
     * text is pseudonymised (does not appear in the signal session id).
     */
    @Test
    void markerCueMatchesEmitsCalibrationSignalWithPseudonymisedId() {
        transport.publishStream("CueMarkers", "Markers", List.of(), 0.0);
        svc = new LslClientService(markerOnlyConfig(), transport, audit);
        svc.start();
        transport.publishMarker("CueMarkers", "subject_42_calibration_start", 0.001);
        transport.publishMarker("CueMarkers", "boring_event", 0.002);
        svc.poll();

        List<IInputSignal> drained = svc.drain("CALIB");
        assertEquals(1, drained.size());
        CalibrationSignal cal = (CalibrationSignal) drained.get(0);
        // §10 R4 — pseudonymised: the raw subject id MUST NOT appear in the
        // emitted session id.
        assertFalse(cal.getSessionId().contains("subject_42"));
        assertTrue(cal.getSessionId().startsWith("MARKER-"));
    }

    /**
     * S4-equivalent — onReconnected drops the cache and emits a
     * BRIDGE_RECONNECTED advisory event.
     */
    @Test
    void onReconnectedClearsCacheAndEmitsAdvisory() {
        transport.publishStream("EEG-A", "EEG", List.of("Cz"), 256.0);
        svc = new LslClientService(singleEegConfig("EEG-A", List.of("Cz")), transport, audit);
        svc.start();
        transport.publishSample("EEG-A", new double[]{1.0}, 0.001);
        svc.poll();
        assertEquals(1, svc.drain("EEG-A-BIND").size());

        transport.publishSample("EEG-A", new double[]{2.0}, 0.002);
        svc.poll();
        svc.onReconnected();
        // Subsequent drain returns nothing (cache dropped) but events
        // contain BRIDGE_RECONNECTED.
        assertTrue(svc.drain("EEG-A-BIND").isEmpty());
        List<IInputSignal> events = svc.drainEvents();
        assertEquals(1, events.size());
        assertEquals(LslClientService.BRIDGE_RECONNECTED,
                ((AlarmSignal) events.get(0)).getConditionCode());
    }

    /**
     * Physiology mapping — HRV → InteroceptiveSignal carries source field
     * with the LSL streamType, not raw subject metadata.
     */
    @Test
    void hrvMapsToInteroceptiveSignal() {
        transport.publishStream("Polar-HRV", "HRV", List.of("RR"), 0.0);
        transport.publishSample("Polar-HRV", new double[]{800.0}, 0.001);
        svc = new LslClientService(hrvConfig(), transport, audit);
        svc.start();
        svc.poll();
        List<IInputSignal> drained = svc.drain("HRV");
        assertEquals(1, drained.size());
        InteroceptiveSignal s = (InteroceptiveSignal) drained.get(0);
        assertEquals(800.0, s.getEnergyBudget(), 1e-9);
        assertEquals("HRV", s.getSource()); // not a subject id
    }

    /**
     * Eye stream → AppraisalSignal; gaze-x/y plus pupil diameter map onto
     * goalDelta / novelty / controllability respectively.
     */
    @Test
    void eyeStreamMapsToAppraisalSignal() {
        transport.publishStream("Eye", "Eye", List.of("gaze_x", "gaze_y", "pupil"), 60.0);
        transport.publishSample("Eye", new double[]{0.3, -0.1, 4.2}, 0.001);
        svc = new LslClientService(eyeConfig(), transport, audit);
        svc.start();
        svc.poll();
        List<IInputSignal> drained = svc.drain("EYE");
        AppraisalSignal s = (AppraisalSignal) drained.get(0);
        assertEquals(0.3, s.getGoalDelta(), 1e-9);
        assertEquals(-0.1, s.getNovelty(), 1e-9);
        assertEquals(4.2, s.getControllability(), 1e-9);
    }

    /* ===== fixtures ====================================================== */

    private void bringFullConfig(BridgeSafetyMode stimMode) {
        bringFullConfig(stimMode, audit);
    }

    private void bringFullConfig(BridgeSafetyMode stimMode, LslAuditOutput sink) {
        transport.publishStream("OpenViBE-EEG-256Hz", "EEG", List.of("Cz", "Fz"), 256.0);
        LslBridgeConfig cfg = new LslBridgeConfig(
                new LslBridgeConfig.DiscoveryConfig(2_000L, List.of()),
                List.of(new LslBridgeConfig.ReadBindingConfig(
                        "EEG-MAIN", "OpenViBE-EEG-256Hz", "EEG",
                        List.of("Cz", "Fz"), 64,
                        LslBridgeConfig.ReadSignalKind.LFP,
                        null, "BCI.EEG", 1, null, 4096)),
                List.of(new LslBridgeConfig.WriteBindingConfig(
                                "STIM-ADVISORY", "Jneopallium-Stim-Advisory",
                                LslBridgeConfig.OutletKind.MARKERS, 0.0,
                                "BCI.STIM.ADVISORY", null, null, null, null, true),
                        new LslBridgeConfig.WriteBindingConfig(
                                "RISK-ADVISORY", "Jneopallium-Risk",
                                LslBridgeConfig.OutletKind.NUMERIC, 10.0,
                                "BCI.RISK", 0.0, 1.0, null, 1.0, false)),
                new LslBridgeConfig.AuditConfig(tempDir.resolve("ignored.jsonl").toString()),
                Map.of("STIM-ADVISORY", stimMode, "RISK-ADVISORY", BridgeSafetyMode.ADVISORY),
                Duration.ofMillis(250));
        svc = new LslClientService(cfg, transport, sink);
        svc.start();
    }

    private LslBridgeConfig eegOnlyConfig() {
        return new LslBridgeConfig(
                new LslBridgeConfig.DiscoveryConfig(1_000L, List.of("OpenViBE-EEG-256Hz")),
                List.of(new LslBridgeConfig.ReadBindingConfig(
                        "EEG-MAIN", "OpenViBE-EEG-256Hz", "EEG",
                        List.of("Cz", "Fz", "Pz"), 64,
                        LslBridgeConfig.ReadSignalKind.LFP,
                        null, "BCI.EEG", 1, null, 4096)),
                List.of(),
                new LslBridgeConfig.AuditConfig(tempDir.resolve("ignored.jsonl").toString()),
                Map.of(),
                Duration.ofMillis(250));
    }

    private LslBridgeConfig singleEegConfig(String streamName, List<String> channels) {
        return new LslBridgeConfig(
                new LslBridgeConfig.DiscoveryConfig(1_000L, List.of()),
                List.of(new LslBridgeConfig.ReadBindingConfig(
                        streamName + "-BIND", streamName, "EEG", channels, 64,
                        LslBridgeConfig.ReadSignalKind.LFP, null, "BCI.EEG", 1, null, 4096)),
                List.of(),
                new LslBridgeConfig.AuditConfig(tempDir.resolve("ignored.jsonl").toString()),
                Map.of(),
                Duration.ofMillis(250));
    }

    private LslBridgeConfig markerOnlyConfig() {
        return new LslBridgeConfig(
                new LslBridgeConfig.DiscoveryConfig(1_000L, List.of()),
                List.of(new LslBridgeConfig.ReadBindingConfig(
                        "CALIB", "CueMarkers", "Markers",
                        List.of(), 64,
                        LslBridgeConfig.ReadSignalKind.CALIBRATION_MARKER,
                        "BCI.CAL", null, 1, "calibration", 4096)),
                List.of(),
                new LslBridgeConfig.AuditConfig(tempDir.resolve("ignored.jsonl").toString()),
                Map.of(),
                Duration.ofMillis(250));
    }

    private LslBridgeConfig hrvConfig() {
        return new LslBridgeConfig(
                new LslBridgeConfig.DiscoveryConfig(1_000L, List.of()),
                List.of(new LslBridgeConfig.ReadBindingConfig(
                        "HRV", "Polar-HRV", "HRV", List.of("RR"), 1,
                        LslBridgeConfig.ReadSignalKind.INTEROCEPTIVE,
                        "BCI.HRV", null, 1, null, 4096)),
                List.of(),
                new LslBridgeConfig.AuditConfig(tempDir.resolve("ignored.jsonl").toString()),
                Map.of(),
                Duration.ofMillis(250));
    }

    private LslBridgeConfig eyeConfig() {
        return new LslBridgeConfig(
                new LslBridgeConfig.DiscoveryConfig(1_000L, List.of()),
                List.of(new LslBridgeConfig.ReadBindingConfig(
                        "EYE", "Eye", "Eye",
                        List.of("gaze_x", "gaze_y", "pupil"), 1,
                        LslBridgeConfig.ReadSignalKind.APPRAISAL,
                        "BCI.EYE", null, 1, null, 4096)),
                List.of(),
                new LslBridgeConfig.AuditConfig(tempDir.resolve("ignored.jsonl").toString()),
                Map.of(),
                Duration.ofMillis(250));
    }

    private String readAudit() throws IOException {
        Path p = tempDir.resolve("lsl-audit.jsonl");
        if (!Files.exists(p)) return "";
        return Files.readString(p);
    }

    private static <K extends IResultSignal<?>> IResult<K> result(K s) {
        return new SimpleResultWrapper<>(s, 7L);
    }

    /** Stub gate that always allows. */
    private static LslAdvisoryOutputAggregator.StimulationGate allowingGate() {
        return (cmd, tick) -> null;
    }
}
