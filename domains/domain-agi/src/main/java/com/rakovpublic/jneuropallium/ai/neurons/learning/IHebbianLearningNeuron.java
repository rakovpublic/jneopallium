package com.rakovpublic.jneuropallium.ai.neurons.learning;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;

import java.util.Map;

public interface IHebbianLearningNeuron extends IModulatableNeuron {
    void applyWeightDelta(String targetNeuronId, double delta);
    double getAchThreshold();
    void setAchThreshold(double achThreshold);
    double getLearningRate();
    void setLearningRate(double learningRate);
    Map<String, Double> getSynapticWeights();
    void setSynapticWeights(Map<String, Double> synapticWeights);
}
