package com.rakovpublic.jneuropallium.ai.neurons.features;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;

/**
 * Contrast-enhancer neuron used by ContrastProcessor.
 * Applies centre-surround contrast: output = input * excitatory - inhibitory.
 */
public class ContrastEnhancerNeuron extends ModulatableNeuron implements IContrastEnhancerNeuron {

    private double excitatory;
    private double inhibitory;

    public ContrastEnhancerNeuron() {
        super();
        this.excitatory = 1.5;
        this.inhibitory = 0.5;
    }

    public ContrastEnhancerNeuron(Long neuronId,
                                  com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain chain,
                                  Long run,
                                  double excitatory,
                                  double inhibitory) {
        super(neuronId, chain, run);
        this.excitatory = excitatory;
        this.inhibitory = inhibitory;
    }

    public double getExcitatory() { return excitatory; }
    public void setExcitatory(double excitatory) { this.excitatory = excitatory; }

    public double getInhibitory() { return inhibitory; }
    public void setInhibitory(double inhibitory) { this.inhibitory = inhibitory; }
}
