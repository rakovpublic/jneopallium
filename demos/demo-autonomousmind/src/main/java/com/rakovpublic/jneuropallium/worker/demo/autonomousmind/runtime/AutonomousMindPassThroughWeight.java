package com.rakovpublic.jneuropallium.worker.demo.autonomousmind.runtime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.rakovpublic.jneuropallium.worker.net.neuron.IWeight;
import com.rakovpublic.jneuropallium.worker.net.signals.IChangingSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

public class AutonomousMindPassThroughWeight implements IWeight<ISignal, IChangingSignal> {
    public String weightClass = AutonomousMindPassThroughWeight.class.getName();
    public String signalClassName = AutonomousMindSignal.class.getName();

    public AutonomousMindPassThroughWeight() {
    }

    @Override
    public ISignal process(ISignal signal) {
        return signal;
    }

    @Override
    public void changeWeight(IChangingSignal signal) {
        // Runtime learning is emitted as typed trace signals; generated demo weights stay deterministic.
    }

    @Override
    @JsonIgnore
    @SuppressWarnings("unchecked")
    public Class<ISignal> getSignalClass() {
        try {
            return (Class<ISignal>) Class.forName(signalClassName);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Cannot resolve AutonomousMind signal class " + signalClassName, e);
        }
    }
}
