/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.output.opcua;

import com.rakovpublic.jneuropallium.worker.net.layers.IResult;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.OverrideKind;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.SafetyMode;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.opcua.MiloOpcUaClientService;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.opcua.OpcUaBridgeConfig;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.opcua.OpcUaNodeBinding;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.ActuatorCommandSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.InterlockSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.OperatorOverrideSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.SetpointSignal;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exercises the §0 ground rules in isolation: the OPC UA client is a
 * fake that records writes; no embedded server is required.
 *
 * <p>Covers: S3 (SHADOW reject), S4 (AUTONOMOUS apply), S5 (rate limit),
 * S6 (interlock priority), S7 (operator override), S9 (clamp),
 * S10 (unknown tag).
 */
class OpcUaCommandOutputAggregatorTest {

    private FakeMiloService svc;
    private OpcUaTransparencyLogOutput audit;
    private OpcUaCommandOutputAggregator agg;
    private Path auditFile;

    @BeforeEach
    void setUp(@TempDir Path tmp) {
        auditFile = tmp.resolve("audit.jsonl");
        svc = new FakeMiloService();
        OpcUaBridgeConfig cfg = baseConfig(auditFile);
        audit = new OpcUaTransparencyLogOutput(cfg.audit(), null);
        agg = new OpcUaCommandOutputAggregator(cfg, svc, audit);
    }

    @AfterEach
    void tearDown() {
        if (audit != null) audit.close();
    }

    private static OpcUaBridgeConfig baseConfig(Path auditFile) {
        OpcUaBridgeConfig.NodeBindingConfig fic = new OpcUaBridgeConfig.NodeBindingConfig(
                "FIC-101", "ns=2;s=Plant.TargetMotorSpeed", "PLANT.FIC101.SP",
                OpcUaBridgeConfig.NodeBindingConfig.Direction.WRITE,
                0.0, 5.0, 0.0, 100.0);
        OpcUaBridgeConfig.NodeBindingConfig tic = new OpcUaBridgeConfig.NodeBindingConfig(
                "TIC-101", "ns=2;s=Plant.TargetTemp", "PLANT.TIC101.SP",
                OpcUaBridgeConfig.NodeBindingConfig.Direction.WRITE,
                null, null, null, null);
        return new OpcUaBridgeConfig(
                new OpcUaBridgeConfig.ConnectionConfig("opc.tcp://x", null, null,
                        Duration.ofSeconds(1), Duration.ofSeconds(60), 3),
                new OpcUaBridgeConfig.SecurityConfig(
                        OpcUaBridgeConfig.SecurityConfig.SecurityPolicy.NONE,
                        OpcUaBridgeConfig.SecurityConfig.MessageSecurityMode.NONE,
                        null, null, null,
                        new OpcUaBridgeConfig.SecurityConfig.Anonymous()),
                List.of(),
                List.of(fic, tic),
                List.of(),
                new OpcUaBridgeConfig.AuditConfig(auditFile.toString(), null, true),
                Map.of("FIC-101", SafetyMode.AUTONOMOUS, "TIC-101", SafetyMode.SHADOW),
                Duration.ofMillis(250));
    }

    /* ---------- S3: SHADOW mode rejects ---------- */
    @Test
    void shadowModeRejectsSetpoint() {
        SetpointSignal sp = new SetpointSignal("PLANT.TIC101.SP", 50.0, 0.0, "test");
        agg.save(List.of(new FakeResult(sp)), 1_000L, 1, null);
        audit.flush();
        assertEquals(0, svc.writes.size(), "SHADOW must not produce a field write");
        assertContains(auditFile(), "\"verdict\":\"REJECTED\"");
        assertContains(auditFile(), "SHADOW_MODE");
    }

    /* ---------- S4: AUTONOMOUS applies ---------- */
    @Test
    void autonomousAppliesSetpointWithinBounds() {
        SetpointSignal sp = new SetpointSignal("PLANT.FIC101.SP", 50.0, 0.0, "test");
        agg.save(List.of(new FakeResult(sp)), 1_000L, 1, null);
        audit.flush();
        assertEquals(1, svc.writes.size());
        assertEquals(50.0, svc.lastDouble(), 1e-9);
        assertContains(auditFile(), "\"verdict\":\"APPLIED\"");
    }

    /* ---------- S5: rate limit ---------- */
    @Test
    void rateLimiterClampsBigStep() {
        SetpointSignal first = new SetpointSignal("PLANT.FIC101.SP", 30.0, 0.0, "test");
        agg.save(List.of(new FakeResult(first)), 1_000L, 1, null);
        SetpointSignal second = new SetpointSignal("PLANT.FIC101.SP", 50.0, 0.0, "test");
        agg.save(List.of(new FakeResult(second)), 1_500L, 2, null);
        audit.flush();
        assertEquals(2, svc.writes.size());
        // dt=0.5s, ramp=5/s -> max step = 2.5; 30 -> 32.5
        assertEquals(32.5, svc.lastDouble(), 1e-6);
        assertContains(auditFile(), "RATE_LIMITED");
    }

    /* ---------- S6: interlock priority ---------- */
    @Test
    void interlockTripWritesFailSafeAndDropsConcurrentSetpoint() {
        SetpointSignal sp = new SetpointSignal("PLANT.FIC101.SP", 80.0, 0.0, "test");
        InterlockSignal il = new InterlockSignal("FIC-101", true, List.of("HIGH_PRESSURE"));
        agg.save(List.of(new FakeResult(il), new FakeResult(sp)), 1_000L, 1, null);
        audit.flush();
        assertEquals(1, svc.writes.size(), "Only the fail-safe write should reach the field");
        assertEquals(0.0, svc.lastDouble(), 1e-9);
        assertContains(auditFile(), "\"verdict\":\"INTERLOCK_TRIP\"");
        assertContains(auditFile(), "INTERLOCK_HOLD");
        // After the trip the loop is held. The concurrent setpoint must not write.
    }

    /* ---------- S7: operator override holds ---------- */
    @Test
    void operatorOverrideSuppressesSetpoint() {
        OperatorOverrideSignal ov = new OperatorOverrideSignal(
                "PLANT.FIC101.SP", OverrideKind.MANUAL, "op1", "manual", 47.0);
        SetpointSignal sp = new SetpointSignal("PLANT.FIC101.SP", 80.0, 0.0, "test");
        agg.save(List.of(new FakeResult(ov), new FakeResult(sp)), 1_000L, 1, null);
        audit.flush();
        assertEquals(0, svc.writes.size(), "Override holds — no field write");
        assertContains(auditFile(), "\"verdict\":\"OVERRIDE_HOLD\"");
    }

    /* ---------- S9: clamp ---------- */
    @Test
    void clampCapsAtMax() {
        SetpointSignal sp = new SetpointSignal("PLANT.FIC101.SP", 150.0, 0.0, "test");
        agg.save(List.of(new FakeResult(sp)), 1_000L, 1, null);
        audit.flush();
        assertEquals(100.0, svc.lastDouble(), 1e-9);
        assertContains(auditFile(), "CLAMPED_HIGH");
    }

    @Test
    void clampCapsAtMin() {
        SetpointSignal sp = new SetpointSignal("PLANT.FIC101.SP", -5.0, 0.0, "test");
        agg.save(List.of(new FakeResult(sp)), 1_000L, 1, null);
        audit.flush();
        assertEquals(0.0, svc.lastDouble(), 1e-9);
        assertContains(auditFile(), "CLAMPED_LOW");
    }

    /* ---------- S10: unknown tag ---------- */
    @Test
    void unknownTagIsRejected() {
        SetpointSignal sp = new SetpointSignal("NOT_CONFIGURED", 5.0, 0.0, "test");
        agg.save(List.of(new FakeResult(sp)), 1_000L, 1, null);
        audit.flush();
        assertEquals(0, svc.writes.size());
        assertContains(auditFile(), "UNKNOWN_TAG");
    }

    @Test
    void shadowOnlyActuatorCommandIsRejected() {
        ActuatorCommandSignal a = new ActuatorCommandSignal("PLANT.TIC101.SP", 10.0, 0.0, true);
        agg.save(List.of(new FakeResult(a)), 1_000L, 1, null);
        audit.flush();
        assertEquals(0, svc.writes.size());
        assertContains(auditFile(), "SHADOW_MODE");
    }

    @Test
    void diffSuppressionSkipsSecondWriteWithinWindow() {
        SetpointSignal sp = new SetpointSignal("PLANT.FIC101.SP", 50.0, 0.0, "test");
        agg.save(List.of(new FakeResult(sp)), 1_000L, 1, null);
        agg.save(List.of(new FakeResult(sp)), 1_500L, 2, null);
        audit.flush();
        assertEquals(1, svc.writes.size(), "Repeat write within 5s window must be diff-suppressed");
        assertContains(auditFile(), "DIFF_SUPPRESSED");
    }

    /* ============== helpers ============== */

    private String auditFile() {
        try { return java.nio.file.Files.readString(auditFile); }
        catch (java.io.IOException e) { return ""; }
    }

    private static void assertContains(String haystack, String needle) {
        assertTrue(haystack.contains(needle),
                "expected audit log to contain `" + needle + "`, got:\n" + haystack);
    }

    /* In-memory MiloOpcUaClientService stand-in. */
    static final class FakeMiloService extends MiloOpcUaClientService {
        final java.util.List<Map.Entry<NodeId, DataValue>> writes = new java.util.ArrayList<>();
        final ConcurrentHashMap<String, OpcUaNodeBinding> bindings = new ConcurrentHashMap<>();

        FakeMiloService() { super(null, null, null); }

        @Override
        public StatusCode writeValue(NodeId nodeId, DataValue value) {
            writes.add(Map.entry(nodeId, value));
            return StatusCode.GOOD;
        }

        @Override
        public OpcUaNodeBinding bindingBySignalTag(String tag) {
            return bindings.get(tag);
        }

        double lastDouble() {
            DataValue dv = writes.get(writes.size() - 1).getValue();
            Object v = dv.getValue().getValue();
            return ((Number) v).doubleValue();
        }
    }
}
