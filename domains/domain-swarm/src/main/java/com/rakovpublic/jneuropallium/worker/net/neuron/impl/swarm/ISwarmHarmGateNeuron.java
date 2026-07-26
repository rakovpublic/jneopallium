package com.rakovpublic.jneuropallium.worker.net.neuron.impl.swarm;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm.SwarmAlertSignal;

public interface ISwarmHarmGateNeuron extends IModulatableNeuron {
    /** Record this agent's projected emission (thermal / EM / chemical / etc.) for the region. */
    void recordEmission(String regionId, String agentId, double projectedEmission);
    /** Aggregate regional emission and emit a SwarmAlert when the threshold is exceeded. */
    SwarmAlertSignal aggregate(String regionId);
    void setRegionalThreshold(String regionId, double threshold);
    double currentEmission(String regionId);
    /** Multiplier in [1, n] that downstream HarmGateNeuron should apply. */
    double tighteningMultiplier(String regionId);

    /**
     * Observation channel: an external alert (e.g. from a regional
     * aggregator) tightens local thresholds even before the agent's own
     * accounting catches up. Default is a no-op.
     */
    default void onAlert(SwarmAlertSignal a) { /* no-op by default */ }
}
