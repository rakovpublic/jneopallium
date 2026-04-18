/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.curiosity;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.curiosity.NoveltySignal;

import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Hash-based novelty detector using a decaying Bloom-style filter over
 * recent context hashes. Produces a {@link NoveltySignal} whose score is
 * high for unseen contexts and decays as a context recurs.
 * Layer 1, loop=1 / epoch=2.
 * <p>Biological analogue: hippocampal CA1 novelty signal (Vinogradova 2001;
 * Lisman &amp; Grace 2005 SN/VTA loop).
 */
public class NoveltyDetectorNeuron extends ModulatableNeuron {

    private final int bitCount;
    private final BitSet filter;
    private final int decayTicks;
    private final Map<String, Long> lastSeen = new LinkedHashMap<>();
    private long tick;

    public NoveltyDetectorNeuron() {
        this(2048, 1000);
    }

    public NoveltyDetectorNeuron(int bitCount, int decayTicks) {
        super();
        this.bitCount = Math.max(64, bitCount);
        this.filter = new BitSet(this.bitCount);
        this.decayTicks = Math.max(1, decayTicks);
    }

    public NoveltyDetectorNeuron(Long neuronId, ISignalChain chain, Long run) {
        super(neuronId, chain, run);
        this.bitCount = 2048;
        this.filter = new BitSet(this.bitCount);
        this.decayTicks = 1000;
    }

    /**
     * Evaluate novelty of a given context hash and insert it into the filter.
     *
     * @return a {@link NoveltySignal} with score in [0,1].
     */
    public NoveltySignal evaluate(String contextHash) {
        if (contextHash == null) return new NoveltySignal(0.0, null);
        tick++;
        evictStale();
        int[] bits = hashBits(contextHash);
        boolean allSet = true;
        for (int b : bits) {
            if (!filter.get(b)) { allSet = false; break; }
        }
        Long prev = lastSeen.get(contextHash);
        double score;
        if (prev == null) {
            score = allSet ? 0.5 : 1.0;
        } else {
            long age = tick - prev;
            double recency = Math.min(1.0, (double) age / decayTicks);
            score = Math.max(0.0, Math.min(1.0, recency));
        }
        for (int b : bits) filter.set(b);
        lastSeen.put(contextHash, tick);
        NoveltySignal out = new NoveltySignal(score, contextHash);
        out.setSourceNeuronId(this.getId());
        return out;
    }

    private void evictStale() {
        long cutoff = tick - (long) decayTicks * 2L;
        lastSeen.entrySet().removeIf(e -> e.getValue() < cutoff);
    }

    private int[] hashBits(String s) {
        int h1 = s.hashCode();
        int h2 = Integer.reverse(h1 ^ 0x9E3779B9);
        int h3 = Integer.rotateLeft(h1, 11) ^ 0x85EBCA6B;
        return new int[] {
                Math.floorMod(h1, bitCount),
                Math.floorMod(h2, bitCount),
                Math.floorMod(h3, bitCount)
        };
    }

    public long getTick() { return tick; }
    public int getBitCount() { return bitCount; }
    public int getDecayTicks() { return decayTicks; }
}
