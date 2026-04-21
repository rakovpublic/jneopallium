package com.rakovpublic.jneuropallium.ai.neurons.memory;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;

import java.util.List;

public interface ILongTermMemoryNeuron extends IModulatableNeuron {
    List<double[]> getPatterns();
    void setPatterns(List<double[]> patterns);
    List<double[]> getValues();
    void setValues(List<double[]> values);
    double getImportanceThreshold();
    void setImportanceThreshold(double importanceThreshold);
    double getLearningRate();
    void setLearningRate(double learningRate);
    double getForgettingRate();
    void setForgettingRate(double forgettingRate);
}
