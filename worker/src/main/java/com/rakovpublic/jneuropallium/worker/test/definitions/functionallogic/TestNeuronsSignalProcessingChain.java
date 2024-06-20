package com.rakovpublic.jneuropallium.worker.test.definitions.functionallogic;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class TestNeuronsSignalProcessingChain implements ISignalChain {
    public String signalChainClass = "com.rakovpublic.jneuropallium.worker.test.definitions.functionallogic.TestNeuronsSignalProcessingChain";
    public List<Class<? extends ISignal>> processingChain = new LinkedList<>();
    public String description = "Test processing chain";
    public TestNeuronsSignalProcessingChain() {
        processingChain.add(DoubleSignal.class);
        processingChain.add(IntSignal.class);
        processingChain.add(ASignal.class);
    }

    public String getSignalChainClass() {
        return signalChainClass;
    }

    public void setSignalChainClass(String signalChainClass) {
        this.signalChainClass = signalChainClass;
    }

    public void setProcessingChain(List<Class<? extends ISignal>> processingChain) {
        this.processingChain = processingChain;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public List<Class<? extends ISignal>> getProcessingChain() {
        return processingChain;
    }

    @Override
    public String getDescription() {
        return "Test processing chain";
    }
}
