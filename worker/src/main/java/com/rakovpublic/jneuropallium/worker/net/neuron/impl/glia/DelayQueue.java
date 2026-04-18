/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.glia;

import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Per-target delay queue used by the dispatcher wrapper around
 * {@link DelayedAxon}. Signals are enqueued with a release-tick; calling
 * {@link #releaseReady(long)} returns (and removes) all signals whose
 * {@code releaseAtTick <= currentTick}.
 * <p>State lives here, not in any {@link com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor}.
 */
public class DelayQueue {

    private static class Entry {
        final long releaseAtTick;
        final long targetNeuronId;
        final ISignal signal;
        Entry(long releaseAtTick, long targetNeuronId, ISignal signal) {
            this.releaseAtTick = releaseAtTick;
            this.targetNeuronId = targetNeuronId;
            this.signal = signal;
        }
    }

    private final Map<Long, Deque<Entry>> byTarget = new HashMap<>();

    public void enqueue(long currentTick, long targetNeuronId, ISignal signal, int delayTicks) {
        if (signal == null) return;
        long release = currentTick + Math.max(0, delayTicks);
        byTarget.computeIfAbsent(targetNeuronId, k -> new ArrayDeque<>())
                .add(new Entry(release, targetNeuronId, signal));
    }

    /**
     * Return and remove every queued signal whose release tick has been
     * reached. The returned list is ordered by target id, then FIFO.
     */
    public List<ReleasedSignal> releaseReady(long currentTick) {
        List<ReleasedSignal> out = new ArrayList<>();
        for (Map.Entry<Long, Deque<Entry>> bucket : byTarget.entrySet()) {
            Deque<Entry> q = bucket.getValue();
            while (!q.isEmpty() && q.peekFirst().releaseAtTick <= currentTick) {
                Entry e = q.removeFirst();
                out.add(new ReleasedSignal(e.targetNeuronId, e.signal));
            }
        }
        byTarget.values().removeIf(Deque::isEmpty);
        return out;
    }

    public int pending() {
        int total = 0;
        for (Deque<Entry> q : byTarget.values()) total += q.size();
        return total;
    }

    public int pendingFor(long targetNeuronId) {
        Deque<Entry> q = byTarget.get(targetNeuronId);
        return q == null ? 0 : q.size();
    }

    public static class ReleasedSignal {
        public final long targetNeuronId;
        public final ISignal signal;
        public ReleasedSignal(long targetNeuronId, ISignal signal) {
            this.targetNeuronId = targetNeuronId;
            this.signal = signal;
        }
    }
}
