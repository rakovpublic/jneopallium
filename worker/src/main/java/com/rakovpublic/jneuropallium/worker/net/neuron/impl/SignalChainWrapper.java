package com.rakovpublic.jneuropallium.worker.net.neuron.impl;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.List;

public class SignalChainWrapper<K extends ISignalChain> implements ISignalChain {
    private K signalChain;
    private Class<K> signalChainClass;

    public SignalChainWrapper(K signalChain, Class<K> signalChainClass) {
        this.signalChain = signalChain;
        this.signalChainClass = signalChainClass;
    }

    @Override
    public List<Class<? extends ISignal>> getProcessingChain() {
        return null;
    }

    @Override
    public String getDescription() {
        return null;
    }
}
