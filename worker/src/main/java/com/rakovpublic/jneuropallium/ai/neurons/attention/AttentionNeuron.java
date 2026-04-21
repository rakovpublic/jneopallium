package com.rakovpublic.jneuropallium.ai.neurons.attention;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;

import java.util.HashMap;
import java.util.Map;

/**
 * Attention neuron used by SalienceComputationProcessor and SalienceGoalProcessor.
 * Maintains a goal-feature map for top-down biasing and a salience map for bottom-up inputs.
 */
public class AttentionNeuron extends ModulatableNeuron implements IAttentionNeuron {

    private Map<String, double[]> goalFeatureMap;
    private Map<String, Double> salienceMap;
    private double[] activeGoalFeature;

    public AttentionNeuron() {
        super();
        this.goalFeatureMap = new HashMap<>();
        this.salienceMap = new HashMap<>();
    }

    public AttentionNeuron(Long neuronId,
                           com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain chain,
                           Long run) {
        super(neuronId, chain, run);
        this.goalFeatureMap = new HashMap<>();
        this.salienceMap = new HashMap<>();
    }

    public Map<String, double[]> getGoalFeatureMap() { return goalFeatureMap; }
    public void setGoalFeatureMap(Map<String, double[]> goalFeatureMap) { this.goalFeatureMap = goalFeatureMap; }

    public Map<String, Double> getSalienceMap() { return salienceMap; }
    public void setSalienceMap(Map<String, Double> salienceMap) { this.salienceMap = salienceMap; }

    public double[] getActiveGoalFeature() { return activeGoalFeature; }
    public void setActiveGoalFeature(double[] activeGoalFeature) { this.activeGoalFeature = activeGoalFeature; }
}
