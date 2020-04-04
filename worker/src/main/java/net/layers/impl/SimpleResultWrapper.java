package net.layers.impl;

import net.layers.IResult;
import net.signals.IResultSignal;

public class SimpleResultWrapper<K extends IResultSignal> implements IResult<K> {
    private K result;
    private Long neuronId;

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
