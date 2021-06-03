package com.rakovpublic.jneuropallium.worker.neuron.impl;

import com.rakovpublic.jneuropallium.worker.neuron.INeuron;
//TODO: add wrappers for weights and other interfaces and rewrite parsing logic to use it
public class NeuronWrapper<N extends INeuron> {
    private String neuronClass;
    private N neuron;
    public NeuronWrapper(N neuron){
        this.neuron=neuron;
        this.neuronClass= neuron.getCurrentNeuronClass().getCanonicalName();

    }
    N getNeuron(){
        return neuron;
    }
    String getNeuronClass(){
        return neuronClass;
    }

}
