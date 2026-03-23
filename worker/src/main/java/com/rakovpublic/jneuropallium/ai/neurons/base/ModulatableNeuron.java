package com.rakovpublic.jneuropallium.ai.neurons.base;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.Neuron;

/**
 * Base neuron class that holds neuromodulator state.
 * All neurons that respond to neuromodulator signals should extend this.
 */
public abstract class ModulatableNeuron extends Neuron {

    protected double dopamineLevel = 1.0;
    protected double errorDampeningFactor = 0.0;
    protected double norepinephrineLevel = 1.0;
    protected double achLevel = 0.0;
    protected double inhibitionLevel = 0.0;

    protected ModulatableNeuron() { super(); }

    protected ModulatableNeuron(Long neuronId, com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain chain, Long run) {
        super(neuronId, chain, run);
    }

    public double getDopamineLevel() { return dopamineLevel; }
    public void setDopamineLevel(double dopamineLevel) { this.dopamineLevel = dopamineLevel; }

    public double getErrorDampeningFactor() { return errorDampeningFactor; }
    public void setErrorDampeningFactor(double errorDampeningFactor) { this.errorDampeningFactor = errorDampeningFactor; }

    public double getNorepinephrineLevel() { return norepinephrineLevel; }
    public void setNorepinephrineLevel(double norepinephrineLevel) { this.norepinephrineLevel = norepinephrineLevel; }

    public double getAchLevel() { return achLevel; }
    public void setAchLevel(double achLevel) { this.achLevel = achLevel; }

    public double getInhibitionLevel() { return inhibitionLevel; }
    public void setInhibitionLevel(double inhibitionLevel) { this.inhibitionLevel = inhibitionLevel; }
}
