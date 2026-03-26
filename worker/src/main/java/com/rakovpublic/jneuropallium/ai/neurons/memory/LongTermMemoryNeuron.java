package com.rakovpublic.jneuropallium.ai.neurons.memory;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;

import java.util.ArrayList;
import java.util.List;

/**
 * Long-term memory neuron used by ConsolidationProcessor.
 * Stores Hebbian pattern/value pairs and supports importance-gated writes with forgetting decay.
 */
public class LongTermMemoryNeuron extends ModulatableNeuron {

    private List<double[]> patterns;
    private List<double[]> values;
    private double importanceThreshold;
    private double learningRate;
    private double forgettingRate;

    public LongTermMemoryNeuron() {
        super();
        this.patterns = new ArrayList<>();
        this.values = new ArrayList<>();
        this.importanceThreshold = 0.5;
        this.learningRate = 0.01;
        this.forgettingRate = 0.001;
    }

    public LongTermMemoryNeuron(Long neuronId,
                                com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain chain,
                                Long run,
                                double importanceThreshold,
                                double learningRate,
                                double forgettingRate) {
        super(neuronId, chain, run);
        this.patterns = new ArrayList<>();
        this.values = new ArrayList<>();
        this.importanceThreshold = importanceThreshold;
        this.learningRate = learningRate;
        this.forgettingRate = forgettingRate;
    }

    public List<double[]> getPatterns() { return patterns; }
    public void setPatterns(List<double[]> patterns) { this.patterns = patterns; }

    public List<double[]> getValues() { return values; }
    public void setValues(List<double[]> values) { this.values = values; }

    public double getImportanceThreshold() { return importanceThreshold; }
    public void setImportanceThreshold(double importanceThreshold) { this.importanceThreshold = importanceThreshold; }

    public double getLearningRate() { return learningRate; }
    public void setLearningRate(double learningRate) { this.learningRate = learningRate; }

    public double getForgettingRate() { return forgettingRate; }
    public void setForgettingRate(double forgettingRate) { this.forgettingRate = forgettingRate; }
}
