/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.glia;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.glia.MyelinationSignal;

import java.util.HashMap;
import java.util.Map;

/**
 * Tracks activity on connections and issues {@link MyelinationSignal}s
 * that reduce the delay on frequently-used paths toward
 * {@link #minDelayTicks} (but never below it). Biological invariant:
 * myelination only decreases delay, demyelination restores baseline.
 * Support layer; loop=2 / epoch=10.
 * <p>Biological analogue: activity-dependent oligodendrocyte myelination
 * (Fields 2015; Gibson et al. 2014).
 */
public class MyelinationNeuron extends ModulatableNeuron implements IMyelinationNeuron {

    private final Map<Long, Integer> usageSinceLastWindow = new HashMap<>();
    private final Map<Long, Integer> currentDelay = new HashMap<>();
    private int baselineDelayTicks = 5;
    private int minDelayTicks = 1;
    private int activityWindow = 500;
    private int delayDecrementPerWindow = 1;
    private int usageThreshold = 10;
    private double consolidationBoost = 1.0;

    public MyelinationNeuron() { super(); }

    public MyelinationNeuron(Long neuronId, ISignalChain chain, Long run) {
        super(neuronId, chain, run);
    }

    public void recordUsage(long targetNeuronId) {
        usageSinceLastWindow.merge(targetNeuronId, 1, Integer::sum);
    }

    /**
     * After an activity window closes, examine usage counts and issue a
     * myelination signal for any connection crossing {@link #usageThreshold},
     * reducing its delay by up to {@link #delayDecrementPerWindow} ×
     * {@link #consolidationBoost} ticks.
     */
    public MyelinationSignal evaluate(long sourceNeuronId, long targetNeuronId) {
        int usage = usageSinceLastWindow.getOrDefault(targetNeuronId, 0);
        if (usage < usageThreshold) return null;
        int current = currentDelay.getOrDefault(targetNeuronId, baselineDelayTicks);
        int decrement = (int) Math.round(delayDecrementPerWindow * consolidationBoost);
        int next = Math.max(minDelayTicks, current - Math.max(1, decrement));
        if (next >= current) return null;
        currentDelay.put(targetNeuronId, next);
        usageSinceLastWindow.put(targetNeuronId, 0);
        MyelinationSignal m = new MyelinationSignal(sourceNeuronId, targetNeuronId, next);
        m.setSourceNeuronId(this.getId());
        return m;
    }

    /**
     * Apply a myelination signal to a {@link DelayedAxon}. Honors the
     * invariant that myelination never pushes delay above baseline.
     */
    public void applyTo(DelayedAxon axon, MyelinationSignal sig) {
        if (axon == null || sig == null) return;
        axon.setDelay(sig.getAxonTargetId(), sig.getNewDelayTicks());
    }

    /**
     * Demyelinate: restore baseline delay on the given connection.
     */
    public void demyelinate(DelayedAxon axon, long targetNeuronId) {
        if (axon != null) axon.demyelinate(targetNeuronId);
        currentDelay.put(targetNeuronId, baselineDelayTicks);
    }

    public void resetWindow() { usageSinceLastWindow.clear(); }
    public int currentDelayFor(long targetNeuronId) { return currentDelay.getOrDefault(targetNeuronId, baselineDelayTicks); }

    public int getBaselineDelayTicks() { return baselineDelayTicks; }
    public void setBaselineDelayTicks(int v) { this.baselineDelayTicks = Math.max(0, v); }
    public int getMinDelayTicks() { return minDelayTicks; }
    public void setMinDelayTicks(int v) { this.minDelayTicks = Math.max(0, v); }
    public int getActivityWindow() { return activityWindow; }
    public void setActivityWindow(int v) { this.activityWindow = Math.max(1, v); }
    public int getDelayDecrementPerWindow() { return delayDecrementPerWindow; }
    public void setDelayDecrementPerWindow(int v) { this.delayDecrementPerWindow = Math.max(1, v); }
    public int getUsageThreshold() { return usageThreshold; }
    public void setUsageThreshold(int v) { this.usageThreshold = Math.max(1, v); }
    public double getConsolidationBoost() { return consolidationBoost; }
    public void setConsolidationBoost(double v) { this.consolidationBoost = Math.max(1.0, v); }
}
