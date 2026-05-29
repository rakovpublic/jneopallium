/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.demo.industrial;

import com.rakovpublic.jneuropallium.worker.input.opcua.OpcUaAlarmInput;
import com.rakovpublic.jneuropallium.worker.input.opcua.OpcUaMeasurementInput;
import com.rakovpublic.jneuropallium.worker.net.layers.IResult;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.ActuatorNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.CascadeNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.InterlockNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.MeasurementValidatorNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.OscillationIntervention;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.OscillationMonitorNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.PIDNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.Quality;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.SafetyGateNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.SafetyMode;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.ActuatorCommandSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.InterlockSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MeasurementSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.OperatorOverrideSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.SetpointSignal;

import java.util.ArrayList;
import java.util.List;

/**
 * The demo-01 industrial sub-net: validator → outer temperature PID →
 * cascade → inner coolant-flow PID → oscillation monitor → interlock →
 * safety-gate → actuator, exactly as drawn in
 * {@code demo-01-reactor-cascade-control.md}.
 *
 * <p>One {@link #tick} reads the two measurements and the interlock alarm
 * through the real OPC UA input classes, runs the cascade, applies any
 * (reversible) oscillation intervention, and returns the {@link IResult}
 * list for {@code OpcUaCommandOutputAggregator} to turn into safe writes.
 *
 * <p>The controller never writes to the plant itself — every actuator move
 * leaves here as a gated {@link ActuatorCommandSignal} and is committed
 * (or rejected) only by the aggregator.
 */
public final class ReactorCascadeController {

    /* ---- tuning (reverse-acting outer loop: rising temp ⇒ more coolant) ---- */
    private static final double OUTER_KP = -2.0, OUTER_KI = -0.4, OUTER_KD = 0.0;
    private static final double INNER_KP = 0.5, INNER_KI = 2.5, INNER_KD = 0.0;

    private static final long INTERLOCK_NEURON_ID = 90L;
    private static final long ACTUATOR_NEURON_ID = 50L;

    private final MeasurementValidatorNeuron validator = new MeasurementValidatorNeuron();
    private final PIDNeuron outerPid = new PIDNeuron();
    private final CascadeNeuron cascade = new CascadeNeuron();
    private final PIDNeuron innerPid = new PIDNeuron();
    private final OscillationMonitorNeuron monitor = new OscillationMonitorNeuron();
    private final InterlockNeuron interlock = new InterlockNeuron();
    private final SafetyGateNeuron gate = new SafetyGateNeuron();
    private final ActuatorNeuron actuator = new ActuatorNeuron();

    private final String tempTag, flowTag, valveTag, innerSpTag;
    private final double dt;

    private double operatorGainScale = 1.0;   // operator "de-tune" multiplier (proportional gain)
    private double dampFactor = 1.0;           // automatic, reversible damping
    private boolean cascadeBroken = false;
    private OscillationIntervention lastIntervention = OscillationIntervention.NONE;

    // running stats so a test can observe the whole episode, not one sample
    private double maxSeveritySeen = 0.0;
    private boolean interventionFired = false;

    public ReactorCascadeController(String tempTag, String flowTag, String valveTag, String innerSpTag,
                                    double tempSetpoint, String interlockId, double interlockThreshold,
                                    SafetyMode valveMode, double dtSeconds, int acfWindowTicks) {
        this.tempTag = tempTag;
        this.flowTag = flowTag;
        this.valveTag = valveTag;
        this.innerSpTag = innerSpTag;
        this.dt = dtSeconds;

        validator.setRange(tempTag, -50.0, 500.0);
        validator.setRange(flowTag, 0.0, 100.0);

        outerPid.setTag(innerSpTag);
        outerPid.setGains(OUTER_KP, OUTER_KI, OUTER_KD);
        outerPid.setOutputLimits(0.0, 100.0);
        outerPid.setSetpoint(new SetpointSignal(tempTag, tempSetpoint, 0.0, "operator"));

        innerPid.setTag(valveTag);
        innerPid.setGains(INNER_KP, INNER_KI, INNER_KD);
        innerPid.setOutputLimits(0.0, 100.0);

        monitor.setAcfWindowTicks(acfWindowTicks);

        // Hard interlock: reactor over-temperature drives the jacket valve to
        // its fail-safe. interlockId matches the write binding loopId so the
        // aggregator fail-safes the correct node (§2.2.2).
        interlock.addInterlock(interlockId, tempTag, interlockThreshold, true, valveTag, 100.0);
        interlock.seal();

        gate.setDefaultMode(valveMode);
        gate.setModeFor(valveTag, valveMode);
    }

    /* ---- scenario controls ---- */

    public void setTempSetpoint(double sp) {
        outerPid.setSetpoint(new SetpointSignal(tempTag, sp, 0.0, "operator"));
    }

    /** Operator de-tune of the inner loop (>1 destabilises ⇒ limit cycle). */
    public void setOperatorGainScale(double scale) { this.operatorGainScale = scale; }

    public void setValveMode(SafetyMode mode) {
        gate.setDefaultMode(mode);
        gate.setModeFor(valveTag, mode);
    }

    /* ---- one bridge tick ---- */

    public List<IResult> tick(OpcUaMeasurementInput ticIn, OpcUaMeasurementInput ficIn,
                              OpcUaAlarmInput ilkIn, long ts,
                              List<OperatorOverrideSignal> overridesToInject) {
        List<IResult> results = new ArrayList<>();

        MeasurementSignal temp = readMeasurement(ticIn, tempTag);
        MeasurementSignal flow = readMeasurement(ficIn, flowTag);
        if (ilkIn != null) ilkIn.readSignals();   // drain alarm queue (quality propagation)

        if (temp != null) temp = validator.validate(temp);
        if (flow != null) flow = validator.validate(flow);

        // Reversible oscillation intervention is applied from the previous
        // tick's window before this tick's controller moves are computed.
        applyIntervention(monitor.intervention(valveTag));

        // Outer loop → cascade → inner setpoint.
        if (temp != null) {
            ActuatorCommandSignal outerOut = outerPid.step(temp, dt);
            SetpointSignal innerSp = cascade.forward(outerOut, innerSpTag);
            if (innerSp != null) innerPid.setSetpoint(innerSp);
        }

        // Inner loop → valve command.
        ActuatorCommandSignal valveCmd = (flow != null) ? innerPid.step(flow, dt) : null;
        if (valveCmd != null) {
            monitor.observe(new MeasurementSignal(valveTag, valveCmd.getTargetValue(), Quality.GOOD, ts));
            maxSeveritySeen = Math.max(maxSeveritySeen, monitor.severity(valveTag));
        }

        // Interlock has direct authority downstream.
        if (temp != null) {
            for (InterlockSignal il : interlock.evaluate(temp)) {
                results.add(new ResultWrapper(il, INTERLOCK_NEURON_ID));
            }
        }

        // Operator overrides (the aggregator's OverrideRegistry honours these).
        if (overridesToInject != null) {
            for (OperatorOverrideSignal o : overridesToInject) {
                actuator.onOverride(o);
                results.add(new ResultWrapper(o, ACTUATOR_NEURON_ID));
            }
        }

        // Safety gate + actuator, then hand to the aggregator.
        if (valveCmd != null) {
            ActuatorCommandSignal gated = gate.gate(valveCmd);
            actuator.dispatch(gated);
            results.add(new ResultWrapper(gated, ACTUATOR_NEURON_ID));
        }
        return results;
    }

    private void applyIntervention(OscillationIntervention iv) {
        double damp;
        boolean broken;
        switch (iv) {
            case NONE -> { damp = 1.0; broken = false; }
            case SCALE_WEIGHTS -> { damp = 0.5; broken = false; }
            case INJECT_INHIBITION -> { damp = 0.25; broken = false; }
            case BREAK_CONNECTION -> { damp = 0.2; broken = true; }
            case QUARANTINE_NEURON -> { damp = 0.1; broken = true; }
            default -> { damp = 1.0; broken = false; }
        }
        this.dampFactor = damp;
        this.cascadeBroken = broken;
        this.lastIntervention = iv;
        if (iv != OscillationIntervention.NONE) interventionFired = true;
        // Operator de-tune raises the proportional gain (the realistic knob that
        // limit-cycles); automatic damping scales the whole controller back.
        innerPid.setGains(INNER_KP * operatorGainScale * damp, INNER_KI * damp, INNER_KD * damp);
        cascade.setBroken(broken);
    }

    private MeasurementSignal readMeasurement(OpcUaMeasurementInput in, String tag) {
        if (in == null) return null;
        for (IInputSignal s : in.readSignals()) {
            if (s instanceof MeasurementSignal m && tag.equals(m.getTag())) return m;
        }
        return null;
    }

    /* ---- accessors for the runner / acceptance test ---- */

    public OscillationIntervention currentIntervention() { return lastIntervention; }
    public double oscillationSeverity() { return monitor.severity(valveTag); }
    public double dampFactor() { return dampFactor; }
    /** Clears the running oscillation stats so a test can scope one episode. */
    public void resetOscillationStats() { maxSeveritySeen = 0.0; interventionFired = false; }
    public double maxSeveritySeen() { return maxSeveritySeen; }
    public boolean interventionFired() { return interventionFired; }
    public boolean isCascadeBroken() { return cascadeBroken; }
    /** True when no automatic intervention is active — proves reversibility. */
    public boolean gainsRestored() { return dampFactor == 1.0 && !cascadeBroken; }
    public ActuatorNeuron actuator() { return actuator; }
}
