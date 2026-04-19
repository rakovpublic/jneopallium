/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.bci.CalibrationSignal;

/**
 * Layer 7 calibration scheduler. Decides when to request decoder recalibration
 * given drift, decode error, time since last session, and user state.
 * Emits a {@link CalibrationSignal} with the chosen target.
 * Loop=2 / Epoch=3.
 */
public class CalibrationSchedulerNeuron extends ModulatableNeuron {

    private long lastCalibrationTick = 0L;
    private long minIntervalTicks = 24 * 60 * 60 * 1000L;  // ≈ 1 day @ 1 kHz
    private long maxIntervalTicks = 7L * 24 * 60 * 60 * 1000L;  // ≈ 1 week
    private double driftTrigger = 0.25;
    private double errorTrigger = 0.30;
    private long sessionCounter;

    public CalibrationSchedulerNeuron() { super(); }
    public CalibrationSchedulerNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    /**
     * Evaluate whether to request calibration now. Returns a
     * {@link CalibrationSignal} if triggered, or {@code null} if not.
     */
    public CalibrationSignal evaluate(long currentTick, double drift, double decodeError, boolean userAlert) {
        long since = currentTick - lastCalibrationTick;
        boolean mustCalibrate = since >= maxIntervalTicks;
        boolean shouldCalibrate = since >= minIntervalTicks
                && (drift >= driftTrigger || decodeError >= errorTrigger);
        if (!mustCalibrate && !shouldCalibrate) return null;
        if (!mustCalibrate && !userAlert) return null;

        CalibrationTarget target;
        if (drift >= driftTrigger && decodeError >= errorTrigger) target = CalibrationTarget.FULL_RECALIBRATION;
        else if (drift >= driftTrigger) target = CalibrationTarget.CHANNEL_SELECTION;
        else if (decodeError >= errorTrigger) target = CalibrationTarget.DECODER_WEIGHTS;
        else target = CalibrationTarget.FEEDBACK_INTENSITY;

        sessionCounter++;
        lastCalibrationTick = currentTick;
        double performance = Math.max(0, Math.min(1, 1.0 - 0.5 * (drift + decodeError)));
        return new CalibrationSignal("session-" + sessionCounter, target, performance);
    }

    public void setMinIntervalTicks(long t) { this.minIntervalTicks = Math.max(0, t); }
    public void setMaxIntervalTicks(long t) { this.maxIntervalTicks = Math.max(0, t); }
    public void setDriftTrigger(double t) { this.driftTrigger = t; }
    public void setErrorTrigger(double t) { this.errorTrigger = t; }
    public long getLastCalibrationTick() { return lastCalibrationTick; }
    public long getSessionCount() { return sessionCounter; }
}
