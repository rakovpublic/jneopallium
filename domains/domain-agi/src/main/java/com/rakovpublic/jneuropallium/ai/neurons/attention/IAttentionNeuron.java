package com.rakovpublic.jneuropallium.ai.neurons.attention;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;

import java.util.Map;

public interface IAttentionNeuron extends IModulatableNeuron {
    Map<String, double[]> getGoalFeatureMap();
    void setGoalFeatureMap(Map<String, double[]> goalFeatureMap);
    Map<String, Double> getSalienceMap();
    void setSalienceMap(Map<String, Double> salienceMap);
    double[] getActiveGoalFeature();
    void setActiveGoalFeature(double[] activeGoalFeature);
}
