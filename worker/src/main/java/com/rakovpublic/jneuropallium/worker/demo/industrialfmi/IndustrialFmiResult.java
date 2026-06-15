/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.demo.industrialfmi;

import com.rakovpublic.jneuropallium.worker.net.layers.IResult;
import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;

/** Public result wrapper used by the demo controller and tests. */
public final class IndustrialFmiResult implements IResult<IResultSignal> {
    private final IResultSignal signal;
    private final Long neuronId;

    public IndustrialFmiResult(IResultSignal signal, Long neuronId) {
        this.signal = signal;
        this.neuronId = neuronId;
    }

    @Override
    public IResultSignal getResult() {
        return signal;
    }

    @Override
    public Long getNeuronId() {
        return neuronId;
    }
}
