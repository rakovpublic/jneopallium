package com.rakovpublic.jneuropallium.ai.neurons.loop;

import com.rakovpublic.jneuropallium.ai.model.ActiveIntervention;
import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;

import java.util.HashMap;
import java.util.Map;

/**
 * Loop circuit-breaker neuron used by InterventionDispatchProcessor.
 * Tracks per-region intervention counts and manages active interventions.
 * Escalates to more severe interventions when a region repeatedly triggers alerts.
 */
public class LoopCircuitBreakerNeuron extends ModulatableNeuron {

    private Map<String, Integer> interventionHistory;
    private Map<String, ActiveIntervention> activeInterventions;
    private int maxInterventions;

    public LoopCircuitBreakerNeuron() {
        super();
        this.interventionHistory = new HashMap<>();
        this.activeInterventions = new HashMap<>();
        this.maxInterventions = 3;
    }

    public LoopCircuitBreakerNeuron(Long neuronId,
                                    com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain chain,
                                    Long run,
                                    int maxInterventions) {
        super(neuronId, chain, run);
        this.interventionHistory = new HashMap<>();
        this.activeInterventions = new HashMap<>();
        this.maxInterventions = maxInterventions;
    }

    public Map<String, Integer> getInterventionHistory() { return interventionHistory; }
    public void setInterventionHistory(Map<String, Integer> interventionHistory) { this.interventionHistory = interventionHistory; }

    public Map<String, ActiveIntervention> getActiveInterventions() { return activeInterventions; }
    public void setActiveInterventions(Map<String, ActiveIntervention> activeInterventions) { this.activeInterventions = activeInterventions; }

    public int getMaxInterventions() { return maxInterventions; }
    public void setMaxInterventions(int maxInterventions) { this.maxInterventions = maxInterventions; }
}
