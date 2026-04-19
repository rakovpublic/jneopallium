/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.tutoring;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.tutoring.InterventionSignal;

/**
 * Layer 2 fatigue detector. Tracks session duration and error-rate drift,
 * emitting {@link InterventionSignal#type} = BREAK when saturated.
 * Loop=2 / Epoch=3.
 */
public class FatigueNeuron extends ModulatableNeuron {

    private long sessionStartTick;
    private long currentTick;
    private int consecutiveErrors;
    private double baselineErrorRate = 0.2;
    private double currentErrorRate = 0.0;
    private int maxSessionTicks = 2700;
    private int maxConsecutiveErrors = 5;

    public FatigueNeuron() { super(); }
    public FatigueNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    public void startSession(long tick) {
        this.sessionStartTick = tick;
        this.currentTick = tick;
        this.consecutiveErrors = 0;
    }

    public InterventionSignal tick(long tick, boolean lastAnswerCorrect, double errorRate) {
        this.currentTick = tick;
        this.currentErrorRate = errorRate;
        if (lastAnswerCorrect) consecutiveErrors = 0; else consecutiveErrors++;

        boolean durationExceeded = (tick - sessionStartTick) >= maxSessionTicks;
        boolean driftDetected = errorRate > baselineErrorRate * 1.5
                && consecutiveErrors >= maxConsecutiveErrors;

        if (durationExceeded || driftDetected) {
            InterventionSignal s = new InterventionSignal(InterventionType.BREAK,
                    durationExceeded ? "max-session-duration" : "error-rate-drift");
            s.setSourceNeuronId(this.getId());
            return s;
        }
        return null;
    }

    public long getSessionStartTick() { return sessionStartTick; }
    public long getCurrentTick() { return currentTick; }
    public int getConsecutiveErrors() { return consecutiveErrors; }
    public double getCurrentErrorRate() { return currentErrorRate; }

    public void setBaselineErrorRate(double r) { this.baselineErrorRate = r; }
    public void setMaxSessionTicks(int t) { this.maxSessionTicks = t; }
    public void setMaxConsecutiveErrors(int n) { this.maxConsecutiveErrors = n; }
}
