package com.rakovpublic.jneuropallium.ai.neurons.features;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;

/**
 * Inhibitory interneuron used by LateralInhibitionProcessor.
 * Suppresses neighbouring neurons in the same layer when strongly activated.
 */
public class InhibitoryInterneuron extends ModulatableNeuron {

    private int layerId;
    private double inhibitionStrength;

    public InhibitoryInterneuron() {
        super();
        this.layerId = 0;
        this.inhibitionStrength = 1.0;
    }

    public InhibitoryInterneuron(Long neuronId,
                                 com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain chain,
                                 Long run,
                                 int layerId,
                                 double inhibitionStrength) {
        super(neuronId, chain, run);
        this.layerId = layerId;
        this.inhibitionStrength = inhibitionStrength;
    }

    public int getLayerId() { return layerId; }
    public void setLayerId(int layerId) { this.layerId = layerId; }

    public double getInhibitionStrength() { return inhibitionStrength; }
    public void setInhibitionStrength(double inhibitionStrength) { this.inhibitionStrength = inhibitionStrength; }
}
