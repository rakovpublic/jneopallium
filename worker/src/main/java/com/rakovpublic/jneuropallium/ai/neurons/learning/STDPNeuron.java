package com.rakovpublic.jneuropallium.ai.neurons.learning;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;

import java.util.HashMap;
import java.util.Map;

/**
 * Spike-Timing Dependent Plasticity neuron used by STDPProcessor.
 * Adjusts synaptic weights based on relative timing of pre- and post-synaptic spikes.
 */
public class STDPNeuron extends ModulatableNeuron {

    private Map<String, Long> preSpikeTimestamps;
    private Map<String, Double> weights;
    private long stdpWindow;
    private double ltpRate;
    private double ltdRate;

    public STDPNeuron() {
        super();
        this.preSpikeTimestamps = new HashMap<>();
        this.weights = new HashMap<>();
        this.stdpWindow = 20L;
        this.ltpRate = 0.01;
        this.ltdRate = 0.005;
    }

    public STDPNeuron(Long neuronId,
                      com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain chain,
                      Long run,
                      long stdpWindow,
                      double ltpRate,
                      double ltdRate) {
        super(neuronId, chain, run);
        this.preSpikeTimestamps = new HashMap<>();
        this.weights = new HashMap<>();
        this.stdpWindow = stdpWindow;
        this.ltpRate = ltpRate;
        this.ltdRate = ltdRate;
    }

    public void adjustWeight(String synapseId, double delta) {
        weights.merge(synapseId, delta, Double::sum);
    }

    public Map<String, Long> getPreSpikeTimestamps() { return preSpikeTimestamps; }
    public void setPreSpikeTimestamps(Map<String, Long> preSpikeTimestamps) { this.preSpikeTimestamps = preSpikeTimestamps; }

    public Map<String, Double> getWeights() { return weights; }
    public void setWeights(Map<String, Double> weights) { this.weights = weights; }

    public long getStdpWindow() { return stdpWindow; }
    public void setStdpWindow(long stdpWindow) { this.stdpWindow = stdpWindow; }

    public double getLtpRate() { return ltpRate; }
    public void setLtpRate(double ltpRate) { this.ltpRate = ltpRate; }

    public double getLtdRate() { return ltdRate; }
    public void setLtdRate(double ltdRate) { this.ltdRate = ltdRate; }
}
