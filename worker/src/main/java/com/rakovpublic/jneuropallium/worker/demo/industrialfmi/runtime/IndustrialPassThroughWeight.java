package com.rakovpublic.jneuropallium.worker.demo.industrialfmi.runtime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.rakovpublic.jneuropallium.worker.net.neuron.IWeight;
import com.rakovpublic.jneuropallium.worker.net.signals.IChangingSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

public class IndustrialPassThroughWeight implements IWeight<ISignal, IChangingSignal> {
    public String weightClass = IndustrialPassThroughWeight.class.getName();
    public String signalClassName = ISignal.class.getName();

    public IndustrialPassThroughWeight() {
    }

    public IndustrialPassThroughWeight(String signalClassName) {
        this.signalClassName = signalClassName;
    }

    @Override
    public ISignal process(ISignal signal) {
        return signal;
    }

    @Override
    public void changeWeight(IChangingSignal signal) {
        // Runtime industrial advisory flow is inference-only.
    }

    @Override
    @JsonIgnore
    @SuppressWarnings("unchecked")
    public Class<ISignal> getSignalClass() {
        try {
            return (Class<ISignal>) Class.forName(signalClassName);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Cannot resolve signal class " + signalClassName, e);
        }
    }
}
