/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.ActuatorCommandSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MeasurementSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.SetpointSignal;

/**
 * Layer 1 PID controller. Velocity-form update with anti-windup via
 * output clamping. Three gains (Kp, Ki, Kd) can be scaled at runtime
 * to implement the {@link OscillationIntervention#SCALE_WEIGHTS}
 * intervention. Loop=1 / Epoch=1.
 */
public class PIDNeuron extends ModulatableNeuron implements IPIDNeuron {

    private double kp, ki, kd;
    private double setpoint;
    private double outMin = -Double.MAX_VALUE, outMax = Double.MAX_VALUE;
    private double integrator;
    private double lastError;
    private double lastOutput;
    private boolean hasPrev;
    private String tag;

    public PIDNeuron() { super(); }
    public PIDNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    public void setTag(String tag) { this.tag = tag; }
    public String getTag() { return tag; }

    @Override public void setGains(double kp, double ki, double kd) { this.kp = kp; this.ki = ki; this.kd = kd; }
    @Override public double getKp() { return kp; }
    @Override public double getKi() { return ki; }
    @Override public double getKd() { return kd; }

    @Override public void scaleGains(double factor) {
        kp *= factor;
        ki *= factor;
        kd *= factor;
    }

    @Override public void setSetpoint(SetpointSignal s) { if (s != null) this.setpoint = s.getSetpoint(); }
    @Override public double getSetpoint() { return setpoint; }
    @Override public void setOutputLimits(double min, double max) {
        this.outMin = Math.min(min, max);
        this.outMax = Math.max(min, max);
    }

    @Override
    public ActuatorCommandSignal step(MeasurementSignal m, double dtSeconds) {
        if (m == null || dtSeconds <= 0) return null;
        double err = setpoint - m.getMeasurement();
        integrator += ki * err * dtSeconds;
        if (integrator > outMax) integrator = outMax;
        if (integrator < outMin) integrator = outMin;
        double derivative = hasPrev ? (err - lastError) / dtSeconds : 0.0;
        double output = kp * err + integrator + kd * derivative;
        if (output > outMax) output = outMax;
        if (output < outMin) output = outMin;
        lastError = err;
        lastOutput = output;
        hasPrev = true;
        return new ActuatorCommandSignal(tag, output, m.getMeasurement(), true);
    }

    @Override public void reset() {
        integrator = 0;
        lastError = 0;
        lastOutput = 0;
        hasPrev = false;
    }

    @Override public double getLastError() { return lastError; }
    @Override public double getLastOutput() { return lastOutput; }
}
