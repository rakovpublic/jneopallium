package com.rakovpublic.jneuropallium.worker.net.neuron.impl.swarm;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;

public interface IBandwidthBudgetNeuron extends IModulatableNeuron {
    void setBudgetKbps(int kbps);
    int getBudgetKbps();
    /** Returns true iff a low-priority signal of the given size in bytes is allowed under the current budget. */
    boolean allowLowPriority(int bytes, long currentTick);
    /** High-priority always passes; the call records the bytes for accounting. */
    void recordHighPriority(int bytes, long currentTick);
    int currentRateBytesPerSec();
}
