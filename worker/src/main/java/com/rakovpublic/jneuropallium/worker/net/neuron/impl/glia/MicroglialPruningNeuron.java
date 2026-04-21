/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.glia;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.glia.PruningSignal;

import java.util.HashMap;
import java.util.Map;

/**
 * Emits {@link PruningSignal}s for connections that have been inactive
 * for at least {@link #minInactivityTicks} and respects a per-epoch
 * cap {@link #maxPruningsPerEpoch}.
 * Support layer; loop=2 / epoch=5.
 * <p>Biological analogue: microglial synaptic pruning
 * (Schafer et al. 2012).
 */
public class MicroglialPruningNeuron extends ModulatableNeuron implements IMicroglialPruningNeuron {

    /** Connection key: encoded {@code (sourceId, targetId)}. */
    private static class Key {
        final long source;
        final long target;
        Key(long s, long t) { source = s; target = t; }
        @Override public boolean equals(Object o) {
            if (!(o instanceof Key)) return false;
            Key k = (Key) o;
            return k.source == source && k.target == target;
        }
        @Override public int hashCode() { return (int) (source * 31L + target); }
    }

    private final Map<Key, Long> lastActivityTick = new HashMap<>();
    private long currentTick;
    private int minInactivityTicks = 2000;
    private int maxPruningsPerEpoch = 10;
    private int emittedThisEpoch;

    public MicroglialPruningNeuron() { super(); }

    public MicroglialPruningNeuron(Long neuronId, ISignalChain chain, Long run) {
        super(neuronId, chain, run);
    }

    public void tick() { currentTick++; }
    public long getCurrentTick() { return currentTick; }

    public void recordActivity(long sourceId, long targetId) {
        lastActivityTick.put(new Key(sourceId, targetId), currentTick);
    }

    /**
     * If the connection has been silent for at least
     * {@link #minInactivityTicks} and the per-epoch cap is not exceeded,
     * return a pruning signal; otherwise return {@code null}.
     */
    public PruningSignal maybePrune(long sourceId, long targetId, String reason) {
        if (emittedThisEpoch >= maxPruningsPerEpoch) return null;
        Key k = new Key(sourceId, targetId);
        Long last = lastActivityTick.get(k);
        long silent = (last == null) ? currentTick : currentTick - last;
        if (silent < minInactivityTicks) return null;
        emittedThisEpoch++;
        lastActivityTick.remove(k);
        PruningSignal p = new PruningSignal(sourceId, targetId, reason == null ? "inactivity" : reason);
        p.setSourceNeuronId(this.getId());
        return p;
    }

    public void resetEpochCounter() { emittedThisEpoch = 0; }

    public int getMinInactivityTicks() { return minInactivityTicks; }
    public void setMinInactivityTicks(int v) { this.minInactivityTicks = Math.max(1, v); }
    public int getMaxPruningsPerEpoch() { return maxPruningsPerEpoch; }
    public void setMaxPruningsPerEpoch(int v) { this.maxPruningsPerEpoch = Math.max(0, v); }
    public int emittedThisEpoch() { return emittedThisEpoch; }
    public int trackedConnections() { return lastActivityTick.size(); }
}
