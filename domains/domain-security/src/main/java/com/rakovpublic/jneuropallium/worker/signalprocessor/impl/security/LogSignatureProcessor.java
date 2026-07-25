/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.security;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.security.ISignaturePatternNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.LogEventSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.SignatureMatchSignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor: runs a {@link LogEventSignal} against the
 * signature matcher so innate detection covers logs as well as packets.
 */
public class LogSignatureProcessor implements ISignalProcessor<LogEventSignal, ISignaturePatternNeuron> {

    private static final String DESCRIPTION = "Innate signature matching on normalised logs";

    @Override
    @SuppressWarnings("unchecked")
    public <I extends ISignal> List<I> process(LogEventSignal input, ISignaturePatternNeuron neuron) {
        List<I> out = new LinkedList<>();
        if (input == null || neuron == null) return out;
        SignatureMatchSignal m = neuron.match(input);
        if (m != null) out.add((I) m);
        return out;
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return LogSignatureProcessor.class; }
    @Override public Class<ISignaturePatternNeuron> getNeuronClass() { return ISignaturePatternNeuron.class; }
    @Override public Class<LogEventSignal> getSignalClass() { return LogEventSignal.class; }
}
