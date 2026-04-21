package com.rakovpublic.jneuropallium.ai.neurons.harm;

import com.rakovpublic.jneuropallium.ai.model.WorldStateModel;
import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;

import java.util.HashMap;
import java.util.Map;

/**
 * Consequence model neuron used by ForwardSimulationProcessor.
 * Maintains a world-state model and per-action effect weights for forward simulation.
 * The world model is always deep-cloned before simulation — the live model is never mutated.
 */
public class ConsequenceModelNeuron extends ModulatableNeuron implements IConsequenceModelNeuron {

    private WorldStateModel worldModel;
    private Map<String, double[]> actionEffectWeights;
    private Map<String, Double> actionConfidences;

    public ConsequenceModelNeuron() {
        super();
        this.worldModel = new WorldStateModel();
        this.actionEffectWeights = new HashMap<>();
        this.actionConfidences = new HashMap<>();
    }

    public ConsequenceModelNeuron(Long neuronId,
                                  com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain chain,
                                  Long run,
                                  WorldStateModel worldModel) {
        super(neuronId, chain, run);
        this.worldModel = worldModel;
        this.actionEffectWeights = new HashMap<>();
        this.actionConfidences = new HashMap<>();
    }

    public double getActionConfidence(String actionType) {
        return actionConfidences.getOrDefault(actionType, 0.5);
    }

    public WorldStateModel getWorldModel() { return worldModel; }
    public void setWorldModel(WorldStateModel worldModel) { this.worldModel = worldModel; }

    public Map<String, double[]> getActionEffectWeights() { return actionEffectWeights; }
    public void setActionEffectWeights(Map<String, double[]> actionEffectWeights) { this.actionEffectWeights = actionEffectWeights; }

    public Map<String, Double> getActionConfidences() { return actionConfidences; }
    public void setActionConfidences(Map<String, Double> actionConfidences) { this.actionConfidences = actionConfidences; }
}
