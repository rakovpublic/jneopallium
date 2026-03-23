package com.rakovpublic.jneuropallium.ai.neurons.base;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.Arrays;
import java.util.List;

public class SimpleSignalChain implements ISignalChain {
    private final List<Class<? extends ISignal>> chain;
    private final String description;

    @SafeVarargs
    public SimpleSignalChain(String description, Class<? extends ISignal>... signalClasses) {
        this.description = description;
        this.chain = Arrays.asList(signalClasses);
    }

    @Override
    public List<Class<? extends ISignal>> getProcessingChain() { return chain; }

    @Override
    public String getDescription() { return description; }
}
