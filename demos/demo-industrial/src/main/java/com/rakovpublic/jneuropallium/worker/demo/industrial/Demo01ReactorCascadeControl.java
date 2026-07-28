/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.demo.industrial;

import com.rakovpublic.jneuropallium.worker.input.opcua.OpcUaAlarmInput;
import com.rakovpublic.jneuropallium.worker.input.opcua.OpcUaMeasurementInput;
import com.rakovpublic.jneuropallium.worker.net.layers.IResult;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.OverrideKind;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.SafetyMode;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.opcua.OpcUaBridgeConfig;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.opcua.OpcUaNodeBinding;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.OperatorOverrideSignal;
import com.rakovpublic.jneuropallium.worker.output.opcua.OpcUaCommandOutputAggregator;
import com.rakovpublic.jneuropallium.worker.output.opcua.OpcUaTransparencyLogOutput;

import java.util.List;

/**
 * Runnable narrative of demo-01 — the reactor jacket-temperature cascade
 * control demo from {@code demo-01-reactor-cascade-control.md}.
 *
 * <p>Runs the full closed loop in-process against {@link ReactorPlantSimulator}
 * through the real OPC UA bridge classes, walking the documented run
 * procedure: SHADOW dry-run → AUTONOMOUS promotion → interlock trip →
 * operator override → oscillation damping. Every decision is written to the
 * audit JSONL ({@code argv[0]}, default {@code /tmp/jneopallium-demo01-audit.jsonl}).
 *
 * <p>For a real over-the-wire run, point a plain {@code MiloOpcUaClientService}
 * at the {@code asyncua} plant in {@code src/test/python/reactor} instead of
 * the simulated service — the rest is identical.
 *
 * <pre>{@code  mvn -q -pl worker exec:java \
 *      -Dexec.mainClass=com.rakovpublic.jneuropallium.worker.demo.industrial.Demo01ReactorCascadeControl }</pre>
 */
public final class Demo01ReactorCascadeControl {

    private static final double DT = 0.1;            // seconds per tick
    private static final long TICK_MS = 100L;

    public static void main(String[] args) {
        String auditFile = args.length > 0 ? args[0] : "/tmp/jneopallium-demo01-audit.jsonl";
        long run = System.currentTimeMillis();
        Harness h = new Harness(auditFile, SafetyMode.SHADOW, run);

        System.out.println("== demo-01 reactor cascade control ==");
        System.out.println("audit: " + auditFile + "\n");

        // 3. Run in SHADOW — the controller computes valve moves but writes nothing.
        System.out.println("[1] SHADOW dry-run (50 ticks) — every write REJECTED, valve frozen");
        h.run(50, null);
        System.out.printf("    valve writes to plant=%d  T=%.1f°C  valve=%.1f%%%n%n",
                h.svc.valveWriteCount(), h.svc.plant().getReactorTempPV(), h.svc.plant().getValve());

        // 4. Promote to AUTONOMOUS (controlled restart — config is not hot-reloaded).
        System.out.println("[2] Promote FIC-101 → AUTONOMOUS (restart) and settle 1200 ticks");
        h = h.restart(SafetyMode.AUTONOMOUS);
        h.run(1200, null);
        System.out.printf("    settled T=%.1f°C (setpoint %.0f)  valve=%.1f%%  writes=%d%n%n",
                h.svc.plant().getReactorTempPV(), Demo01Config.TEMP_SETPOINT,
                h.svc.plant().getValve(), h.svc.valveWriteCount());

        // 5. Trip the interlock.
        System.out.println("[3] Force HI_TEMP interlock — fail-safe valve=100% within one tick");
        h.svc.plant().pinReactorTemp(115.0);
        h.run(1, null);
        System.out.printf("    valve after trip=%.1f%% (fail-safe %.0f%%)%n%n",
                h.svc.plant().getValve(), Demo01Config.FAIL_SAFE_VALVE);
        h.svc.plant().pinReactorTemp(null);

        // 6. Operator override.
        System.out.println("[4] Operator MANUAL override @40% — tag held (OVERRIDE_HOLD)");
        var override = new OperatorOverrideSignal(Demo01Config.VALVE_TAG, OverrideKind.MANUAL,
                "op-7", "manual hold for sampling", 40.0);
        h.svc.plant().setValve(40.0);
        h.run(1, List.of(override));
        h.run(10, null);
        System.out.printf("    valve held=%.1f%%%n%n", h.svc.plant().getValve());

        // 7. Oscillation: de-tune the inner loop, watch the net damp it, then release.
        System.out.println("[5] De-tune inner PID (gain ×8) — induce limit cycle");
        h.controller.setOperatorGainScale(8.0);
        h.controller.resetOscillationStats();
        h.run(120, null);
        System.out.printf("    peak severity=%.2f  interventionFired=%b%n",
                h.controller.maxSeveritySeen(), h.controller.interventionFired());
        System.out.println("    re-tune to nominal — intervention releases automatically");
        h.controller.setOperatorGainScale(1.0);
        h.run(150, null);
        System.out.printf("    severity=%.2f  intervention=%s  gainsRestored=%b%n%n",
                h.controller.oscillationSeverity(), h.controller.currentIntervention(), h.controller.gainsRestored());

        List<String> lines = h.audit.readAllLines();
        System.out.println("== done: " + lines.size() + " audit records at " + auditFile + " ==");
        h.close();
    }

    /** Wires the bridge + controller for one config "generation". */
    static final class Harness {
        final OpcUaBridgeConfig cfg;
        final SimulatedReactorOpcUaService svc;
        final ReactorCascadeController controller;
        final OpcUaTransparencyLogOutput audit;
        final OpcUaCommandOutputAggregator agg;
        final OpcUaMeasurementInput ticIn, ficIn;
        final OpcUaAlarmInput ilkIn;
        final String auditFile;
        final long run;
        long ts = 1_740_000_000_000L;

        Harness(String auditFile, SafetyMode mode, long run) {
            this(auditFile, mode, run, new ReactorPlantSimulator());
        }

        Harness(String auditFile, SafetyMode mode, long run, ReactorPlantSimulator plant) {
            this.auditFile = auditFile;
            this.run = run;
            this.cfg = Demo01Config.build(auditFile, mode);
            this.svc = new SimulatedReactorOpcUaService(cfg, plant,
                    Demo01Config.TEMP_TAG, Demo01Config.FLOW_TAG, Demo01Config.VALVE_TAG);
            this.controller = new ReactorCascadeController(
                    Demo01Config.TEMP_TAG, Demo01Config.FLOW_TAG, Demo01Config.VALVE_TAG,
                    Demo01Config.INNER_SP_TAG, Demo01Config.TEMP_SETPOINT,
                    Demo01Config.VALVE_LOOP_ID, Demo01Config.INTERLOCK_THRESHOLD,
                    mode, DT, 24);
            this.audit = new OpcUaTransparencyLogOutput(cfg.audit(), null);
            this.agg = new OpcUaCommandOutputAggregator(cfg, svc, audit);
            this.ticIn = new OpcUaMeasurementInput("opc-tic", null, svc, readBinding(Demo01Config.TEMP_TAG));
            this.ficIn = new OpcUaMeasurementInput("opc-fic", null, svc, readBinding(Demo01Config.FLOW_TAG));
            this.ilkIn = new OpcUaAlarmInput("opc-ilk", null, svc);
        }

        private List<OpcUaNodeBinding> readBinding(String signalTag) {
            for (OpcUaBridgeConfig.NodeBindingConfig c : cfg.reads()) {
                if (c.signalTag().equals(signalTag)) return List.of(new OpcUaNodeBinding(c));
            }
            return List.of();
        }

        void run(int ticks, List<OperatorOverrideSignal> overrides) {
            for (int i = 0; i < ticks; i++) {
                svc.advance(DT);
                ts += TICK_MS;
                List<IResult> results = controller.tick(ticIn, ficIn, ilkIn, ts,
                        i == 0 ? overrides : null);
                agg.save(results, ts, run, null);
            }
        }

        /** Documented controlled restart: a brand-new config generation, same plant. */
        Harness restart(SafetyMode mode) {
            audit.close();
            Harness next = new Harness(auditFile, mode, run, svc.plant());
            next.ts = this.ts;
            return next;
        }

        void close() { audit.close(); }
    }
}
