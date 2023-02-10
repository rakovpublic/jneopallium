package com.rakovpublic.jneuropallium.worker.neuron.impl.cycleprocessing;

import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.Objects;

public class ProcessingFrequencySignalItem {
    private Class<? extends ISignal> signalClass;
    private Integer frequency;

    public Class<? extends ISignal> getSignalClass() {
        return signalClass;
    }

    public void setSignalClass(Class<? extends ISignal> signalClass) {
        this.signalClass = signalClass;
    }

    public Integer getFrequency() {
        return frequency;
    }

    public void setFrequency(Integer frequency) {
        this.frequency = frequency;
    }

    public ProcessingFrequencySignalItem() {
    }

    public ProcessingFrequencySignalItem(Class<? extends ISignal> signalClass, Integer frequency) {
        this.signalClass = signalClass;
        this.frequency = frequency;
    }
}
