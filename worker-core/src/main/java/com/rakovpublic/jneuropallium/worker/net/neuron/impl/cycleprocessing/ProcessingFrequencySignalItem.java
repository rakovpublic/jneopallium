package com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing;

import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

public class ProcessingFrequencySignalItem {
    private Class<? extends ISignal> signalClass;
    private ProcessingFrequency frequency;


    public Class<? extends ISignal> getSignalClass() {
        return signalClass;
    }

    public void setSignalClass(Class<? extends ISignal> signalClass) {
        this.signalClass = signalClass;
    }

    public ProcessingFrequency getFrequency() {
        return frequency;
    }

    public void setFrequency(ProcessingFrequency frequency) {
        this.frequency = frequency;
    }

    public ProcessingFrequencySignalItem() {
    }

    public ProcessingFrequencySignalItem(Class<? extends ISignal> signalClass, ProcessingFrequency frequency) {
        this.signalClass = signalClass;
        this.frequency = frequency;
    }
}
