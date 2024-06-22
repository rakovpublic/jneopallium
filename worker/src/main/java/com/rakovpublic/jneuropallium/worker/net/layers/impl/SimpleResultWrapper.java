package com.rakovpublic.jneuropallium.worker.net.layers.impl;

import com.rakovpublic.jneuropallium.worker.net.layers.IResult;
import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;

public class SimpleResultWrapper<K extends IResultSignal> implements IResult<K> {
    private final K result;
    private final Long neuronId;

    public SimpleResultWrapper(K result, Long neuronId) {
        this.result = result;
        this.neuronId = neuronId;
    }

    @Override
    public K getResult() {
        return result;
    }

    @Override
    public Long getNeuronId() {
        return neuronId;
    }
}
