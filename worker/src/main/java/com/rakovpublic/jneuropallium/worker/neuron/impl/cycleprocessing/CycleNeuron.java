package com.rakovpublic.jneuropallium.worker.neuron.impl.cycleprocessing;

import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.storages.InputStatusMeta;
import com.rakovpublic.jneuropallium.worker.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.neuron.impl.Neuron;

import java.util.HashMap;

public class CycleNeuron extends Neuron {
    private int loopCount;
    private InputStatusMeta inputStatusMeta;
    private HashMap<Class<? extends ISignal>, ProcessingFrequency> signalProcessingFrequencyMap;

    public CycleNeuron(int loopCount, ISignalChain signalChain, InputStatusMeta inputMeta, Long id, Long run) {
        super(id, signalChain, run);
        this.loopCount = loopCount;
        this.inputStatusMeta = inputMeta;
        signalProcessingFrequencyMap = new HashMap<>();
    }
    public CycleNeuron(int loopCount, ISignalChain signalChain, InputStatusMeta inputMeta, Long id, Long run, HashMap<Class<? extends ISignal>, ProcessingFrequency> signalProcessingFrequencyMap) {
        super(id, signalChain, run);
        this.loopCount = loopCount;
        this.inputStatusMeta = inputMeta;
        this.signalProcessingFrequencyMap = signalProcessingFrequencyMap;
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

    public HashMap<Class<? extends ISignal>, ProcessingFrequency> getSignalProcessingFrequencyMap() {
        return signalProcessingFrequencyMap;
    }
}
