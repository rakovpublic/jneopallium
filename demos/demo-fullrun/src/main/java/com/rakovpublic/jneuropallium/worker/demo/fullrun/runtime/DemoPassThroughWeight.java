package com.rakovpublic.jneuropallium.worker.demo.fullrun.runtime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.rakovpublic.jneuropallium.worker.net.neuron.IWeight;
import com.rakovpublic.jneuropallium.worker.net.signals.IChangingSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

public class DemoPassThroughWeight implements IWeight<ISignal, IChangingSignal> {
    public String weightClass = DemoPassThroughWeight.class.getName();
    public String signalClassName = DemoSignal.class.getName();

    public DemoPassThroughWeight() {
    }

    public DemoPassThroughWeight(String signalClassName) {
        this.signalClassName = signalClassName;
    }

    @Override
    public ISignal process(ISignal signal) {
        return signal;
    }

    @Override
    public void changeWeight(IChangingSignal signal) {
        // Demo models are deterministic inference/advisory runs; no online weight mutation is used.
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
