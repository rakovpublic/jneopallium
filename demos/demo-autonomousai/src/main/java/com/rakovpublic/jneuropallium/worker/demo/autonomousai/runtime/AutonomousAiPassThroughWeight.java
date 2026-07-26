package com.rakovpublic.jneuropallium.worker.demo.autonomousai.runtime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.rakovpublic.jneuropallium.worker.net.neuron.IWeight;
import com.rakovpublic.jneuropallium.worker.net.signals.IChangingSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

public class AutonomousAiPassThroughWeight implements IWeight<ISignal, IChangingSignal> {
    public String weightClass = AutonomousAiPassThroughWeight.class.getName();
    public String signalClassName = AutonomousAiSignal.class.getName();

    public AutonomousAiPassThroughWeight() {
    }

    @Override
    public ISignal process(ISignal signal) {
        return signal;
    }

    @Override
    public void changeWeight(IChangingSignal signal) {
        // The demo records plasticity signals but keeps the executable model deterministic.
    }

    @Override
    @JsonIgnore
    @SuppressWarnings("unchecked")
    public Class<ISignal> getSignalClass() {
        try {
            return (Class<ISignal>) Class.forName(signalClassName);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Cannot resolve autonomous AI signal class " + signalClassName, e);
        }
    }
}
