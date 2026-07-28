/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.swarm;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;

/**
 * Layer 7 outbound-bandwidth budget. Tracks cumulative bytes per
 * second; rate-limits low-priority signals when the budget is
 * saturated. Loop=2 / Epoch=1.
 */
public class BandwidthBudgetNeuron extends ModulatableNeuron implements IBandwidthBudgetNeuron {

    private int budgetKbps = 100;
    private long windowStartTickSec = Long.MIN_VALUE;
    private long bytesInWindow;

    public BandwidthBudgetNeuron() { super(); }
    public BandwidthBudgetNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    @Override public void setBudgetKbps(int kbps) { this.budgetKbps = Math.max(1, kbps); }
    @Override public int getBudgetKbps() { return budgetKbps; }

    private void rotateWindow(long currentTick) {
        long sec = currentTick / 1000L;
        if (sec != windowStartTickSec) { windowStartTickSec = sec; bytesInWindow = 0; }
    }

    @Override
    public boolean allowLowPriority(int bytes, long currentTick) {
        rotateWindow(currentTick);
        long budgetBytes = (long) budgetKbps * 1000L / 8L;
        if (bytesInWindow + bytes > budgetBytes) return false;
        bytesInWindow += bytes;
        return true;
    }

    @Override
    public void recordHighPriority(int bytes, long currentTick) {
        rotateWindow(currentTick);
        bytesInWindow += bytes;
    }

    @Override public int currentRateBytesPerSec() { return (int) bytesInWindow; }
}
