package com.rakovpublic.jneuropallium.ai.neurons.harm;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;

import java.util.HashMap;
import java.util.Map;

/**
 * Harm learning neuron used by FeedbackLearningProcessor.
 * Maintains a per-plan predicted-harm cache and applies asymmetric learning:
 * harm under-estimates are corrected faster than over-estimates (conservatism bias > 1.0).
 */
public class HarmLearningNeuron extends ModulatableNeuron implements IHarmLearningNeuron {

    private double learningRate;
    private double conservatismBias;
    private Map<String, double[]> predictedHarmByPlan;

    public HarmLearningNeuron() {
        super();
        this.learningRate = 0.05;
        this.conservatismBias = 2.0;
        this.predictedHarmByPlan = new HashMap<>();
    }

    public HarmLearningNeuron(Long neuronId,
                              com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain chain,
                              Long run,
                              double learningRate,
                              double conservatismBias) {
        super(neuronId, chain, run);
        this.learningRate = learningRate;
        this.conservatismBias = conservatismBias;
        this.predictedHarmByPlan = new HashMap<>();
    }

    public double[] getPredictedHarmForPlan(String planId) {
        return predictedHarmByPlan.get(planId);
    }

    public void setPredictedHarmForPlan(String planId, double[] predicted) {
        predictedHarmByPlan.put(planId, predicted);
    }

    public double getLearningRate() { return learningRate; }
    public void setLearningRate(double learningRate) { this.learningRate = learningRate; }

    public double getConservatismBias() { return conservatismBias; }
    public void setConservatismBias(double conservatismBias) { this.conservatismBias = conservatismBias; }

    public Map<String, double[]> getPredictedHarmByPlan() { return predictedHarmByPlan; }
    public void setPredictedHarmByPlan(Map<String, double[]> predictedHarmByPlan) { this.predictedHarmByPlan = predictedHarmByPlan; }
}
