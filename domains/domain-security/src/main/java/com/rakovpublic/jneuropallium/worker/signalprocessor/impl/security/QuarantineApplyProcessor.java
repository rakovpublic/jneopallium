/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.security;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.security.IQuarantineEntityNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.QuarantineRequestSignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor: applies a gated {@link QuarantineRequestSignal}
 * to an {@link IQuarantineEntityNeuron}, scheduling its automatic
 * lift. Uses the signal's epoch tick as &quot;now&quot;.
 */
public class QuarantineApplyProcessor implements ISignalProcessor<QuarantineRequestSignal, IQuarantineEntityNeuron> {

    private static final String DESCRIPTION = "Applies gated quarantine with automatic lift scheduling";

    @Override
    public <I extends ISignal> List<I> process(QuarantineRequestSignal input, IQuarantineEntityNeuron neuron) {
        if (input == null || neuron == null) return new LinkedList<>();
        neuron.apply(input, input.getEpoch());
        return new LinkedList<>();
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return QuarantineApplyProcessor.class; }
    @Override public Class<IQuarantineEntityNeuron> getNeuronClass() { return IQuarantineEntityNeuron.class; }
    @Override public Class<QuarantineRequestSignal> getSignalClass() { return QuarantineRequestSignal.class; }
}
