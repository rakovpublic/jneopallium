package com.rakovpublic.jneuropallium.worker.net.neuron.impl.glia;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.glia.PruningSignal;

public interface IMicroglialPruningNeuron extends IModulatableNeuron {
    boolean equals(Object o);
    int hashCode();
    void tick();
    long getCurrentTick();
    void recordActivity(long sourceId, long targetId);
    PruningSignal maybePrune(long sourceId, long targetId, String reason);
    void resetEpochCounter();
    int getMinInactivityTicks();
    void setMinInactivityTicks(int v);
    int getMaxPruningsPerEpoch();
    void setMaxPruningsPerEpoch(int v);
    int emittedThisEpoch();
    int trackedConnections();
}
