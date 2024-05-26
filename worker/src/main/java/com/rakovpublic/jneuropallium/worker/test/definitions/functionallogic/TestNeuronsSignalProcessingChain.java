package com.rakovpublic.jneuropallium.worker.test.definitions.functionallogic;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.ArrayList;
import java.util.List;

public class TestNeuronsSignalProcessingChain implements ISignalChain {
    @Override
    public List<Class<? extends ISignal>> getProcessingChain() {
        List<Class<? extends ISignal>> result = new ArrayList<>();
        result.add(DoubleSignal.class);
        result.add(IntSignal.class);
        result.add(ASignal.class);
        return result;
    }

    @Override
    public String getDescription() {
        return "Test processing chain";
    }
}
