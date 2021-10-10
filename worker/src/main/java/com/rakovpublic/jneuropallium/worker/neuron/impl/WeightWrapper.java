package com.rakovpublic.jneuropallium.worker.neuron.impl;

import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.neuron.IWeight;


//Add wrapper usage for weights
class WeightWrapper<N extends IWeight, S extends ISignal, K extends ISignal> implements IWeight<S, K> {
    private N weight;
    private String weightClass;

    public WeightWrapper(N weight) {
        this.weight = weight;
        this.weightClass = weight.getClass().getCanonicalName();
    }

    @Override
    public S process(S signal) {
        return (S) weight.process(signal);
    }

    @Override
    public void changeWeight(K signal) {
        weight.changeWeight(signal);

    }

    @Override
    public Class<S> getSignalClass() {
        return weight.getSignalClass();
    }

    public N getWeight() {
        return weight;
    }
}
