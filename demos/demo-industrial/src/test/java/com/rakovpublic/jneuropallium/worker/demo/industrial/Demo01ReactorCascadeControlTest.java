/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.demo.industrial;

import com.google.gson.Gson;
import com.rakovpublic.jneuropallium.worker.demo.industrial.Demo01ReactorCascadeControl.Harness;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.OscillationIntervention;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.OverrideKind;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.SafetyMode;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.OperatorOverrideSignal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end acceptance test for {@code demo-01-reactor-cascade-control.md}.
 *
 * <p>Drives the full closed loop (plant → OPC UA bridge → industrial neuron
 * pipeline → aggregator → audit) and asserts every bullet from the demo's
 * "Acceptance" section. The OPC UA transport is the in-process
 * {@link SimulatedReactorOpcUaService}; everything else is production code.
 */
class Demo01ReactorCascadeControlTest {

    private static final Gson GSON = new Gson();
    private static final double RAMP_PER_TICK = Demo01Config.RAMP_RATE_MAX_PER_SEC * 0.1; // %/tick

    /* ---------- SHADOW: every proposed write rejected, valve frozen ---------- */
    @Test
    void shadow_rejectsEveryWrite_andValveNeverChanges(@TempDir Path tmp) {
        Harness h = new Harness(tmp.resolve("audit.jsonl").toString(), SafetyMode.SHADOW, 1L);
        double valveBefore = h.svc.plant().getValve();
        h.run(80, null);

        assertEquals(0, h.svc.valveWriteCount(), "no write may reach the plant in SHADOW");
        assertEquals(valveBefore, h.svc.plant().getValve(), 1e-9, "valve node must not change");

        List<Map<String, Object>> recs = audit(h);
        List<Map<String, Object>> valveRecs = forTag(recs, Demo01Config.VALVE_TAG);
        assertFalse(valveRecs.isEmpty(), "the controller must still propose moves in SHADOW");
        for (Map<String, Object> r : valveRecs) {
            assertEquals("REJECTED", r.get("verdict"));
            assertEquals("SHADOW_MODE", r.get("reason"));
        }
        h.close();
    }

    /* ---------- AUTONOMOUS: tracks setpoint, clamped, rate-limited ---------- */
    @Test
    void autonomous_tracksSetpoint_writesClampedAndRateLimited(@TempDir Path tmp) {
        Harness h = new Harness(tmp.resolve("audit.jsonl").toString(), SafetyMode.AUTONOMOUS, 2L);
        h.run(1500, null);

        double temp = h.svc.plant().getReactorTempPV();
        assertEquals(Demo01Config.TEMP_SETPOINT, temp, 5.0,
                "reactor temperature should track its setpoint within band, was " + temp);
        assertTrue(h.svc.valveWriteCount() > 0, "AUTONOMOUS must actually write the valve");

        List<Map<String, Object>> applied = byVerdict(forTag(audit(h), Demo01Config.VALVE_TAG), "APPLIED");
        assertFalse(applied.isEmpty());
        Double prev = null;
        for (Map<String, Object> r : applied) {
            double eff = num(r.get("effective"));
            assertTrue(eff >= 0.0 && eff <= 100.0, "effective valve out of clamp: " + eff);
            if (prev != null) {
                assertTrue(Math.abs(eff - prev) <= RAMP_PER_TICK + 1e-6,
                        "valve step " + Math.abs(eff - prev) + " exceeds rate limit " + RAMP_PER_TICK);
            }
            prev = eff;
        }
        h.close();
    }

    /* ---------- Interlock: fail-safe write within one tick, beats PID ---------- */
    @Test
    void interlock_writesFailSafeWithinOneTick_beatingPidCommand(@TempDir Path tmp) {
        Harness h = new Harness(tmp.resolve("audit.jsonl").toString(), SafetyMode.AUTONOMOUS, 3L);
        h.run(300, null);                       // settle so the PID is actively commanding
        int before = h.svc.valveWriteCount();

        h.svc.plant().pinReactorTemp(115.0);    // force HI_TEMP over the 110 °C interlock
        h.run(1, null);                          // exactly one tick

        assertEquals(Demo01Config.FAIL_SAFE_VALVE, h.svc.plant().getValve(), 1e-9,
                "valve must be driven to fail-safe within one tick");
        assertTrue(h.svc.valveWriteCount() > before, "interlock must issue a write");

        List<Map<String, Object>> tail = audit(h);
        assertTrue(tail.stream().anyMatch(r ->
                        "INTERLOCK_TRIP".equals(r.get("verdict"))
                                && Demo01Config.VALVE_TAG.equals(r.get("tag"))
                                && num(r.get("effective")) == Demo01Config.FAIL_SAFE_VALVE),
                "expected an INTERLOCK_TRIP write of the fail-safe value");
        assertTrue(tail.stream().anyMatch(r ->
                        "REJECTED".equals(r.get("verdict"))
                                && "INTERLOCK_HOLD".equals(r.get("reason"))
                                && Demo01Config.VALVE_TAG.equals(r.get("tag"))),
                "the PID command in the same tick must be held by INTERLOCK_HOLD");
        h.close();
    }

    /* ---------- Operator override holds the tag, OVERRIDE_HOLD ---------- */
    @Test
    void operatorOverride_holdsTag_andEmitsOverrideHold(@TempDir Path tmp) {
        Harness h = new Harness(tmp.resolve("audit.jsonl").toString(), SafetyMode.AUTONOMOUS, 4L);
        h.run(300, null);

        var override = new OperatorOverrideSignal(Demo01Config.VALVE_TAG, OverrideKind.MANUAL,
                "op-7", "manual hold", 40.0);
        int writesBefore = h.svc.valveWriteCount();
        h.run(1, List.of(override));            // record the override
        h.run(20, null);                         // ... and hold across subsequent ticks

        assertEquals(writesBefore, h.svc.valveWriteCount(),
                "no neuron-derived write may reach the tag while override is active");
        long holds = audit(h).stream()
                .filter(r -> "OVERRIDE_HOLD".equals(r.get("verdict"))
                        && Demo01Config.VALVE_TAG.equals(r.get("tag")))
                .count();
        assertTrue(holds >= 20, "expected sustained OVERRIDE_HOLD audit lines, got " + holds);
        h.close();
    }

    /* ---------- Oscillation detected, damped, and released automatically ---------- */
    @Test
    void oscillation_detectedDampedThenReleased_noPermanentReconfig(@TempDir Path tmp) {
        Harness h = new Harness(tmp.resolve("audit.jsonl").toString(), SafetyMode.AUTONOMOUS, 5L);
        h.run(300, null);                        // stable baseline
        assertEquals(OscillationIntervention.NONE, h.controller.currentIntervention());

        h.controller.setOperatorGainScale(8.0); // de-tune ⇒ limit cycle
        h.controller.resetOscillationStats();
        h.run(120, null);
        assertTrue(h.controller.maxSeveritySeen() > 0.30,
                "limit cycle should raise ACF severity, peak was " + h.controller.maxSeveritySeen());
        assertTrue(h.controller.interventionFired(),
                "an intervention band should fire while oscillating");

        h.controller.setOperatorGainScale(1.0); // re-tune; intervention must release on its own
        h.run(150, null);
        assertEquals(OscillationIntervention.NONE, h.controller.currentIntervention(),
                "intervention must release once ACF recovers");
        assertTrue(h.controller.gainsRestored(),
                "gains/cascade must be restored — no permanent reconfiguration");
        assertTrue(h.controller.oscillationSeverity() < 0.30);
        h.close();
    }

    /* ---------- Audit shape: 00-FRAMEWORK §4 fields on every line ---------- */
    @Test
    void audit_everyLineHasFrameworkShape(@TempDir Path tmp) {
        Harness h = new Harness(tmp.resolve("audit.jsonl").toString(), SafetyMode.AUTONOMOUS, 6L);
        h.run(200, null);
        List<Map<String, Object>> recs = audit(h);
        assertFalse(recs.isEmpty());
        for (Map<String, Object> r : recs) {
            for (String k : List.of("ts", "run", "verdict", "loopId", "tag",
                    "proposed", "effective", "reason", "safetyMode")) {
                assertTrue(r.containsKey(k), "audit line missing key '" + k + "': " + r);
            }
            assertNotNull(r.get("ts"));
            assertNotNull(r.get("verdict"));
        }
        h.close();
    }

    /* ===================== helpers ===================== */

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> audit(Harness h) {
        return h.audit.readAllLines().stream()
                .filter(l -> !l.isBlank())
                .map(l -> (Map<String, Object>) GSON.fromJson(l, Map.class))
                .toList();
    }

    private static List<Map<String, Object>> forTag(List<Map<String, Object>> recs, String tag) {
        return recs.stream().filter(r -> tag.equals(r.get("tag"))).toList();
    }

    private static List<Map<String, Object>> byVerdict(List<Map<String, Object>> recs, String verdict) {
        return recs.stream().filter(r -> verdict.equals(r.get("verdict"))).toList();
    }

    private static double num(Object o) {
        return o == null ? Double.NaN : ((Number) o).doubleValue();
    }
}
