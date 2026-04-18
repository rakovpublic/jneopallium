/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.sleep;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.sleep.SharpWaveRippleSignal;

import java.util.ArrayList;
import java.util.List;

/**
 * Emits a compressed burst replay during NREM3. Output is routed to
 * {@code LongTermMemoryNeuron}'s consolidation processor via the fast-path
 * documented in the autonomous-AI paper.
 * Layer 3, loop=2 / epoch=1.
 * <p>Biological analogue: hippocampal sharp-wave ripples (Buzsáki 2015).
 */
public class SharpWaveRippleNeuron extends ModulatableNeuron {

    private double minNrem3Depth = 0.6;

    public SharpWaveRippleNeuron() { super(); }

    public SharpWaveRippleNeuron(Long neuronId, ISignalChain chain, Long run) {
        super(neuronId, chain, run);
    }

    /**
     * Emit a sharp-wave ripple for the provided neuron sequence if the
     * controller phase is NREM3 with depth &gt;= {@link #minNrem3Depth}.
     */
    public SharpWaveRippleSignal maybeEmit(SleepPhase phase, double depth, List<Long> neuronSequence, double power) {
        if (phase != SleepPhase.NREM3) return null;
        if (depth < minNrem3Depth) return null;
        List<Long> seq = neuronSequence == null ? new ArrayList<>() : new ArrayList<>(neuronSequence);
        SharpWaveRippleSignal s = new SharpWaveRippleSignal(seq, power);
        s.setSourceNeuronId(this.getId());
        return s;
    }

    public double getMinNrem3Depth() { return minNrem3Depth; }
    public void setMinNrem3Depth(double v) { this.minNrem3Depth = Math.max(0.0, Math.min(1.0, v)); }
}
