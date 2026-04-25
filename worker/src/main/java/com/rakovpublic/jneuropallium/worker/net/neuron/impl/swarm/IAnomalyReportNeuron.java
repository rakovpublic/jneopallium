package com.rakovpublic.jneuropallium.worker.net.neuron.impl.swarm;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm.AgentAnomalySignal;

public interface IAnomalyReportNeuron extends IModulatableNeuron {
    void setSelfId(String id);
    String getSelfId();
    /** Lodge an anomaly report against {@code suspectId}. */
    AgentAnomalySignal report(String suspectId, AnomalyKind kind);
    int reportsAgainst(String suspectId);
    /** True iff this agent has already reported {@code suspectId} since last cooldown. */
    boolean alreadyReported(String suspectId);
}
