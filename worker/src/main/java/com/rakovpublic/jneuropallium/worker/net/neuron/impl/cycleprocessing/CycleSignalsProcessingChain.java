package com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing;

import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;

import java.util.LinkedList;
import java.util.List;

public class CycleSignalsProcessingChain implements ISignalChain {
    private List<Class<? extends ISignal>> chain;
    private static final String description = "simple math operation oder cycle chain";

    public CycleSignalsProcessingChain() {
        this.chain = new LinkedList<>();
        chain.add(MultiplyCycleSignal.class);
        chain.add(SumCycleSignal.class);
    }

    @Override
    public List<Class<? extends ISignal>> getProcessingChain() {
        return chain;
    }

    @Override
    public String getDescription() {
        return description;
    }
}
