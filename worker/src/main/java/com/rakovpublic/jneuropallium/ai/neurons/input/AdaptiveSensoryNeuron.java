package com.rakovpublic.jneuropallium.ai.neurons.input;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;

/**
 * Sensory neuron with an adaptive firing threshold used by AdaptiveThresholdProcessor.
 * The threshold decays over time and rises after each spike (spike-frequency adaptation).
 */
public class AdaptiveSensoryNeuron extends ModulatableNeuron {

    private double threshold;
    private double decayRate;
    private double adaptationRate;

    public AdaptiveSensoryNeuron() {
        super();
        this.threshold = 0.5;
        this.decayRate = 0.01;
        this.adaptationRate = 0.1;
    }

    public AdaptiveSensoryNeuron(Long neuronId,
                                 com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain chain,
                                 Long run,
                                 double threshold,
                                 double decayRate,
                                 double adaptationRate) {
        super(neuronId, chain, run);
        this.threshold = threshold;
        this.decayRate = decayRate;
        this.adaptationRate = adaptationRate;
    }

    public double getThreshold() { return threshold; }
    public void setThreshold(double threshold) { this.threshold = threshold; }

    public double getDecayRate() { return decayRate; }
    public void setDecayRate(double decayRate) { this.decayRate = decayRate; }

    public double getAdaptationRate() { return adaptationRate; }
    public void setAdaptationRate(double adaptationRate) { this.adaptationRate = adaptationRate; }
}
