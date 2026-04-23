/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.tutoring;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.tutoring.IResponseObserverNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.tutoring.ResponseSignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor: forwards a learner {@link ResponseSignal} to an
 * {@link IResponseObserverNeuron} which updates accuracy / latency
 * counters. Emits no follow-up signals.
 */
public class ResponseObservationProcessor implements ISignalProcessor<ResponseSignal, IResponseObserverNeuron> {

    private static final String DESCRIPTION = "Accumulates learner response metrics";

    @Override
    public <I extends ISignal> List<I> process(ResponseSignal input, IResponseObserverNeuron neuron) {
        if (input != null && neuron != null) neuron.observe(input);
        return new LinkedList<>();
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return ResponseObservationProcessor.class; }
    @Override public Class<IResponseObserverNeuron> getNeuronClass() { return IResponseObserverNeuron.class; }
    @Override public Class<ResponseSignal> getSignalClass() { return ResponseSignal.class; }
}
