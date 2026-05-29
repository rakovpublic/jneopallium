/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.demo.industrial;

import com.rakovpublic.jneuropallium.worker.net.layers.IResult;
import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;

/**
 * Minimal {@link IResult} wrapper so neuron-derived signals can be handed
 * to {@link com.rakovpublic.jneuropallium.worker.output.opcua.OpcUaCommandOutputAggregator}.
 */
final class ResultWrapper implements IResult<IResultSignal> {

    private final IResultSignal signal;
    private final Long neuronId;

    ResultWrapper(IResultSignal signal, Long neuronId) {
        this.signal = signal;
        this.neuronId = neuronId;
    }

    @Override public IResultSignal getResult() { return signal; }

    @Override public Long getNeuronId() { return neuronId; }
}
