/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.glia;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.Axon;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Axon subclass that carries per-connection propagation delays in ticks.
 * The signal dispatcher inspects {@link #getDelay(long)} and, if non-zero,
 * enqueues the outgoing signal for delivery {@code delay} ticks later.
 * <p>Biological analogue: myelinated axon with activity-dependent
 * propagation speed (Fields 2015).
 */
public class DelayedAxon extends Axon {

    private final Map<Long, Integer> perConnectionDelayTicks = new ConcurrentHashMap<>();
    private int baselineDelayTicks = 5;
    private int minDelayTicks = 1;

    public DelayedAxon() { super(); }

    public DelayedAxon(int baselineDelayTicks, int minDelayTicks) {
        super();
        this.baselineDelayTicks = Math.max(0, baselineDelayTicks);
        this.minDelayTicks = Math.max(0, minDelayTicks);
    }

    /**
     * @return the delay in ticks currently configured for a given target
     * neuron, or {@code 0} if none configured.
     */
    public int getDelay(long targetNeuronId) {
        Integer d = perConnectionDelayTicks.get(targetNeuronId);
        return d == null ? 0 : d;
    }

    /**
     * Set the delay for a target; the value is clamped into
     * {@code [minDelayTicks, baselineDelayTicks]} so that myelination can
     * never push a delay above the configured maximum.
     */
    public void setDelay(long targetNeuronId, int ticks) {
        int clamped = Math.max(minDelayTicks, Math.min(baselineDelayTicks, ticks));
        perConnectionDelayTicks.put(targetNeuronId, clamped);
    }

    /**
     * Unconditionally remove any delay for a connection.
     */
    public void removeDelay(long targetNeuronId) {
        perConnectionDelayTicks.remove(targetNeuronId);
    }

    /**
     * Monotonic accelerator: reduces delay for a frequently-used path
     * toward {@link #minDelayTicks} but never below it and never above
     * {@link #baselineDelayTicks}. Used by {@link MyelinationNeuron}.
     *
     * @return the new delay after the decrement.
     */
    public int accelerate(long targetNeuronId, int decrement) {
        int current = perConnectionDelayTicks.getOrDefault(targetNeuronId, baselineDelayTicks);
        int next = Math.max(minDelayTicks, current - Math.max(0, decrement));
        perConnectionDelayTicks.put(targetNeuronId, next);
        return next;
    }

    /**
     * Demyelinate: restore the baseline delay; never pushes beyond baseline.
     */
    public int demyelinate(long targetNeuronId) {
        perConnectionDelayTicks.put(targetNeuronId, baselineDelayTicks);
        return baselineDelayTicks;
    }

    public int getBaselineDelayTicks() { return baselineDelayTicks; }
    public void setBaselineDelayTicks(int baselineDelayTicks) { this.baselineDelayTicks = Math.max(0, baselineDelayTicks); }
    public int getMinDelayTicks() { return minDelayTicks; }
    public void setMinDelayTicks(int minDelayTicks) { this.minDelayTicks = Math.max(0, minDelayTicks); }

    public int connectionCount() { return perConnectionDelayTicks.size(); }
}
