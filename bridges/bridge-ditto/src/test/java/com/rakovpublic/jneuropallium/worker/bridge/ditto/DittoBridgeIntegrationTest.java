/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.ditto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeSafetyMode;
import com.rakovpublic.jneuropallium.worker.net.layers.IResult;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.Quality;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.AlarmSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MeasurementSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.SetpointSignal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the Eclipse Ditto bridge. Covers the universal
 * 00-FRAMEWORK §5 scenarios that apply (S1, S3, S4, S5, S6) plus the
 * bridge-specific scenarios S7–S11 from 10-DITTO.md §8. Runs against
 * {@link InMemoryDittoTransport} — no Ditto sandbox required.
 */
class DittoBridgeIntegrationTest {

    @TempDir Path tempDir;

    private InMemoryDittoTransport transport;
    private DittoAuditOutput audit;
    private DittoClientService svc;
    private DittoSignalMapper mapper;

    @BeforeEach
    void setUp() {
        transport = new InMemoryDittoTransport();
    }

    @AfterEach
    void tearDown() {
        if (svc != null) svc.close();
        if (audit != null) audit.close();
    }

    /* ===== framework universal scenarios ====================================== */

    /** S1 / S7: a feature-property modify event delivers a typed {@link MeasurementSignal}. */
    @Test
    void s1_propertyModifiedEmitsMeasurementSignal() throws Exception {
        DittoBridgeConfig cfg = baseConfig(
                List.of(read("PUMP1-VIB", "factory.line-a:pump-1", "vibration", "rms_z",
                        "PUMP01.VIB.Z")),
                List.of());
        bring(cfg);

        deliverPropertyModified("factory.line-a:pump-1", "vibration", "rms_z", 4.2);

        List<IInputSignal> signals = svc.drainMeasurements("PUMP1-VIB");
        assertEquals(1, signals.size());
        MeasurementSignal m = (MeasurementSignal) signals.get(0);
        assertEquals("PUMP01.VIB.Z", m.getTag());
        assertEquals(4.2, m.getMeasurement(), 0.0001);
        assertEquals(Quality.GOOD, m.getQuality());
    }

    /** S3: SHADOW mode rejects writes with the SHADOW_MODE audit reason. */
    @Test
    void s3_shadowModeRejectsWrite() throws Exception {
        DittoBridgeConfig cfg = baseConfig(
                List.of(),
                List.of(write("REACTOR1-ADVISED-SP", "factory.line-a:reactor-1",
                        "recommended_setpoint", "value", "REACTOR01.SP.ADV", null, null)),
                Map.of("REACTOR1-ADVISED-SP", BridgeSafetyMode.SHADOW));
        bring(cfg);

        DittoAdvisoryOutputAggregator agg = new DittoAdvisoryOutputAggregator(svc, audit);
        agg.save(List.of(result(setpoint("REACTOR01.SP.ADV", 50.0))),
                System.currentTimeMillis(), 1L, null);

        assertTrue(transport.puts().isEmpty(), "SHADOW mode must not write to Ditto");
        String log = Files.readString(tempDir.resolve("ditto-audit.jsonl"));
        assertTrue(log.contains("SHADOW_MODE"), "expected a SHADOW_MODE audit, got: " + log);
    }

    /** S4 / S11: reconnect drops the alive cache and emits a {@code BRIDGE_RECONNECTED} event. */
    @Test
    void s4_reconnectClearsCacheAndEmitsAdvisoryEvent() throws Exception {
        DittoBridgeConfig cfg = baseConfig(
                List.of(read("PUMP1-VIB", "factory.line-a:pump-1", "vibration", "rms_z",
                        "PUMP01.VIB.Z")),
                List.of());
        bring(cfg);

        assertTrue(svc.isThingAlive("factory.line-a:pump-1"));
        // Mark thing offline first to give the reconnect something to clear-and-restore.
        deliverThingDeleted("factory.line-a:pump-1");
        assertFalse(svc.isThingAlive("factory.line-a:pump-1"));

        svc.onReconnected();
        // After reconnect, the alive set is restored from configured things.
        assertTrue(svc.isThingAlive("factory.line-a:pump-1"));

        List<IInputSignal> events = svc.drainEvents();
        assertTrue(events.stream().anyMatch(e ->
                e instanceof AlarmSignal a
                        && DittoClientService.BRIDGE_RECONNECTED.equals(a.getConditionCode())));
    }

    /** S5: an unwritable audit file does not crash the bridge — degraded mode persists. */
    @Test
    void s5_auditFailureDoesNotKillBridge() throws Exception {
        Path file = tempDir.resolve("ditto-audit.jsonl");
        Files.writeString(file, "");
        DittoAuditOutput output = new DittoAuditOutput(file);
        output.close();
        // append() on a closed sink falls back to degraded; no exception expected.
        assertDoesNotThrow(() -> output.append(new com.rakovpublic.jneuropallium.worker.bridge.common.BridgeAuditRecord(
                System.currentTimeMillis(), 1, "ditto",
                com.rakovpublic.jneuropallium.worker.bridge.common.BridgeAuditRecord.Verdict.APPLIED,
                "X", "X.tag", null, null, null, null, List.of())));
    }

    /** S6: an aggregator that receives a command for an unknown tag audits {@code UNKNOWN_TAG}. */
    @Test
    void s6_unknownTagRejected() throws Exception {
        DittoBridgeConfig cfg = baseConfig(
                List.of(),
                List.of(write("OK", "factory.line-a:reactor-1",
                        "recommended_setpoint", "value", "KNOWN.TAG", null, null)));
        bring(cfg);

        DittoAdvisoryOutputAggregator agg = new DittoAdvisoryOutputAggregator(svc, audit);
        agg.save(List.of(result(setpoint("UNKNOWN.TAG", 1.0))),
                System.currentTimeMillis(), 1L, null);

        assertTrue(transport.puts().isEmpty());
        String log = Files.readString(tempDir.resolve("ditto-audit.jsonl"));
        assertTrue(log.contains("UNKNOWN_TAG"), "audit should mention UNKNOWN_TAG: " + log);
    }

    /* ===== bridge-specific scenarios ========================================== */

    /** S7: bridge connects and emits a MeasurementSignal per configured feature. */
    @Test
    void s7_connectAndEmitSignal() throws Exception {
        DittoBridgeConfig cfg = baseConfig(
                List.of(read("REACTOR1-TEMP", "factory.line-a:reactor-1", "temperature",
                        "current", "REACTOR01.TEMP")),
                List.of());
        bring(cfg);

        deliverPropertyModified("factory.line-a:reactor-1", "temperature", "current", 73.5);
        List<IInputSignal> sigs = svc.drainMeasurements("REACTOR1-TEMP");
        assertEquals(1, sigs.size());
        assertEquals(73.5, ((MeasurementSignal) sigs.get(0)).getMeasurement(), 0.0001);
    }

    /** S8: a twin update via the REST API surfaces as a property-modified event in the same shape. */
    @Test
    void s8_twinUpdateReflectsInCache() throws Exception {
        DittoBridgeConfig cfg = baseConfig(
                List.of(read("R", "factory.line-a:reactor-1", "temperature", "current", "R.TAG")),
                List.of());
        bring(cfg);

        deliverPropertyModified("factory.line-a:reactor-1", "temperature", "current", 50.0);
        deliverPropertyModified("factory.line-a:reactor-1", "temperature", "current", 60.0);
        List<IInputSignal> sigs = svc.drainMeasurements("R");
        assertEquals(2, sigs.size());
        assertEquals(60.0, ((MeasurementSignal) sigs.get(1)).getMeasurement(), 0.0001);
    }

    /** S9: a non-advisory write binding is rejected at config-load. */
    @Test
    void s9_configLoadRejectsNonAdvisoryWrite() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> baseConfig(
                        List.of(),
                        List.of(write("BAD", "factory.line-a:reactor-1",
                                "setpoint", "value", "BAD.TAG", null, null))));
        assertTrue(ex.getMessage().contains("advisory feature"),
                "Expected the loader/config to mention the advisory rule, got: " + ex.getMessage());
    }

    /**
     * S10: when a configured thing is deleted upstream, the bridge emits
     * {@code AlarmSignal(TWIN_OFFLINE)} and subsequent reads of any of its
     * features carry {@link Quality#UNCERTAIN}.
     */
    @Test
    void s10_thingDeletedAlarmAndUncertainQuality() throws Exception {
        DittoBridgeConfig cfg = baseConfig(
                List.of(read("PUMP1-VIB", "factory.line-a:pump-1", "vibration", "rms_z",
                        "PUMP01.VIB.Z")),
                List.of());
        bring(cfg);

        deliverThingDeleted("factory.line-a:pump-1");
        List<IInputSignal> events = svc.drainEvents();
        assertTrue(events.stream().anyMatch(s ->
                s instanceof AlarmSignal a
                        && DittoClientService.TWIN_OFFLINE.equals(a.getConditionCode())));
        assertFalse(svc.isThingAlive("factory.line-a:pump-1"));

        // A subsequent property modify is ingested with UNCERTAIN quality.
        deliverPropertyModified("factory.line-a:pump-1", "vibration", "rms_z", 4.2);
        List<IInputSignal> sigs = svc.drainMeasurements("PUMP1-VIB");
        assertEquals(1, sigs.size());
        assertEquals(Quality.UNCERTAIN, ((MeasurementSignal) sigs.get(0)).getQuality());
    }

    /** S11: reconnect re-arms subscriptions and emits an advisory event. */
    @Test
    void s11_reconnectEmitsAdvisoryEvent() throws Exception {
        DittoBridgeConfig cfg = baseConfig(
                List.of(read("PUMP1-VIB", "factory.line-a:pump-1", "vibration", "rms_z",
                        "PUMP01.VIB.Z")),
                List.of());
        bring(cfg);

        svc.onReconnected();
        List<IInputSignal> events = svc.drainEvents();
        assertTrue(events.stream().anyMatch(s ->
                s instanceof AlarmSignal a
                        && DittoClientService.BRIDGE_RECONNECTED.equals(a.getConditionCode())));
    }

    /** ADVISORY mode writes the recommended-feature property and audits APPLIED with clamping. */
    @Test
    void advisoryWriteAppliesAndClamps() throws Exception {
        DittoBridgeConfig cfg = baseConfig(
                List.of(),
                List.of(write("REACTOR1-ADVISED-SP", "factory.line-a:reactor-1",
                        "recommended_setpoint", "value", "REACTOR01.SP.ADV", 0.0, 100.0)));
        bring(cfg);

        DittoAdvisoryOutputAggregator agg = new DittoAdvisoryOutputAggregator(svc, audit);
        // 250.0 must be clamped to 100.0.
        agg.save(List.of(result(setpoint("REACTOR01.SP.ADV", 250.0))),
                System.currentTimeMillis(), 1L, null);

        List<InMemoryDittoTransport.Put> puts = transport.puts();
        assertEquals(1, puts.size());
        InMemoryDittoTransport.Put put = puts.get(0);
        assertEquals("recommended_setpoint", put.feature());
        assertEquals("value", put.property());
        // Body is a JSON scalar — extract and check.
        JsonNode body = new ObjectMapper().readTree(put.body());
        assertEquals(100.0, body.asDouble(), 0.0001);

        String log = Files.readString(tempDir.resolve("ditto-audit.jsonl"));
        assertTrue(log.contains("APPLIED"), "expected APPLIED audit; got: " + log);
        assertTrue(log.contains("CLAMPED_HIGH"), "expected CLAMPED_HIGH reason; got: " + log);
    }

    /** A representative APPLIED audit line conforms to the universal §4 schema. */
    @Test
    void auditLineConformsToFrameworkSchema() throws Exception {
        DittoBridgeConfig cfg = baseConfig(
                List.of(),
                List.of(write("S", "factory.line-a:reactor-1",
                        "advisory_value", "x", "S.tag", null, null)));
        bring(cfg);

        DittoAdvisoryOutputAggregator agg = new DittoAdvisoryOutputAggregator(svc, audit);
        agg.save(List.of(result(setpoint("S.tag", 1.0))), 100L, 7L, null);

        String log = Files.readString(tempDir.resolve("ditto-audit.jsonl")).trim();
        String[] lines = log.split("\n");
        JsonNode line = new ObjectMapper().readTree(lines[lines.length - 1]);
        assertEquals(7, line.get("run").asLong());
        assertEquals("ditto", line.get("bridge").asText());
        assertEquals("APPLIED", line.get("verdict").asText());
        assertEquals("S.tag", line.get("tag").asText());
    }

    /* ===== helpers ============================================================ */

    private DittoBridgeConfig baseConfig(
            List<DittoBridgeConfig.ReadBindingConfig> reads,
            List<DittoBridgeConfig.WriteBindingConfig> writes) {
        return baseConfig(reads, writes, Map.of());
    }

    private DittoBridgeConfig baseConfig(
            List<DittoBridgeConfig.ReadBindingConfig> reads,
            List<DittoBridgeConfig.WriteBindingConfig> writes,
            Map<String, BridgeSafetyMode> perTag) {
        DittoBridgeConfig.ConnectionConfig conn = new DittoBridgeConfig.ConnectionConfig(
                "https://ditto.local", "/ws/2", "/api/2",
                Duration.ofSeconds(5), 10_000);
        return new DittoBridgeConfig(
                conn,
                null,
                List.of(),
                reads, writes,
                new DittoBridgeConfig.AuditConfig(tempDir.resolve("ditto-audit.jsonl").toString()),
                perTag,
                Map.of(),
                Duration.ofMillis(250));
    }

    private DittoBridgeConfig.ReadBindingConfig read(String id, String thingId,
                                                     String feature, String property,
                                                     String tag) {
        return new DittoBridgeConfig.ReadBindingConfig(
                id, thingId, feature, property, tag,
                DittoBridgeConfig.ReadSignalKind.MEASUREMENT);
    }

    private DittoBridgeConfig.WriteBindingConfig write(String id, String thingId,
                                                       String feature, String property,
                                                       String tag,
                                                       Double min, Double max) {
        return new DittoBridgeConfig.WriteBindingConfig(
                id, thingId, feature, property, tag, min, max);
    }

    private SetpointSignal setpoint(String tag, double value) {
        SetpointSignal s = new SetpointSignal();
        s.setTag(tag);
        s.setSetpoint(value);
        return s;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private IResult result(IResultSignal sig) {
        return new IResult<IResultSignal>() {
            @Override public IResultSignal getResult() { return sig; }
            @Override public Long getNeuronId() { return 42L; }
        };
    }

    private void bring(DittoBridgeConfig cfg) {
        audit = new DittoAuditOutput(tempDir.resolve("ditto-audit.jsonl"));
        mapper = new DittoSignalMapper(cfg);
        svc = new DittoClientService(cfg, transport, mapper, audit);
        svc.start();
    }

    private void deliverPropertyModified(String thingId, String feature, String property, double value) {
        // Build a minimal Ditto-protocol envelope: { topic, path, value, timestamp }.
        String[] parts = thingId.split(":", 2);
        String topic = parts[0] + "/" + parts[1] + "/things/twin/events/modified";
        String json = "{"
                + "\"topic\":\"" + topic + "\","
                + "\"path\":\"/features/" + feature + "/properties/" + property + "\","
                + "\"value\":" + value
                + "}";
        transport.deliver(new DittoTransport.TwinEvent(
                DittoTransport.EventType.FEATURE_PROPERTY_MODIFIED, thingId, feature,
                json.getBytes(StandardCharsets.UTF_8)));
    }

    private void deliverThingDeleted(String thingId) {
        String[] parts = thingId.split(":", 2);
        String topic = parts[0] + "/" + parts[1] + "/things/twin/events/deleted";
        String json = "{"
                + "\"topic\":\"" + topic + "\","
                + "\"path\":\"/\""
                + "}";
        transport.deliver(new DittoTransport.TwinEvent(
                DittoTransport.EventType.THING_DELETED, thingId, null,
                json.getBytes(StandardCharsets.UTF_8)));
    }
}
