/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.security;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.security.IInnateInterneuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.SelfToleranceSignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor: updates the soft allow-list held by the innate
 * interneuron (additions or revocations). The hard constraints on the
 * response gate are separate and cannot be touched at runtime.
 */
public class SelfToleranceProcessor implements ISignalProcessor<SelfToleranceSignal, IInnateInterneuron> {

    private static final String DESCRIPTION = "Soft allow-list maintenance for the innate interneuron";

    @Override
    public <I extends ISignal> List<I> process(SelfToleranceSignal input, IInnateInterneuron neuron) {
        if (input != null && neuron != null) neuron.onTolerance(input);
        return new LinkedList<>();
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return SelfToleranceProcessor.class; }
    @Override public Class<IInnateInterneuron> getNeuronClass() { return IInnateInterneuron.class; }
    @Override public Class<SelfToleranceSignal> getSignalClass() { return SelfToleranceSignal.class; }
}
