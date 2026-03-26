package com.rakovpublic.jneuropallium.ai.neurons.action;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.ai.signals.fast.MotorCommandSignal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Action selection neuron used by CompetitiveSelectionProcessor.
 * Maintains a candidate action map and a set of vetoed plan IDs.
 * Softmax selection is dopamine-scaled; confidence threshold is NE-modulated.
 */
public class ActionSelectionNeuron extends ModulatableNeuron {

    private Map<String, MotorCommandSignal> candidates;
    private Set<String> pendingVetoes;
    private double confidenceThreshold;

    public ActionSelectionNeuron() {
        super();
        this.candidates = new HashMap<>();
        this.pendingVetoes = new HashSet<>();
        this.confidenceThreshold = 0.3;
    }

    public ActionSelectionNeuron(Long neuronId,
                                 com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain chain,
                                 Long run,
                                 double confidenceThreshold) {
        super(neuronId, chain, run);
        this.candidates = new HashMap<>();
        this.pendingVetoes = new HashSet<>();
        this.confidenceThreshold = confidenceThreshold;
    }

    public boolean hasPendingVeto(String actionPlanId) {
        return pendingVetoes.contains(actionPlanId);
    }

    public void addVeto(String actionPlanId) {
        pendingVetoes.add(actionPlanId);
    }

    public Map<String, MotorCommandSignal> getCandidates() { return candidates; }
    public void setCandidates(Map<String, MotorCommandSignal> candidates) { this.candidates = candidates; }

    public Set<String> getPendingVetoes() { return pendingVetoes; }
    public void setPendingVetoes(Set<String> pendingVetoes) { this.pendingVetoes = pendingVetoes; }

    public double getConfidenceThreshold() { return confidenceThreshold; }
    public void setConfidenceThreshold(double confidenceThreshold) { this.confidenceThreshold = confidenceThreshold; }
}
