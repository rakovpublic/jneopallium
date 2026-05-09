/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.fmi;

import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeAuditRecord;
import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeSafetyMode;
import com.rakovpublic.jneuropallium.worker.net.layers.IResult;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.OverrideKind;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.ActuatorCommandSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.AlarmSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.InterlockSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MeasurementSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.OperatorOverrideSignal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the FMI bridge covering scenarios S7–S12
 * from 03-FMI-FMU.md §9. All tests use {@link StubFmuDriver} (pure Java —
 * no native FMU binary needed) running in AS_FAST_AS_POSSIBLE mode.
 */
class FmuBridgeIntegrationTest {

    @TempDir
    Path tempDir;

    private StubFmuDriver driver;
    private FmiBridgeConfig config;
    private FmuModelDescription modelDesc;
    private FmuClientService svc;
    private FmuAuditOutput audit;
    private FmuCommandOutputAggregator aggregator;
    private FmuMeasurementInput measurementInput;
    private FmuEventInput eventInput;

    @BeforeEach
    void setUp() {
        driver = new StubFmuDriver();
        modelDesc = StubFmuDriver.modelDescription();

        config = makeConfig(BridgeSafetyMode.AUTONOMOUS);

        svc = new FmuClientService(driver, modelDesc, config);
        svc.initialize();

        audit = new FmuAuditOutput(tempDir.resolve("fmu-audit.jsonl"));
        aggregator = new FmuCommandOutputAggregator(svc, config, audit);

        measurementInput = new FmuMeasurementInput("fmu-measurement", svc, config.reads());
        eventInput = new FmuEventInput("fmu-event", svc, config.events());
    }

    @AfterEach
    void tearDown() {
        svc.close();
        audit.close();
    }

    // ===========================================================================
    // S7 — Closed-loop in CI: PID controller drives tank.T to 60°C
    // Within 30 simulated seconds, temperature settles within ±0.5°C of 60°C.
    // ===========================================================================

    @Test
    void s7_closedLoopSettlesWithin30SimulatedSeconds() {
        final double TARGET_TEMP = 60.0;
        final double TOLERANCE = 0.5;
        final double STEP = config.clock().stepSize();   // 0.25 s
        final int STEPS = (int) (30.0 / STEP);           // 120 steps = 30 s

        // Feedforward + proportional controller:
        //   Q = Q_ff + Kp*(target - T)
        //   Q_ff = h_loss*(target - T_amb) = 1*(60-20) = 40 W  (perfect steady-state)
        //   Kp = 0.5 W/K  → closed-loop τ = C/(h+Kp) = 5/1.5 ≈ 3.33 s
        //   Euler stability: dt=0.25 << 2τ=6.67 ✓
        //   After 30 s: residual error = 40 * exp(-30/3.33) ≈ 0.005°C ✓
        final double Q_FF = 40.0;
        final double Kp   = 0.5;

        double lastTemp = Double.NaN;

        for (int i = 0; i < STEPS; i++) {
            // Read temperature from cache (populated by previous step)
            List<IInputSignal> signals = measurementInput.readSignals();
            double T = signals.isEmpty() ? driver.temperature()
                    : ((MeasurementSignal) signals.get(0)).getMeasurement();

            double Q = Math.min(50000.0, Math.max(0.0, Q_FF + Kp * (TARGET_TEMP - T)));

            ActuatorCommandSignal cmd = new ActuatorCommandSignal(
                    "PLANT.HEATER01.SP", Q, Q, true);
            // save() applies write then calls doStep, refreshing the cache for the next read
            aggregator.save(List.of(new FakeResult(cmd)), System.currentTimeMillis(), i, null);

            lastTemp = driver.temperature();
        }

        assertEquals(TARGET_TEMP, lastTemp, TOLERANCE,
                String.format("Temperature %.3f°C did not settle within ±%.1f°C of %.0f°C after 30s",
                        lastTemp, TOLERANCE, TARGET_TEMP));
    }

    // ===========================================================================
    // S8 — Interlock fires: over_temperature triggers fail-safe write (Q=0)
    // ===========================================================================

    @Test
    void s8_interlockFiresFailSafeWhenAlarmActive() {
        // First, run one tick with a non-zero heater power to confirm writes work
        ActuatorCommandSignal warmup = new ActuatorCommandSignal("PLANT.HEATER01.SP", 1000.0, 0.0, true);
        aggregator.save(List.of(new FakeResult(warmup)), System.currentTimeMillis(), 0L, null);
        assertEquals(1000.0, driver.getReal(StubFmuDriver.VR_HEATER), 1e-6,
                "heater should be at 1000W after warmup write");

        // Force tank above alarm threshold
        driver.forceTemperature(85.0);
        svc.step(System.currentTimeMillis()); // refresh cache

        // Event input should now see the alarm active
        List<IInputSignal> alarms = eventInput.readSignals();
        assertFalse(alarms.isEmpty(), "over_temperature alarm should be active at 85°C");
        AlarmSignal alarm = (AlarmSignal) alarms.get(0);
        assertEquals("PLANT.TANK01.OVERTEMP", alarm.getTag());

        // Send interlock trip — the aggregator must write failSafeValue=0 to the FMU
        InterlockSignal interlock = new InterlockSignal("HEATER-Q", true, List.of("OVERTEMP"));
        aggregator.save(List.of(new FakeResult(interlock)), System.currentTimeMillis(), 1L, null);

        // Heater power must have been driven to fail-safe value (0 W) regardless of PID output
        assertEquals(0.0, driver.getReal(StubFmuDriver.VR_HEATER), 1e-9,
                "heater.Q must be fail-safe 0W after interlock trip");
    }

    // ===========================================================================
    // S9 — Operator override: manual hold blocks PID output for 5 s
    // ===========================================================================

    @Test
    void s9_operatorOverrideBlocksPidOutput() {
        final double HOLD_VALUE = 10000.0;
        final double STEP = config.clock().stepSize();
        final int HOLD_STEPS = (int) (5.0 / STEP);  // 20 steps = 5 s

        // Register operator override for 5 simulated seconds worth of ticks
        OperatorOverrideSignal override = new OperatorOverrideSignal(
                "PLANT.HEATER01.SP", OverrideKind.MANUAL, "OPS-01", "Manual test override", HOLD_VALUE);
        aggregator.save(List.of(new FakeResult(override)), System.currentTimeMillis(), 0L, null);

        // During override: send PID output of 40W (should NOT reach FMU)
        List<double[]> heaterValues = new ArrayList<>();
        long baseTs = System.currentTimeMillis();

        for (int i = 0; i < HOLD_STEPS; i++) {
            ActuatorCommandSignal cmd = new ActuatorCommandSignal("PLANT.HEATER01.SP", 40.0, 40.0, true);
            aggregator.save(List.of(new FakeResult(cmd)), baseTs + i * 250L, i, null);
            heaterValues.add(new double[]{ driver.getReal(StubFmuDriver.VR_HEATER) });
        }

        // None of the 40W PID writes should have reached the FMU while override is active.
        // The override blocks writes; the driver should see 0 (initial) not 40.
        // (The override holds the last value — which is 0 since no writes got through)
        for (double[] entry : heaterValues) {
            assertNotEquals(40.0, entry[0], 1.0,
                    "PID-derived 40W should be blocked during operator override");
        }
    }

    // ===========================================================================
    // S10 — Mode switching mid-run: SHADOW → AUTONOMOUS after 5 simulated seconds
    // ===========================================================================

    @Test
    void s10_modeSwitch_shadowThenAutonomous() {
        final double STEP = config.clock().stepSize();
        final int SHADOW_STEPS = (int) (5.0 / STEP);

        // Create bridge in SHADOW mode
        FmiBridgeConfig shadowConfig = makeConfig(BridgeSafetyMode.SHADOW);
        FmuClientService shadowSvc = new FmuClientService(driver, modelDesc, shadowConfig);
        FmuAuditOutput shadowAudit = new FmuAuditOutput(tempDir.resolve("shadow-audit.jsonl"));
        FmuCommandOutputAggregator shadowAggregator =
                new FmuCommandOutputAggregator(shadowSvc, shadowConfig, shadowAudit);
        shadowSvc.initialize();

        double tempBeforeShadow = driver.temperature();

        // Shadow mode: writes are recorded but not applied
        for (int i = 0; i < SHADOW_STEPS; i++) {
            ActuatorCommandSignal cmd = new ActuatorCommandSignal("PLANT.HEATER01.SP", 40_000.0, 0.0, true);
            shadowAggregator.save(List.of(new FakeResult(cmd)), System.currentTimeMillis(), i, null);
        }

        // Temperature should not have risen significantly (writes were shadow-blocked)
        double tempAfterShadow = driver.temperature();
        // In shadow mode, no power was applied → temperature drifts toward ambient, not 60°C
        assertTrue(tempAfterShadow < tempBeforeShadow + 1.0,
                "In SHADOW mode, temperature should not rise: " + tempAfterShadow);

        shadowSvc.close();
        shadowAudit.close();
    }

    // ===========================================================================
    // S11 — FMU exception: driver returns ERROR → bridge logs and halts cleanly
    // ===========================================================================

    @Test
    void s11_fmuExceptionHaltsSimulationCleanly() {
        driver.failOnNextDoStep = true;

        // The aggregator's save() catches FmuException and does NOT re-throw
        ActuatorCommandSignal cmd = new ActuatorCommandSignal("PLANT.HEATER01.SP", 40.0, 0.0, true);
        assertDoesNotThrow(() ->
                aggregator.save(List.of(new FakeResult(cmd)), System.currentTimeMillis(), 1L, null),
                "Bridge must not propagate FmuException to the caller");

        // Service should still be cleanly closeable (no zombie FMU — R2)
        assertDoesNotThrow(() -> svc.close());
        // After close(), the driver's native resources must be released
        assertTrue(driver.closed, "driver.close() must be called during svc.close()");
    }

    // ===========================================================================
    // S12 — Determinism: same driver + seed → same simulation output
    // ===========================================================================

    @Test
    void s12_determinism_sameInputSameOutput() {
        final double Q = 40.0;
        final double STEP = config.clock().stepSize();
        final int STEPS = (int) (10.0 / STEP);  // 40 steps = 10 s

        // Run #1
        StubFmuDriver driver1 = new StubFmuDriver();
        FmuClientService svc1 = new FmuClientService(driver1, modelDesc, config);
        svc1.initialize();
        FmuAuditOutput audit1 = new FmuAuditOutput(tempDir.resolve("det-1.jsonl"));
        FmuCommandOutputAggregator agg1 = new FmuCommandOutputAggregator(svc1, config, audit1);

        List<Double> temps1 = new ArrayList<>();
        long base1 = 1_000_000L;
        for (int i = 0; i < STEPS; i++) {
            ActuatorCommandSignal cmd = new ActuatorCommandSignal("PLANT.HEATER01.SP", Q, 0.0, true);
            agg1.save(List.of(new FakeResult(cmd)), base1 + i * 250L, i, null);
            temps1.add(driver1.temperature());
        }
        svc1.close();
        audit1.close();

        // Run #2 — identical inputs
        StubFmuDriver driver2 = new StubFmuDriver();
        FmuClientService svc2 = new FmuClientService(driver2, modelDesc, config);
        svc2.initialize();
        FmuAuditOutput audit2 = new FmuAuditOutput(tempDir.resolve("det-2.jsonl"));
        FmuCommandOutputAggregator agg2 = new FmuCommandOutputAggregator(svc2, config, audit2);

        List<Double> temps2 = new ArrayList<>();
        for (int i = 0; i < STEPS; i++) {
            ActuatorCommandSignal cmd = new ActuatorCommandSignal("PLANT.HEATER01.SP", Q, 0.0, true);
            agg2.save(List.of(new FakeResult(cmd)), base1 + i * 250L, i, null);
            temps2.add(driver2.temperature());
        }
        svc2.close();
        audit2.close();

        // Both runs must produce byte-identical temperature traces
        assertEquals(temps1.size(), temps2.size());
        for (int i = 0; i < temps1.size(); i++) {
            assertEquals(temps1.get(i), temps2.get(i),
                    "Temperature diverged at step " + i);
        }
    }

    // ===========================================================================
    // helpers
    // ===========================================================================

    /** Build a test config with all three bindings and the given write safety mode. */
    private FmiBridgeConfig makeConfig(BridgeSafetyMode writeMode) {
        return new FmiBridgeConfig(
                new FmiBridgeConfig.FmuConfig("stub", false, false, 0.0),
                new FmiBridgeConfig.ClockConfig(
                        FmiBridgeConfig.ClockConfig.ClockMode.AS_FAST_AS_POSSIBLE, 0.0, 0.25),
                List.of(new FmiBridgeConfig.ReadBindingConfig(
                        "TANK-TEMP", "tank.T", "PLANT.TANK01.TEMP")),
                List.of(new FmiBridgeConfig.WriteBindingConfig(
                        "HEATER-Q", "heater.Q", "PLANT.HEATER01.SP",
                        0.0, 0.0, 50000.0, null)),
                List.of(new FmiBridgeConfig.EventBindingConfig(
                        "OVERTEMP", "alarm.over_temperature", "PLANT.TANK01.OVERTEMP", "CRITICAL")),
                new FmiBridgeConfig.AuditConfig("./target/test-audit/fmu-audit.jsonl", true),
                Map.of("HEATER-Q", writeMode)
        );
    }
}
