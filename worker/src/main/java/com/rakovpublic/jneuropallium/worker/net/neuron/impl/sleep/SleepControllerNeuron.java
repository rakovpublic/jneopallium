/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.sleep;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.sleep.SleepStateSignal;

/**
 * Drives the sleep-wake cycle on a fixed circadian period supplied by
 * {@code CircadianNeuron}. Emits {@link SleepStateSignal}s with the
 * current phase and depth and gates other sleep neurons.
 * Layer 7, loop=2 / epoch=10.
 * <p>Biological analogue: suprachiasmatic-nucleus + brainstem sleep-wake
 * control (Saper et al. 2005).
 */
public class SleepControllerNeuron extends ModulatableNeuron implements ISleepControllerNeuron {

    private int cycleTicks = 10000;
    private double nremFraction = 0.6;
    private double remFraction = 0.15;
    private long tick;
    private SleepPhase phase = SleepPhase.WAKE;

    public SleepControllerNeuron() { super(); }

    public SleepControllerNeuron(Long neuronId, ISignalChain chain, Long run) {
        super(neuronId, chain, run);
    }

    public SleepStateSignal advance() {
        tick++;
        double progress = (tick % cycleTicks) / (double) cycleTicks;
        double wakeEnd = 1.0 - (nremFraction + remFraction);
        double nremEnd = wakeEnd + nremFraction;
        double depth;
        if (progress < wakeEnd) {
            phase = SleepPhase.WAKE;
            depth = 0.0;
        } else if (progress < nremEnd) {
            double relative = (progress - wakeEnd) / Math.max(1e-9, nremFraction);
            if (relative < 0.5) {
                phase = SleepPhase.NREM2;
                depth = 0.5 * (relative / 0.5);
            } else {
                phase = SleepPhase.NREM3;
                depth = 0.5 + 0.5 * ((relative - 0.5) / 0.5);
            }
        } else {
            phase = SleepPhase.REM;
            depth = 0.7;
        }
        SleepStateSignal s = new SleepStateSignal(phase, depth);
        s.setSourceNeuronId(this.getId());
        return s;
    }

    /**
     * Force-set the phase. Exposed for tests and for externally-driven
     * sleep scheduling (e.g. circadian override).
     */
    public void setPhase(SleepPhase phase) {
        this.phase = phase == null ? SleepPhase.WAKE : phase;
    }

    public SleepPhase currentPhase() { return phase; }
    public long getTick() { return tick; }
    public int getCycleTicks() { return cycleTicks; }
    public void setCycleTicks(int v) { this.cycleTicks = Math.max(2, v); }
    public double getNremFraction() { return nremFraction; }
    public void setNremFraction(double v) { this.nremFraction = clamp01(v); }
    public double getRemFraction() { return remFraction; }
    public void setRemFraction(double v) { this.remFraction = clamp01(v); }

    private static double clamp01(double v) { return Math.max(0.0, Math.min(1.0, v)); }
}
