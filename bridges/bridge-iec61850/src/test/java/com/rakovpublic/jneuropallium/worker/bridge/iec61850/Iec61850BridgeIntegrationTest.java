/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.iec61850;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.AlarmPriority;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.Quality;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.AlarmSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MeasurementSignal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end tests for the IEC 61850 bridge (11-IEC61850.md §9 S7–S11
 * plus 00-FRAMEWORK §5 universal scenarios S1, S2, S4, S5).
 */
class Iec61850BridgeIntegrationTest {

    private static final String SCL = """
            <?xml version="1.0" encoding="UTF-8"?>
            <SCL xmlns="http://www.iec.ch/61850/2003/SCL">
              <IED name="RELAY-A1">
                <AccessPoint name="S1"><Server>
                  <LDevice inst="LD0">
                    <LN0 lnClass="LLN0" inst=""/>
                    <LN lnClass="MMXU" inst="1"/>
                    <LN lnClass="MMXU" inst="2"/>
                    <LN lnClass="XCBR" inst="1"/>
                    <LN lnClass="PIOC" inst="1"/>
                    <LN lnClass="PTOC" inst="1"/>
                  </LDevice>
                </Server></AccessPoint>
              </IED>
            </SCL>
            """;

    private Path writeScl(Path tmp) throws IOException {
        Path scl = tmp.resolve("SUB1.icd");
        Files.writeString(scl, SCL, StandardCharsets.UTF_8);
        return scl;
    }

    private Iec61850BridgeConfig baseConfig(Path tmp, Path auditFile) throws IOException {
        Path scl = writeScl(tmp);
        return new Iec61850BridgeConfig(
                List.of(new Iec61850BridgeConfig.IedConfig(
                        "RELAY-A1", "10.50.1.10", 102,
                        scl.toString(), "LD0/LLN0.RP.urcbA01")),
                List.of(
                        new Iec61850BridgeConfig.DaReadConfig(
                                "BUSBAR-V", "RELAY-A1",
                                "LD0/MMXU1.PhV.phsA.cVal.mag.f",
                                "SUB1.BUSBAR.VA",
                                Iec61850BridgeConfig.TargetSignal.MEASUREMENT),
                        new Iec61850BridgeConfig.DaReadConfig(
                                "FEEDER-1-CURRENT", "RELAY-A1",
                                "LD0/MMXU2.A.phsA.cVal.mag.f",
                                "SUB1.FEED1.IA",
                                Iec61850BridgeConfig.TargetSignal.MEASUREMENT),
                        new Iec61850BridgeConfig.DaReadConfig(
                                "BREAKER-CB1-POS", "RELAY-A1",
                                "LD0/XCBR1.Pos.stVal",
                                "SUB1.CB1.POS",
                                Iec61850BridgeConfig.TargetSignal.STATUS)),
                List.of(new Iec61850BridgeConfig.ReportEventConfig(
                        "PROT-OPS", "RELAY-A1",
                        "LD0/LLN0.RP.urcbProt01",
                        "ALARM",
                        Map.of("PIOC", "CRITICAL", "PTOC", "CRITICAL", "PTUV", "HIGH"))),
                new Iec61850BridgeConfig.AuditConfig(auditFile.toString()),
                null);
    }

    /** §9 S7 + framework S1 — Pure read against a simulated IED. */
    @Test
    void s7_pureReadEmitsMeasurementSignals(@TempDir Path tmp) throws Exception {
        Path auditFile = tmp.resolve("audit.jsonl");
        Iec61850BridgeConfig cfg = baseConfig(tmp, auditFile);
        InMemoryIec61850MmsClient mms = new InMemoryIec61850MmsClient()
                .putMeasurement("RELAY-A1",
                        "LD0/MMXU1.PhV.phsA.cVal.mag.f", 11_002.5,
                        Iec61850MmsClient.Iec61850Quality.GOOD, 1_700_000_000_000L)
                .putMeasurement("RELAY-A1",
                        "LD0/MMXU2.A.phsA.cVal.mag.f", 320.4,
                        Iec61850MmsClient.Iec61850Quality.GOOD, 1_700_000_000_000L)
                .putStatus("RELAY-A1",
                        "LD0/XCBR1.Pos.stVal", true,
                        Iec61850MmsClient.Iec61850Quality.GOOD, 1_700_000_000_000L);
        try (Iec61850AuditOutput audit = new Iec61850AuditOutput(auditFile);
             Iec61850ClientService svc = new Iec61850ClientService(cfg, mms, audit)) {
            svc.start();
            svc.poll();
            Iec61850MeasurementInput input = new Iec61850MeasurementInput(
                    "iec-demo", svc,
                    List.of("BUSBAR-V", "FEEDER-1-CURRENT", "BREAKER-CB1-POS"));
            List<IInputSignal> signals = input.readSignals();
            assertEquals(3, signals.size(),
                    "expected one signal per binding, was: " + signals);
            MeasurementSignal busbar = (MeasurementSignal) signals.get(0);
            assertEquals("SUB1.BUSBAR.VA", busbar.getTag());
            assertEquals(11_002.5, busbar.getMeasurement(), 1e-9);
            assertEquals(Quality.GOOD, busbar.getQuality());
            assertEquals(1_700_000_000_000L, busbar.getTimestamp(),
                    "framework §0 rule 6 — source timestamp wins");

            MeasurementSignal pos = (MeasurementSignal) signals.get(2);
            assertEquals(1.0, pos.getMeasurement(), 1e-9,
                    "CLOSED breaker maps to 1.0 (status as observation, not directive)");
        }
    }

    /** §9 S8 + framework S2 — Quality propagation. */
    @Test
    void s8_qualityPropagation(@TempDir Path tmp) throws Exception {
        Path auditFile = tmp.resolve("audit.jsonl");
        Iec61850BridgeConfig cfg = baseConfig(tmp, auditFile);
        InMemoryIec61850MmsClient mms = new InMemoryIec61850MmsClient()
                .putMeasurement("RELAY-A1",
                        "LD0/MMXU1.PhV.phsA.cVal.mag.f", 11_000.0,
                        Iec61850MmsClient.Iec61850Quality.INVALID, 1_700_000_000_000L);
        try (Iec61850AuditOutput audit = new Iec61850AuditOutput(auditFile);
             Iec61850ClientService svc = new Iec61850ClientService(cfg, mms, audit)) {
            svc.start();
            svc.poll();
            Iec61850MeasurementInput input = new Iec61850MeasurementInput(
                    "iec", svc, List.of("BUSBAR-V"));
            List<IInputSignal> signals = input.readSignals();
            assertEquals(1, signals.size());
            MeasurementSignal s = (MeasurementSignal) signals.get(0);
            assertEquals(Quality.BAD, s.getQuality(),
                    "INVALID q-attribute must propagate as Quality.BAD (framework §0 rule 5)");
            assertEquals(11_000.0, s.getMeasurement(), 1e-9,
                    "value must be passed through unmodified despite bad quality");
        }
    }

    /** §9 S9 — Bridge fails fast at startup when the SCL file is missing. */
    @Test
    void s9_missingSclFailsFastAtStartup(@TempDir Path tmp) throws Exception {
        Path auditFile = tmp.resolve("audit.jsonl");
        Iec61850BridgeConfig cfg = new Iec61850BridgeConfig(
                List.of(new Iec61850BridgeConfig.IedConfig(
                        "RELAY-A1", "10.50.1.10", 102,
                        tmp.resolve("does-not-exist.icd").toString(), null)),
                List.of(), List.of(),
                new Iec61850BridgeConfig.AuditConfig(auditFile.toString()),
                null);
        InMemoryIec61850MmsClient mms = new InMemoryIec61850MmsClient();
        try (Iec61850AuditOutput audit = new Iec61850AuditOutput(auditFile);
             Iec61850ClientService svc = new Iec61850ClientService(cfg, mms, audit)) {
            IOException ex = assertThrows(IOException.class, svc::start);
            assertTrue(ex.getMessage().contains("SCL file not found"),
                    "expected fail-fast on missing SCL, was: " + ex.getMessage());
        }
    }

    /** §9 S10 — Report subscription emits AlarmSignal per report entry. */
    @Test
    void s10_reportSubscriptionEmitsAlarmSignals(@TempDir Path tmp) throws Exception {
        Path auditFile = tmp.resolve("audit.jsonl");
        Iec61850BridgeConfig cfg = baseConfig(tmp, auditFile);
        InMemoryIec61850MmsClient mms = new InMemoryIec61850MmsClient();
        try (Iec61850AuditOutput audit = new Iec61850AuditOutput(auditFile);
             Iec61850ClientService svc = new Iec61850ClientService(cfg, mms, audit)) {
            svc.start();
            // Emit a synthetic protection trip on the subscribed RCB.
            Map<String, String> reasons = new LinkedHashMap<>();
            reasons.put("PIOC", "INST_OVERCURRENT");
            reasons.put("PTOC", "TIMED_OVERCURRENT");
            mms.emitReport("RELAY-A1", "LD0/LLN0.RP.urcbProt01",
                    new Iec61850MmsClient.MmsReport(
                            "LD0/LLN0.RP.urcbProt01",
                            1_700_000_001_000L,
                            InMemoryIec61850MmsClient.entriesFor(reasons)));

            Iec61850EventInput input = new Iec61850EventInput(
                    "iec-events", svc, List.of("PROT-OPS"));
            List<IInputSignal> signals = input.readSignals();
            assertEquals(2, signals.size(),
                    "two report entries must produce two AlarmSignals");
            AlarmSignal a = (AlarmSignal) signals.get(0);
            assertEquals(AlarmPriority.URGENT, a.getPriority(),
                    "PIOC/CRITICAL must escalate to URGENT (closest in-tree priority)");
            assertNotNull(a.getConditionCode());
            assertEquals(1_700_000_001_000L, a.getTimestamp(),
                    "source-system timestamp must win (framework §0 rule 6)");
        }
    }

    /**
     * §9 S11 — Compile-time absence of an aggregator. There is no
     * {@code Iec61850CommandOutputAggregator} class in this package; this
     * test asserts it via {@link Class#forName} so the contract is
     * enforced even if a future contributor tries to add one.
     */
    @Test
    void s11_noWriteSurfaceExists() {
        assertThrows(ClassNotFoundException.class,
                () -> Class.forName(
                        "com.rakovpublic.jneuropallium.worker.bridge.iec61850"
                                + ".Iec61850CommandOutputAggregator"),
                "11-IEC61850.md §7 — no aggregator class may exist in this package");
    }

    /** Framework §5 S4 — MMS not ready: poll cycle is a graceful no-op. */
    @Test
    void s4_mmsNotReadyIsGraceful(@TempDir Path tmp) throws Exception {
        Path auditFile = tmp.resolve("audit.jsonl");
        Iec61850BridgeConfig cfg = baseConfig(tmp, auditFile);
        InMemoryIec61850MmsClient mms = new InMemoryIec61850MmsClient().setReady(false);
        try (Iec61850AuditOutput audit = new Iec61850AuditOutput(auditFile);
             Iec61850ClientService svc = new Iec61850ClientService(cfg, mms, audit)) {
            svc.start();
            svc.poll(); // must not throw
            Iec61850MeasurementInput input = new Iec61850MeasurementInput(
                    "iec", svc, List.of("BUSBAR-V"));
            assertTrue(input.readSignals().isEmpty(),
                    "no signals while MMS is not ready");
        }
    }

    /**
     * Framework §5 S5 — Audit failure isolation. A failing MMS read writes
     * an audit record but does not throw out of {@code poll()}.
     */
    @Test
    void s5_failingReadIsAuditedAndIsolated(@TempDir Path tmp) throws Exception {
        Path auditFile = tmp.resolve("audit.jsonl");
        Iec61850BridgeConfig cfg = baseConfig(tmp, auditFile);
        InMemoryIec61850MmsClient mms = new InMemoryIec61850MmsClient()
                .failNextReadWith(new IOException("association lost"))
                .putMeasurement("RELAY-A1",
                        "LD0/MMXU2.A.phsA.cVal.mag.f", 1.0,
                        Iec61850MmsClient.Iec61850Quality.GOOD, 1L);
        try (Iec61850AuditOutput audit = new Iec61850AuditOutput(auditFile);
             Iec61850ClientService svc = new Iec61850ClientService(cfg, mms, audit)) {
            svc.start();
            svc.poll(); // must not throw
        }
        assertTrue(Files.exists(auditFile),
                "audit JSONL must be created when MMS reads fail");
        String content = Files.readString(auditFile);
        assertTrue(content.contains("\"verdict\":\"FAILED\""),
                "expected FAILED audit record on read failure, was: " + content);
        assertTrue(content.contains("EXCEPTION:IOException"),
                "expected EXCEPTION reason carrying the exception class, was: " + content);
    }

    /** Audit JSONL conformance to 00-FRAMEWORK §4 schema. */
    @Test
    void auditJsonlConformsToFrameworkSchema(@TempDir Path tmp) throws Exception {
        Path auditFile = tmp.resolve("audit.jsonl");
        Iec61850BridgeConfig cfg = baseConfig(tmp, auditFile);
        InMemoryIec61850MmsClient mms = new InMemoryIec61850MmsClient()
                .failNextReadWith(new IOException("simulated"));
        try (Iec61850AuditOutput audit = new Iec61850AuditOutput(auditFile);
             Iec61850ClientService svc = new Iec61850ClientService(cfg, mms, audit)) {
            svc.start();
            svc.poll();
        }
        String line = Files.readString(auditFile).strip();
        assertTrue(line.startsWith("{") && line.endsWith("}"),
                "audit must be one JSON object per line, was: " + line);
        assertTrue(line.contains("\"bridge\":\"iec61850\""),
                "audit must carry bridge identity, was: " + line);
        assertTrue(line.contains("\"verdict\":"), "audit must carry verdict");
        assertFalse(line.contains("\n"), "one record per line");
    }
}
