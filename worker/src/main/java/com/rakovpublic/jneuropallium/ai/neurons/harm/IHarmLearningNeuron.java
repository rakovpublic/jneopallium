package com.rakovpublic.jneuropallium.ai.neurons.harm;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;

import java.util.Map;

public interface IHarmLearningNeuron extends IModulatableNeuron {
    double[] getPredictedHarmForPlan(String planId);
    void setPredictedHarmForPlan(String planId, double[] predicted);
    double getLearningRate();
    void setLearningRate(double learningRate);
    double getConservatismBias();
    void setConservatismBias(double conservatismBias);
    Map<String, double[]> getPredictedHarmByPlan();
    void setPredictedHarmByPlan(Map<String, double[]> predictedHarmByPlan);
}
