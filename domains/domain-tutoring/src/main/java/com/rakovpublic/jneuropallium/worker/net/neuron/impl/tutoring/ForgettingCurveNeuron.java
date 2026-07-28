/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.tutoring;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.tutoring.ReviewScheduleSignal;

import java.util.HashMap;
import java.util.Map;

/**
 * Layer 3 per-concept retention model combining the Ebbinghaus forgetting
 * curve (1885) with SuperMemo SM-2 style interval scheduling. Emits
 * {@link ReviewScheduleSignal} entries to feed the sleep-driven replay and
 * the ZPD planner.
 * Loop=2 / Epoch=3.
 */
public class ForgettingCurveNeuron extends ModulatableNeuron implements IForgettingCurveNeuron {

    private static final class State {
        double stability;       // days-equivalent of stable retention
        double easiness = 2.5;  // SM-2 EF
        int repetitions;
        long lastSeenTick;
        long intervalTicks;
    }

    private final Map<String, State> perConcept = new HashMap<>();
    private double targetRetention = 0.9;
    private long baseIntervalTicks = 100;

    public ForgettingCurveNeuron() { super(); }
    public ForgettingCurveNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    /**
     * Estimate current retention R = exp(-elapsed / stability).
     */
    public double retention(String conceptId, long nowTick) {
        State s = perConcept.get(conceptId);
        if (s == null || s.stability <= 0) return 0.0;
        long elapsed = nowTick - s.lastSeenTick;
        if (elapsed <= 0) return 1.0;
        return Math.exp(-((double) elapsed) / s.stability);
    }

    /**
     * Record an attempt. {@code quality} is the SM-2 quality grade in [0..5].
     * Returns a ReviewScheduleSignal with the next review tick.
     */
    public ReviewScheduleSignal recordAttempt(String conceptId, long nowTick, int quality) {
        State s = perConcept.computeIfAbsent(conceptId, k -> new State());
        s.lastSeenTick = nowTick;
        if (quality < 3) {
            s.repetitions = 0;
            s.intervalTicks = baseIntervalTicks;
        } else {
            s.repetitions++;
            if (s.repetitions == 1) s.intervalTicks = baseIntervalTicks;
            else if (s.repetitions == 2) s.intervalTicks = baseIntervalTicks * 6;
            else s.intervalTicks = (long) (s.intervalTicks * s.easiness);
            s.easiness = Math.max(1.3, s.easiness + 0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02));
        }
        s.stability = Math.max(1.0, s.intervalTicks / Math.log(1.0 / Math.max(0.01, targetRetention)));

        ReviewScheduleSignal sig = new ReviewScheduleSignal(conceptId,
                nowTick + s.intervalTicks, targetRetention);
        sig.setSourceNeuronId(this.getId());
        return sig;
    }

    public void setTargetRetention(double r) { this.targetRetention = Math.max(0.01, Math.min(1.0, r)); }
    public double getTargetRetention() { return targetRetention; }
    public void setBaseIntervalTicks(long t) { this.baseIntervalTicks = Math.max(1, t); }
    public int trackedConcepts() { return perConcept.size(); }
    public long intervalFor(String conceptId) {
        State s = perConcept.get(conceptId);
        return s == null ? 0 : s.intervalTicks;
    }
    public int repetitionsFor(String conceptId) {
        State s = perConcept.get(conceptId);
        return s == null ? 0 : s.repetitions;
    }
}
