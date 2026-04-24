/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.security;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.security.IInnateInterneuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.SignatureMatchSignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor: passes a {@link SignatureMatchSignal} through
 * the innate interneuron's allow-list filter. When suppressed, the
 * signal is dropped — preventing self-attack.
 */
public class SignatureToleranceProcessor implements ISignalProcessor<SignatureMatchSignal, IInnateInterneuron> {

    private static final String DESCRIPTION = "Self-tolerance filter on signature matches";

    @Override
    @SuppressWarnings("unchecked")
    public <I extends ISignal> List<I> process(SignatureMatchSignal input, IInnateInterneuron neuron) {
        List<I> out = new LinkedList<>();
        if (input == null || neuron == null) return out;
        SignatureMatchSignal kept = neuron.filter(input, input.getReferenceIoc());
        if (kept != null) out.add((I) kept);
        return out;
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return SignatureToleranceProcessor.class; }
    @Override public Class<IInnateInterneuron> getNeuronClass() { return IInnateInterneuron.class; }
    @Override public Class<SignatureMatchSignal> getSignalClass() { return SignatureMatchSignal.class; }
}
