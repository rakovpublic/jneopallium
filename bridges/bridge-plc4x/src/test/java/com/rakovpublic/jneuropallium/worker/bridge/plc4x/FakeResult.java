/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.plc4x;

import com.rakovpublic.jneuropallium.worker.net.layers.IResult;
import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;

/** Test-only {@link IResult} wrapper. */
final class FakeResult implements IResult<IResultSignal> {

    private final IResultSignal signal;
    private final Long neuronId;

    FakeResult(IResultSignal signal) { this(signal, 1L); }

    FakeResult(IResultSignal signal, Long neuronId) {
        this.signal = signal;
        this.neuronId = neuronId;
    }

    @Override public IResultSignal getResult() { return signal; }
    @Override public Long getNeuronId() { return neuronId; }
}
