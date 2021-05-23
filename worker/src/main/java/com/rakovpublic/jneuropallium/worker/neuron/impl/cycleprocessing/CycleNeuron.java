package com.rakovpublic.jneuropallium.worker.neuron.impl.cycleprocessing;

import com.rakovpublic.jneuropallium.worker.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.neuron.impl.Neuron;

public class CycleNeuron extends Neuron {
    private int loopCount;

    public CycleNeuron(int loopCount, ISignalChain signalChain) {
        super(0l,signalChain );
        this.loopCount = loopCount;
    }

    public int getLoopCount() {
        return loopCount;
    }

    public void setLoopCount(int loopCount) {
        this.loopCount = loopCount;
    }
}
