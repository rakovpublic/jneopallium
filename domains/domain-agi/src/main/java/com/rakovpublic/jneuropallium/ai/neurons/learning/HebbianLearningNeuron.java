package com.rakovpublic.jneuropallium.ai.neurons.learning;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;

import java.util.HashMap;
import java.util.Map;

/**
 * Hebbian learning neuron used by HebbianUpdateProcessor.
 * ACh-gated: weight updates are only applied when ACh level exceeds the threshold.
 */
public class HebbianLearningNeuron extends ModulatableNeuron implements IHebbianLearningNeuron {

    private double achThreshold;
    private double learningRate;
    private Map<String, Double> synapticWeights;

    public HebbianLearningNeuron() {
        super();
        this.achThreshold = 0.5;
        this.learningRate = 0.01;
        this.synapticWeights = new HashMap<>();
    }

    public HebbianLearningNeuron(Long neuronId,
                                 com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain chain,
                                 Long run,
                                 double achThreshold,
                                 double learningRate) {
        super(neuronId, chain, run);
        this.achThreshold = achThreshold;
        this.learningRate = learningRate;
        this.synapticWeights = new HashMap<>();
    }

    public void applyWeightDelta(String targetNeuronId, double delta) {
        if (targetNeuronId == null) return;
        synapticWeights.merge(targetNeuronId, delta, Double::sum);
    }

    public double getAchThreshold() { return achThreshold; }
    public void setAchThreshold(double achThreshold) { this.achThreshold = achThreshold; }

    public double getLearningRate() { return learningRate; }
    public void setLearningRate(double learningRate) { this.learningRate = learningRate; }

    public Map<String, Double> getSynapticWeights() { return synapticWeights; }
    public void setSynapticWeights(Map<String, Double> synapticWeights) { this.synapticWeights = synapticWeights; }
}
