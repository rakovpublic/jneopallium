package com.rakovpublic.jneuropallium.worker.net.neuron.impl;

import com.rakovpublic.jneuropallium.worker.net.neuron.INeuron;

//TODO: add wrappers for weights and other interfaces and rewrite parsing logic to use it
//wrapper example
public class NeuronWrapper<N extends INeuron> extends Neuron implements INeuron {
    private String neuronClass;
    private N neuron;

    public NeuronWrapper(N neuron) {
        this.neuron = neuron;
        this.neuronClass = neuron.getCurrentNeuronClass().getCanonicalName();

    }

    N getNeuron() {
        return neuron;
    }

    String getNeuronClass() {
        return neuronClass;
    }

}
