package com.rakovpublic.jneuropallium.ai.neurons.input;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;

/**
 * Sensory encoder neuron used by PopulationCodeProcessor.
 * Each neuron is tuned to a preferred stimulus value with a Gaussian receptive field.
 */
public class SensoryEncoderNeuron extends ModulatableNeuron implements ISensoryEncoderNeuron {

    private double preferredValue;
    private double sigma;

    public SensoryEncoderNeuron() {
        super();
        this.preferredValue = 0.5;
        this.sigma = 0.2;
    }

    public SensoryEncoderNeuron(Long neuronId,
                                com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain chain,
                                Long run,
                                double preferredValue,
                                double sigma) {
        super(neuronId, chain, run);
        this.preferredValue = preferredValue;
        this.sigma = sigma;
    }

    public double getPreferredValue() { return preferredValue; }
    public void setPreferredValue(double preferredValue) { this.preferredValue = preferredValue; }

    public double getSigma() { return sigma; }
    public void setSigma(double sigma) { this.sigma = sigma; }
}
