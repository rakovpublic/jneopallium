package com.rakovpublic.jneuropallium.worker.neuron.impl;

import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.neuron.IWeight;


//Add wrapper usage for weights
class WeightWrapper<N extends IWeight,S extends ISignal,K extends ISignal> implements IWeight<S,K> {
    private N weight;
    private String weightClass;

    public WeightWrapper(N weight) {
        this.weight = weight;
        this.weightClass = weight.getClass().getCanonicalName();
    }

    @Override
    public S process(S signal) {
        return null;
    }

    @Override
    public void changeWeight(K signal) {

    }

    @Override
    public Class<S> getSignalClass() {
        return null;
    }

    public N getWeight() {
        return weight;
    }
}
