package com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.Neuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.storage.IInitInput;
import com.rakovpublic.jneuropallium.worker.test.definitions.functionallogic.ASignal;

import java.util.HashMap;

public class CycleNeuron extends Neuron {
    private int loopCount;

    private HashMap<Class<? extends ISignal>, ProcessingFrequency> signalProcessingFrequencyMap;
    private HashMap<IInitInput, ProcessingFrequency> inputProcessingFrequencyHashMap;

    public void setSignalProcessingFrequencyMap(HashMap<Class<? extends ISignal>, ProcessingFrequency> signalProcessingFrequencyMap) {
        this.signalProcessingFrequencyMap = signalProcessingFrequencyMap;
    }

    public void setInputProcessingFrequencyHashMap(HashMap<IInitInput, ProcessingFrequency> inputProcessingFrequencyHashMap) {
        this.inputProcessingFrequencyHashMap = inputProcessingFrequencyHashMap;
    }

    public CycleNeuron(int loopCount, ISignalChain signalChain, Long id, Long run) {
        super(id, signalChain, run);
        this.loopCount = loopCount;
        signalProcessingFrequencyMap = new HashMap<>();
        this.addSignalProcessor(CycleInputUpdateSignal.class, new CycleInputUpdateProcessor());
        this.addSignalProcessor(ProcessingFrequencySignal.class, new ProcessingFrequencySignalProcessor());
        this.addSignalProcessor(SumCycleSignal.class, new SumCycleSignalProcessor());
        this.addSignalProcessor(MultiplyCycleSignal.class, new MultiplyCycleSignalProcessor());
    }

    public CycleNeuron(int loopCount, ISignalChain signalChain, Long id, Long run, HashMap<Class<? extends ISignal>, ProcessingFrequency> signalProcessingFrequencyMap, HashMap<IInitInput, ProcessingFrequency> inputProcessingFrequencyHashMap) {
        super(id, signalChain, run);
        this.loopCount = loopCount;
        this.signalProcessingFrequencyMap = signalProcessingFrequencyMap;
        this.inputProcessingFrequencyHashMap = inputProcessingFrequencyHashMap;
        this.addSignalProcessor(CycleInputUpdateSignal.class, new CycleInputUpdateProcessor());
        this.addSignalProcessor(ProcessingFrequencySignal.class, new ProcessingFrequencySignalProcessor());
        this.addSignalProcessor(SumCycleSignal.class, new SumCycleSignalProcessor());
        this.addSignalProcessor(MultiplyCycleSignal.class, new MultiplyCycleSignalProcessor());
    }


    public int getLoopCount() {
        return loopCount;
    }

    public void setLoopCount(int loopCount) {
        this.loopCount = loopCount;
    }


    public HashMap<IInitInput, ProcessingFrequency> getInputProcessingFrequencyHashMap() {
        return inputProcessingFrequencyHashMap;
    }

    public HashMap<Class<? extends ISignal>, ProcessingFrequency> getSignalProcessingFrequencyMap() {
        return signalProcessingFrequencyMap;
    }
    @Override
    public void activate() {

        super.activate();
        isProcessed=true;
    }

}
