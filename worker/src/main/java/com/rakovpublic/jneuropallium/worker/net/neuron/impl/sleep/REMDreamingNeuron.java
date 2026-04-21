/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.sleep;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.sleep.DreamSignal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Recombines episode bindings during REM to generate candidate plans with
 * {@code priority=low, source=DREAM}. Consumers must still route those
 * candidates through {@code HarmGateNeuron} before any motor execution —
 * the safety invariant for sleep-generated plans.
 * Layer 3, loop=2 / epoch=5.
 * <p>Biological analogue: prefrontal-cortex REM recombination
 * (Wamsley &amp; Stickgold 2011).
 */
public class REMDreamingNeuron extends ModulatableNeuron implements IREMDreamingNeuron {

    private int recombinationCount = 5;
    private double maxNoveltyForPlanning = 0.7;
    private final Random rng = new Random(0xBEEFBEEFL);

    public REMDreamingNeuron() { super(); }

    public REMDreamingNeuron(Long neuronId, ISignalChain chain, Long run) {
        super(neuronId, chain, run);
    }

    /**
     * Generate up to {@link #recombinationCount} dream signals by shuffled
     * recombination of bindings across the supplied episodes. Dreams whose
     * novelty exceeds {@link #maxNoveltyForPlanning} are still emitted so
     * tests can validate that a high-harm dream still reaches the harm gate
     * — the safety invariant for REM-originated plans.
     */
    public List<DreamSignal> recombine(SleepPhase phase, List<List<Long>> episodes) {
        List<DreamSignal> out = new ArrayList<>();
        if (phase != SleepPhase.REM || episodes == null || episodes.isEmpty()) return out;
        List<Long> pool = new ArrayList<>();
        for (List<Long> e : episodes) if (e != null) pool.addAll(e);
        if (pool.isEmpty()) return out;
        Set<Long> seenInPool = new HashSet<>(pool);
        for (int i = 0; i < recombinationCount; i++) {
            List<Long> shuffled = new ArrayList<>(pool);
            Collections.shuffle(shuffled, rng);
            int size = Math.min(shuffled.size(), 1 + rng.nextInt(Math.max(1, pool.size())));
            List<Long> bindings = new ArrayList<>(shuffled.subList(0, size));
            Set<Long> uniq = new HashSet<>(bindings);
            double novelty = seenInPool.isEmpty() ? 0.0
                    : 1.0 - ((double) uniq.size() / seenInPool.size());
            DreamSignal d = new DreamSignal(bindings, clamp01(novelty));
            d.setSourceNeuronId(this.getId());
            out.add(d);
        }
        return out;
    }

    /**
     * Filter predicate used by a planner to reject unsafe high-novelty
     * dreams from being proposed as plans.
     */
    public boolean isPlanningCandidate(DreamSignal dream) {
        if (dream == null) return false;
        return dream.getNoveltyScore() <= maxNoveltyForPlanning;
    }

    private static double clamp01(double v) { return Math.max(0.0, Math.min(1.0, v)); }

    public int getRecombinationCount() { return recombinationCount; }
    public void setRecombinationCount(int v) { this.recombinationCount = Math.max(1, v); }
    public double getMaxNoveltyForPlanning() { return maxNoveltyForPlanning; }
    public void setMaxNoveltyForPlanning(double v) { this.maxNoveltyForPlanning = clamp01(v); }
}
