/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.plc4x;

import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeSafetyMode;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.Quality;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.ActuatorCommandSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.AlarmSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.InterlockSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MeasurementSignal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the PLC4X bridge covering 00-FRAMEWORK §5
 * scenarios S1, S2, S3, S6 and 01-PLC4X.md §8 bridge-specific S7–S11.
 *
 * <p>Uses {@link StubPlc4xDriver} (pure Java in-memory PLC simulator) so no
 * native PLC, no JPype, no docker container, no flaky network is involved.
 */
class Plc4xBridgeIntegrationTest {

    @TempDir Path tempDir;

    private StubPlc4xDriver driver;
    private Plc4xClientService svc;
    private Plc4xAuditOutput audit;
    private Plc4xCommandOutputAggregator aggregator;
    private Plc4xMeasurementInput measurementInput;
    private Plc4xEventInput eventInput;

    @BeforeEach
    void setUp() {
        driver = new StubPlc4xDriver();
        // Pre-seed values so validate() and the first poll succeed
        driver.open("S7", "s7://10.10.0.1");
        driver.setValue("S7", "%DB1.DBD0:REAL", 0.0);
        driver.setValue("S7", "%DB1.DBD8:REAL", 0.0);
        driver.setValue("S7", "%DB100.DBW0:WORD", 0);
        driver.open("MODBUS", "modbus-tcp://10.10.0.2:502");
        driver.setValue("MODBUS", "coil:0", Boolean.FALSE);
        // Re-close them so service.connect() opens cleanly
        driver.closeAll();
    }

    @AfterEach
    void tearDown() {
        if (svc != null) svc.close();
        if (audit != null) audit.close();
    }

    // ===========================================================================
    // S1 — Pure read: bridge connects, value flows through to MeasurementSignal
    // ===========================================================================
    @Test
    void s1_pureReadEmitsMeasurementSignal() throws Exception {
        Plc4xConfig cfg = configForS1();
        startBridge(cfg, BridgeSafetyMode.SHADOW);

        // Pre-seed the value the bridge will read, then wait for a fresh poll
        driver.setValue("S7", "%DB1.DBD0:REAL", 72.5);
        waitForCachedValue("PLANT.TIC101.PV", 72.5, 2000);

        List<IInputSignal> sigs = measurementInput.readSignals();
        MeasurementSignal m = sigs.stream()
                .map(MeasurementSignal.class::cast)
                .filter(x -> "PLANT.TIC101.PV".equals(x.getTag()))
                .findFirst().orElseThrow();
        assertEquals(72.5, m.getMeasurement(), 1e-9);
        assertEquals(Quality.GOOD, m.getQuality());
    }

    // ===========================================================================
    // S2 — Bad quality propagates: NOT_FOUND → Quality.UNCERTAIN
    // ===========================================================================
    @Test
    void s2_badQualityPropagates() throws Exception {
        Plc4xConfig cfg = configForS1();
        startBridge(cfg, BridgeSafetyMode.SHADOW);

        // Wait for one good poll, then drop the connection to force REMOTE_ERROR
        waitForPolls("TIC-101", 1, 2000);
        driver.dropConnection("S7");
        // Wait for the cache to flip to a non-OK response
        long deadline = System.currentTimeMillis() + 2000;
        while (System.currentTimeMillis() < deadline) {
            Plc4xDriver.ReadResponse r = svc.latest("PLANT.TIC101.PV");
            if (r != null && r.code() != Plc4xResponseCode.OK) break;
            Thread.sleep(50);
        }

        List<IInputSignal> sigs = measurementInput.readSignals();
        assertFalse(sigs.isEmpty());
        MeasurementSignal m = (MeasurementSignal) sigs.get(0);
        // REMOTE_ERROR while disconnected → Quality.BAD
        assertEquals(Quality.BAD, m.getQuality(),
                "non-OK PlcResponseCode must propagate as Quality.BAD/UNCERTAIN");
    }

    // ===========================================================================
    // S3 — SHADOW mode rejects writes: nothing reaches the PLC, audit recorded
    // ===========================================================================
    @Test
    void s3_shadowModeRejectsWrite() throws Exception {
        Plc4xConfig cfg = configForS3(BridgeSafetyMode.SHADOW);
        startBridge(cfg, BridgeSafetyMode.SHADOW);

        ActuatorCommandSignal cmd = new ActuatorCommandSignal(
                "PLANT.TIC101.SP", 50.0, 50.0, true);
        aggregator.save(List.of(new FakeResult(cmd)), System.currentTimeMillis(), 0L, null);

        // Stub never saw the SP write
        assertNull(driver.lastWrite("S7", "%DB1.DBD8:REAL"),
                "SHADOW mode must not perform a protocol write");

        // Audit recorded the rejection
        audit.close();   // flush + close so the file is fully written
        Path auditFile = Path.of(cfg.audit().localAuditFile());
        assertTrue(Files.exists(auditFile), "audit file must exist");
        String content = Files.readString(auditFile);
        assertTrue(content.contains("\"verdict\":\"REJECTED\""), content);
        assertTrue(content.contains("SHADOW_MODE"), content);
    }

    // ===========================================================================
    // S6 — Unknown tag rejected
    // ===========================================================================
    @Test
    void s6_unknownTagRejected() throws Exception {
        Plc4xConfig cfg = configForS3(BridgeSafetyMode.AUTONOMOUS);
        startBridge(cfg, BridgeSafetyMode.AUTONOMOUS);

        ActuatorCommandSignal cmd = new ActuatorCommandSignal(
                "PLANT.NOT_A_TAG", 1.0, 1.0, true);
        aggregator.save(List.of(new FakeResult(cmd)), System.currentTimeMillis(), 0L, null);

        // The configured (and known) tag was never touched
        assertNull(driver.lastWrite("S7", "%DB1.DBD8:REAL"));

        audit.close();
        Path auditFile = Path.of(cfg.audit().localAuditFile());
        String content = Files.readString(auditFile);
        assertTrue(content.contains("UNKNOWN_TAG"), content);
    }

    // ===========================================================================
    // S7 — Multi-driver coexistence: S7 + Modbus in one config both produce signals
    // ===========================================================================
    @Test
    void s7_multiDriverCoexistence() throws Exception {
        Plc4xConfig cfg = configForS1();
        startBridge(cfg, BridgeSafetyMode.SHADOW);

        driver.setValue("S7", "%DB1.DBD0:REAL", 11.0);
        driver.setValue("MODBUS", "coil:0", Boolean.TRUE);

        waitForCachedValue("PLANT.TIC101.PV", 11.0, 2000);
        waitForCachedValue("PLANT.PUMP01.STATE", Boolean.TRUE, 2000);

        List<IInputSignal> sigs = measurementInput.readSignals();
        assertEquals(2, sigs.size(), "both S7 and Modbus measurements should be present");
        boolean sawS7 = false, sawModbus = false;
        for (IInputSignal s : sigs) {
            MeasurementSignal m = (MeasurementSignal) s;
            if ("PLANT.TIC101.PV".equals(m.getTag())) {
                assertEquals(11.0, m.getMeasurement(), 1e-9);
                sawS7 = true;
            }
            if ("PLANT.PUMP01.STATE".equals(m.getTag())) {
                assertEquals(1.0, m.getMeasurement(), 1e-9, "Boolean coil → 1.0");
                sawModbus = true;
            }
        }
        assertTrue(sawS7 && sawModbus);
    }

    // ===========================================================================
    // S8 — Polling rate respected: 1Hz binding produces ~1 read/sec
    // ===========================================================================
    @Test
    void s8_pollingRateRespected() throws Exception {
        Plc4xConfig cfg = new Plc4xConfig(
                List.of(new Plc4xConfig.ConnectionConfig(
                        "S7", "s7://10.10.0.1", Duration.ofSeconds(1), null)),
                List.of(new Plc4xConfig.ReadBindingConfig(
                        "TIC-101", "S7", "%DB1.DBD0:REAL", "PLANT.TIC101.PV", 250L)),
                List.of(),
                List.of(),
                new Plc4xConfig.AuditConfig(
                        tempDir.resolve("s8.jsonl").toString(), true),
                Map.of(),
                Duration.ofMillis(250));
        startBridge(cfg, BridgeSafetyMode.SHADOW);
        driver.setValue("S7", "%DB1.DBD0:REAL", 1.0);

        // Poll for ~1s with 250ms cadence → expect ~4 reads (allow 3..6 jitter)
        Thread.sleep(1100);
        long n = svc.pollCount("TIC-101");
        assertTrue(n >= 3 && n <= 8,
                "expected 3..8 polls in 1.1s at 250ms cadence, got " + n);
    }

    // ===========================================================================
    // S9 — Address rejected at startup: connect() fails fast
    // ===========================================================================
    @Test
    void s9_addressRejectedAtStartupFailsFast() {
        StubPlc4xDriver d = new StubPlc4xDriver();
        d.open("S7", "s7://10.10.0.1");
        d.rejectAddress("S7", "%TYPO.DBD0:REAL");
        d.closeAll();

        Plc4xConfig cfg = new Plc4xConfig(
                List.of(new Plc4xConfig.ConnectionConfig("S7", "s7://10.10.0.1", null, null)),
                List.of(new Plc4xConfig.ReadBindingConfig(
                        "BAD", "S7", "%TYPO.DBD0:REAL", "PLANT.BAD.PV", 250L)),
                List.of(), List.of(),
                new Plc4xConfig.AuditConfig(tempDir.resolve("s9.jsonl").toString(), true),
                Map.of(), Duration.ofMillis(250));

        Plc4xClientService bad = new Plc4xClientService(d, cfg);
        Plc4xException ex = assertThrows(Plc4xException.class, bad::connect,
                "startup must fail when an address is rejected");
        assertTrue(ex.getMessage().contains("BAD"));
        assertTrue(ex.getMessage().contains("INVALID_ADDRESS"));
        bad.close();
    }

    // ===========================================================================
    // S10 — Driver missing for scheme
    // ===========================================================================
    @Test
    void s10_missingDriverFailsAtStartup() {
        StubPlc4xDriver d = new StubPlc4xDriver();
        Plc4xConfig cfg = new Plc4xConfig(
                List.of(new Plc4xConfig.ConnectionConfig(
                        "BOGUS", "made-up-scheme://nope", null, null)),
                List.of(), List.of(), List.of(),
                new Plc4xConfig.AuditConfig(tempDir.resolve("s10.jsonl").toString(), true),
                Map.of(), Duration.ofMillis(250));
        Plc4xClientService bad = new Plc4xClientService(d, cfg);
        Plc4xException ex = assertThrows(Plc4xException.class, bad::connect);
        assertTrue(ex.getMessage().toLowerCase().contains("no driver"),
                "expected 'no driver' message, got: " + ex.getMessage());
        bad.close();
    }

    // ===========================================================================
    // S11 — Severity-map decode produces the right priority
    // ===========================================================================
    @Test
    void s11_severityMapDecodeProducesCriticalAlarm() throws Exception {
        Plc4xConfig cfg = configWithEvents();
        startBridge(cfg, BridgeSafetyMode.SHADOW);

        driver.setValue("S7", "%DB100.DBW0:WORD", 0x0010);
        long deadline = System.currentTimeMillis() + 2000;
        while (System.currentTimeMillis() < deadline) {
            Plc4xDriver.ReadResponse r = svc.latest("PLANT.LINE_A.TROUBLE");
            if (r != null && r.value() != null && ((Number) r.value()).intValue() == 0x0010) break;
            Thread.sleep(50);
        }

        List<IInputSignal> sigs = eventInput.readSignals();
        assertEquals(1, sigs.size());
        AlarmSignal a = (AlarmSignal) sigs.get(0);
        assertEquals(com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.AlarmPriority.URGENT,
                a.getPriority(), "0x0010 → CRITICAL → URGENT priority");
    }

    // ===========================================================================
    // Bonus: an interlock trip drives the bound output to its fail-safe value.
    // Verifies §0.2 (interlock has direct authority — no SHADOW veto) on PLC4X.
    // ===========================================================================
    @Test
    void interlockTripDrivesFailSafeRegardlessOfSafetyMode() throws Exception {
        Plc4xConfig cfg = configForS3(BridgeSafetyMode.SHADOW);
        startBridge(cfg, BridgeSafetyMode.SHADOW);

        InterlockSignal il = new InterlockSignal("TIC-101", true, List.of("OVERTEMP"));
        aggregator.save(List.of(new FakeResult(il)), System.currentTimeMillis(), 0L, null);

        Object written = driver.lastWrite("S7", "%DB1.DBD8:REAL");
        assertNotNull(written, "interlock must drive failSafeValue regardless of SHADOW");
        assertEquals(0.0, ((Number) written).doubleValue(), 1e-9);
    }

    // ===========================================================================
    // Helpers
    // ===========================================================================

    private void startBridge(Plc4xConfig cfg, BridgeSafetyMode ignoredDefault) {
        svc = new Plc4xClientService(driver, cfg);
        svc.connect();
        audit = new Plc4xAuditOutput(Path.of(cfg.audit().localAuditFile()));
        aggregator = new Plc4xCommandOutputAggregator(svc, cfg, audit);
        measurementInput = new Plc4xMeasurementInput("plc4x-meas", svc, cfg.reads());
        eventInput = new Plc4xEventInput("plc4x-evt", svc, cfg.events());
    }

    private void waitForPolls(String bindingId, long minPolls, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (svc.pollCount(bindingId) >= minPolls) return;
            Thread.sleep(20);
        }
        fail("Binding '" + bindingId + "' did not reach " + minPolls + " polls in " + timeoutMs + "ms");
    }

    /** Wait until the latest cache for {@code signalTag} matches the expected value. */
    private void waitForCachedValue(String signalTag, Object expected, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            Plc4xDriver.ReadResponse r = svc.latest(signalTag);
            if (r != null && r.code() == Plc4xResponseCode.OK
                    && java.util.Objects.equals(r.value(), expected)) return;
            Thread.sleep(20);
        }
        Plc4xDriver.ReadResponse r = svc.latest(signalTag);
        fail("Signal '" + signalTag + "' did not reach " + expected + " in " + timeoutMs
                + "ms; latest=" + (r == null ? "null" : r.code() + "/" + r.value()));
    }

    /** S1/S2/S7 config: two connections, two reads, one event. */
    private Plc4xConfig configForS1() throws IOException {
        Path auditFile = Files.createTempFile(tempDir, "s1-", ".jsonl");
        Map<String, String> sev = new LinkedHashMap<>();
        sev.put("0x0001", "LOW");
        sev.put("0x0010", "CRITICAL");
        return new Plc4xConfig(
                List.of(
                        new Plc4xConfig.ConnectionConfig(
                                "S7", "s7://10.10.0.1", Duration.ofSeconds(1), null),
                        new Plc4xConfig.ConnectionConfig(
                                "MODBUS", "modbus-tcp://10.10.0.2:502", Duration.ofSeconds(1), null)),
                List.of(
                        new Plc4xConfig.ReadBindingConfig(
                                "TIC-101", "S7", "%DB1.DBD0:REAL", "PLANT.TIC101.PV", 100L),
                        new Plc4xConfig.ReadBindingConfig(
                                "PUMP-RUN", "MODBUS", "coil:0", "PLANT.PUMP01.STATE", 100L)),
                List.of(),
                List.of(new Plc4xConfig.EventBindingConfig(
                        "TROUBLE", "S7", "%DB100.DBW0:WORD", "PLANT.LINE_A.TROUBLE", 100L, sev)),
                new Plc4xConfig.AuditConfig(auditFile.toString(), true),
                Map.of(), Duration.ofMillis(250));
    }

    /** S3/S6/interlock config: single S7 connection with one read & one write binding. */
    private Plc4xConfig configForS3(BridgeSafetyMode mode) throws IOException {
        Path auditFile = Files.createTempFile(tempDir, "s3-", ".jsonl");
        return new Plc4xConfig(
                List.of(new Plc4xConfig.ConnectionConfig(
                        "S7", "s7://10.10.0.1", Duration.ofSeconds(1), null)),
                List.of(new Plc4xConfig.ReadBindingConfig(
                        "TIC-101", "S7", "%DB1.DBD0:REAL", "PLANT.TIC101.PV", 100L)),
                List.of(new Plc4xConfig.WriteBindingConfig(
                        "TIC-101", "S7", "%DB1.DBD8:REAL", "PLANT.TIC101.SP",
                        0.0, 2.0, 0.0, 100.0)),
                List.of(),
                new Plc4xConfig.AuditConfig(auditFile.toString(), true),
                Map.of("TIC-101", mode), Duration.ofMillis(250));
    }

    /** S11 config: one event binding with severity map. */
    private Plc4xConfig configWithEvents() throws IOException {
        Path auditFile = Files.createTempFile(tempDir, "s11-", ".jsonl");
        Map<String, String> sev = new LinkedHashMap<>();
        sev.put("0x0001", "LOW");
        sev.put("0x0002", "HIGH");
        sev.put("0x0010", "CRITICAL");
        return new Plc4xConfig(
                List.of(new Plc4xConfig.ConnectionConfig(
                        "S7", "s7://10.10.0.1", Duration.ofSeconds(1), null)),
                List.of(),
                List.of(),
                List.of(new Plc4xConfig.EventBindingConfig(
                        "TROUBLE", "S7", "%DB100.DBW0:WORD", "PLANT.LINE_A.TROUBLE", 100L, sev)),
                new Plc4xConfig.AuditConfig(auditFile.toString(), true),
                Map.of(), Duration.ofMillis(250));
    }
}
