package com.rakovpublic.jneuropallium.worker.neuron.impl.cycleprocessing;

import com.rakovpublic.jneuropallium.worker.net.storages.InputStatusMeta;
import com.rakovpublic.jneuropallium.worker.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.neuron.impl.Neuron;

public class CycleNeuron extends Neuron {
    private int loopCount;
    private InputStatusMeta inputStatusMeta;
    public CycleNeuron(int loopCount, ISignalChain signalChain, InputStatusMeta inputMeta, Long id) {
        super(id,signalChain );
        this.loopCount = loopCount;
        this.inputStatusMeta= inputMeta;
    }

    public int getLoopCount() {
        return loopCount;
    }

    public void setLoopCount(int loopCount) {
        this.loopCount = loopCount;
    }

    public InputStatusMeta getInputStatusMeta() {
        return inputStatusMeta;
    }

    public void setInputStatusMeta(InputStatusMeta inputStatusMeta) {
        this.inputStatusMeta = inputStatusMeta;
    }
}
